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

package io.kamax.mxhsd.core.room.directory;

import io.kamax.matrix.room.RoomAlias;
import io.kamax.mxhsd.api.event.IEvent;
import io.kamax.mxhsd.api.event.StateTuple;
import io.kamax.mxhsd.api.room.RoomEventType;
import io.kamax.mxhsd.api.room.directory.ICoreRoomDirectory;
import io.kamax.mxhsd.api.room.directory.IFederatedRoomAliasLookup;
import io.kamax.mxhsd.api.room.event.RoomAliasEvent;
import io.kamax.mxhsd.core.HomeserverState;
import net.engio.mbassy.listener.Handler;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GlobalRoomDirectory implements ICoreRoomDirectory {

    private final Logger log = LoggerFactory.getLogger(GlobalRoomDirectory.class);

    private HomeserverState global;

    private Map<String, String> mappings = new ConcurrentHashMap<>();
    private Map<String, List<String>> idToAliases = new ConcurrentHashMap<>();

    public GlobalRoomDirectory(HomeserverState global) {
        (this.global = global).getRoomMgr().forAllRooms().addListener(this);
    }

    @Handler
    public void handleEvents(IEvent ev) {
        log.debug("Received event {}", ev.getId());
        if (!RoomEventType.Aliases.is(ev.getType())) {
            return;
        }

        if (!StringUtils.equals(global.getDomain(), ev.getOrigin())) {
            return;
        }

        List<String> newAliases = new ArrayList<>(new RoomAliasEvent(ev).getAliases());
        log.info("Processing new aliases of room {}: {}", ev.getRoomId(), newAliases);

        List<String> oldAliases = new ArrayList<>(idToAliases.getOrDefault(ev.getRoomId(), Collections.emptyList()));
        oldAliases.removeAll(newAliases);

        idToAliases.put(ev.getRoomId(), Collections.unmodifiableList(newAliases));
        newAliases.forEach(a -> mappings.put(a, ev.getRoomId()));
        oldAliases.forEach(alias -> mappings.remove(alias));
    }

    @Override
    public Optional<IFederatedRoomAliasLookup> lookup(String alias) {
        RoomAlias ra = RoomAlias.from(alias);
        if (StringUtils.equals(global.getDomain(), ra.getDomain())) {
            return Optional.ofNullable(mappings.get(alias))
                    // FIXME lookup server list
                    .map(id -> new FederatedRoomAliasLookup(global.getDomain(), id, alias, Collections.singletonList(global.getDomain())));
        } else {
            return global.getHsMgr().get(ra.getDomain()).lookup(alias);
        }
    }

    @Override
    public List<String> getAliases(String roomId) {
        return global.getRoomMgr().getRoom(roomId)
                .getCurrentState()
                .findEventFor(StateTuple.of(RoomEventType.Aliases, global.getDomain()))
                .map(RoomAliasEvent::new)
                .map(RoomAliasEvent::getAliases)
                .orElse(Collections.emptyList());
    }

}
