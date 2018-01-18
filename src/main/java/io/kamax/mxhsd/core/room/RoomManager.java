/*
 * mxhsd - Corporate Matrix Homeserver
 * Copyright (C) 2017 Maxime Dor
 *
 * https://www.kamax.io/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.kamax.mxhsd.core.room;

import com.google.gson.JsonObject;
import io.kamax.matrix.hs.RoomMembership;
import io.kamax.mxhsd.GsonUtil;
import io.kamax.mxhsd.api.event.ISignedEvent;
import io.kamax.mxhsd.api.federation.FederationException;
import io.kamax.mxhsd.api.federation.IRemoteHomeServer;
import io.kamax.mxhsd.api.room.*;
import io.kamax.mxhsd.api.room.directory.IRoomAliasLookup;
import io.kamax.mxhsd.api.room.event.*;
import io.kamax.mxhsd.core.HomeserverState;
import io.kamax.mxhsd.core.event.SignedEvent;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class RoomManager implements IRoomManager {

    private Logger log = LoggerFactory.getLogger(RoomManager.class);

    private HomeserverState state;
    private Map<String, IRoom> rooms;

    public RoomManager(HomeserverState state) {
        this.state = state;
        rooms = new HashMap<>();
    }

    private boolean hasRoom(String id) {
        synchronized (rooms) {
            return rooms.containsKey(id);
        }
    }

    private String getId() {
        String id;
        do {
            id = "!" + RandomStringUtils.randomAlphanumeric(16) + ":" + state.getDomain();
        } while (hasRoom(id));

        log.info("Generated Room ID {}", id);
        return id;
    }

    // TODO make it configurable via JSON data
    private RoomPowerLevels getPowerLevelEvent(IRoomCreateOptions options) {
        return new RoomPowerLevels.Builder()
                // Default state PL, moderator is a good compromise
                .setStateDefault(PowerLevel.Moderator)

                // Anyone can send any message events by default
                .setEventsDefault(PowerLevel.None)
                .addEvent(RoomEventType.HistoryVisibility.get(), PowerLevel.Admin)
                .addEvent(RoomEventType.PowerLevels.get(), PowerLevel.Admin)

                // Users don't get any PL by default, adding creator
                .setUsersDefault(PowerLevel.None)
                .addUser(options.getCreator().getId(), PowerLevel.Admin)

                // Define some basic room management, anyone can invite
                .setBan(PowerLevel.Moderator)
                .setInvite(PowerLevel.None)
                .setKick(PowerLevel.Moderator)
                .setRedact(PowerLevel.Moderator)
                .build();
    }

    @Override
    public IRoom createRoom(IRoomCreateOptions options) { // FIXME use RWLock
        String creator = options.getCreator().getId();
        String id = getId();
        Room room = new Room(state, id);

        synchronized (rooms) {
            room.inject(new RoomCreateEvent(creator));
            room.inject(new RoomMembershipEvent(creator, RoomMembership.Join.get(), creator));
            room.inject(new RoomPowerLevelEvent(creator, getPowerLevelEvent(options)));

            options.getPreset().ifPresent(p -> {
                log.info("Checking presets for room  {} creation", id);

                if (StringUtils.equals(p, "public_chat")) {
                    log.info("Applying preset {} for room {}", p, id);
                    room.inject(new RoomJoinRulesEvent(creator, "public"));
                    room.inject(new RoomHistoryVisibilityEvent(creator, "shared"));
                } else if (StringUtils.equals(p, "private_chat")) {
                    log.info("Applying preset {} for room {}", p, id);
                    room.inject(new RoomJoinRulesEvent(creator, "invite"));
                    room.inject(new RoomHistoryVisibilityEvent(creator, "shared"));
                } else if (StringUtils.equals(p, "trusted_private_chat")) {
                    log.info("Applying preset {} for room {}", p, id);
                    room.inject(new RoomJoinRulesEvent(creator, "invite"));
                    room.inject(new RoomHistoryVisibilityEvent(creator, "shared"));

                    RoomPowerLevels pls = room.getCurrentState().getEffectivePowerLevels();
                    long creatorPl = pls.getForUser(creator);
                    RoomPowerLevels.Builder plsBuilder = RoomPowerLevels.Builder.from(pls);
                    options.getInvitees().forEach(iId -> plsBuilder.addUser(iId.getId(), creatorPl));
                    room.inject(new RoomPowerLevelEvent(creator, plsBuilder.build()));
                } else {
                    log.info("Ignoring unknown preset {} for room {}", p, id);
                }
            });

            // FIXME handle initial_state

            // TODO handle name

            // TODO handle topic

            options.getInvitees().forEach(mxId -> {
                room.inject(new RoomMembershipEvent(creator, RoomMembership.Invite.get(), mxId.getId()));
            });

            // TODO handle invite_3pid

            rooms.put(id, room);

            log.info("Room {} created", id);
            return room;
        }
    }

    @Override
    public IAliasRoom getRoom(final IRoomAliasLookup lookup) {
        return findRoom(lookup.getId()).map(r -> (IAliasRoom) r).orElseGet(() -> {
            if (lookup.getServers().isEmpty()) {
                throw new IllegalArgumentException("Cannot join a room without resident homeservers");
            }

            return userId -> {
                for (String server : lookup.getServers()) {
                    try {
                        IRemoteHomeServer rHs = state.getHsMgr().get(server);
                        JsonObject protoEv = rHs.makeJoin(lookup.getId(), userId).getAsJsonObject("event");
                        log.info("Proto-event for remote join: {}", GsonUtil.getPrettyForLog(protoEv));
                        ISignedEvent joinEv = state.getEvMgr().finalize(protoEv);
                        JsonObject data = rHs.sendJoin(joinEv);
                        log.info("Remote data before join: {}", GsonUtil.getPrettyForLog(data));

                        List<ISignedEvent> state = GsonUtil.asList(data, "state", JsonObject.class)
                                .stream().map(SignedEvent::new).collect(Collectors.toList());
                        state.add(joinEv);
                        List<ISignedEvent> authChain = GsonUtil.asList(data, "auth_chain", JsonObject.class)
                                .stream().map(SignedEvent::new).collect(Collectors.toList());

                        synchronized (rooms) {
                            Room room = new Room(RoomManager.this.state, lookup.getId(), state, authChain);
                            rooms.put(room.getId(), room);
                            return room;
                        }
                    } catch (FederationException e) {
                        log.warn("Unable to join {} using {}: {}", lookup.getAlias(), server, e.getMessage());
                    }
                }

                throw new RuntimeException("Unable to join " + lookup.getAlias() + ": all servers failed");
            };
        });
    }

    @Override
    public synchronized Optional<IRoom> findRoom(String id) { // FIXME use RWLock
        synchronized (rooms) {
            return Optional.ofNullable(rooms.get(id));
        }
    }

    @Override
    public synchronized List<IRoom> listRooms() { // FIXME use RWLock
        synchronized (rooms) {
            return new ArrayList<>(rooms.values());
        }
    }

}
