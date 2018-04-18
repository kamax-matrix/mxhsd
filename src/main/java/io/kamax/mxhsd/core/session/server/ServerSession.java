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

package io.kamax.mxhsd.core.session.server;

import com.google.gson.JsonObject;
import io.kamax.mxhsd.GsonUtil;
import io.kamax.mxhsd.api.event.IEvent;
import io.kamax.mxhsd.api.federation.ITransaction;
import io.kamax.mxhsd.api.room.IRoom;
import io.kamax.mxhsd.api.session.server.IServerEventManager;
import io.kamax.mxhsd.api.session.server.IServerRoomDirectory;
import io.kamax.mxhsd.api.session.server.IServerSession;
import io.kamax.mxhsd.core.GlobalStateHolder;
import io.kamax.mxhsd.core.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ServerSession implements IServerSession {

    private final Logger log = LoggerFactory.getLogger(ServerSession.class);

    private GlobalStateHolder global;

    public ServerSession(GlobalStateHolder global) {
        this.global = global;
    }

    @Override
    public IServerEventManager getEventMgr() {
        return ids -> global.getEvMgr().get(ids);
    }

    @Override
    public IServerRoomDirectory getDirectory() {
        return new ServerRoomDirectory(global);
    }

    @Override
    public IRoom getRoom(String id) {
        return global.getRoomMgr().getRoom(id);
    }

    @Override
    public void push(ITransaction transaction) {
        log.debug("Inbound transaction {} from {} @ {}", transaction.getId(), transaction.getOrigin(), transaction.getOriginTimestamp());

        try {
            transaction.getPdus().forEach(sEv -> {
                log.info("Transaction {}: Processing event {} for room {}", transaction.getId(), sEv.getId(), sEv.getRoomId());
                synchronized (global.getRoomMgr()) { // FIXME have a lock per Room ID
                    Optional<IRoom> rOpt = global.getRoomMgr().findRoom(sEv.getRoomId());
                    if (!rOpt.isPresent()) {
                        log.warn("Got event about unknown room {} from {}", sEv.getRoomId(), transaction.getOrigin());

                        log.info("Trying to fetch seed state from {}", transaction.getOrigin());
                        JsonObject body = global.getHsMgr().get(transaction.getOrigin()).send("GET", "/_matrix/federation/v1/state/" + sEv.getRoomId() + "/", Collections.singletonMap("event_id", sEv.getId()), null);
                        List<IEvent> authChain = GsonUtil.asList(body, "auth_chain", JsonObject.class).stream().map(Event::new).collect(Collectors.toList());
                        List<IEvent> state = GsonUtil.asList(body, "pdus", JsonObject.class).stream().map(Event::new).collect(Collectors.toList());
                        global.getRoomMgr().discoverRoom(sEv.getRoomId(), state, authChain, sEv);
                        log.info("Room was discovered successfully");
                    } else {
                        rOpt.get().inject(sEv);
                    }
                }
            });
        } catch (RuntimeException e) {
            log.warn("Inbound transaction {} from {}: Failure during processing: {}", transaction.getId(), transaction.getOrigin(), e.getMessage());
            throw e;
        }
    }

}
