/*
 * mxhsd - Corporate Matrix Homeserver
 * Copyright (C) 2017 Kamax Sarl
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

import io.kamax.mxhsd.api.event.IEvent;
import io.kamax.mxhsd.api.federation.ITransaction;
import io.kamax.mxhsd.api.room.IRoom;
import io.kamax.mxhsd.api.room.IRoomStateSnapshot;
import io.kamax.mxhsd.api.session.server.IServerEventManager;
import io.kamax.mxhsd.api.session.server.IServerRoomDirectory;
import io.kamax.mxhsd.api.session.server.IServerSession;
import io.kamax.mxhsd.core.GlobalStateHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;

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

        transaction.getPdus().forEach(sEv -> ForkJoinPool.commonPool().execute(() -> {
            try {
                String evId = sEv.getId();
                String roomId = sEv.getRoomId();
                log.info("Transaction {}: Processing event {} for room {}", transaction.getId(), sEv.getId(), sEv.getRoomId());

                synchronized (global.getRoomMgr()) { // FIXME have a lock per Room ID
                    Optional<IRoom> rOpt = global.getRoomMgr().findRoom(sEv.getRoomId());
                    if (!rOpt.isPresent()) {
                        log.warn("Got event about unknown room {} from {}", sEv.getRoomId(), transaction.getOrigin());

                        log.info("Trying to fetch seed state from {}", transaction.getOrigin());
                        IRoomStateSnapshot snapshot = global.getHsMgr().get(transaction.getOrigin()).getState(roomId, evId);
                        List<IEvent> authChain = new ArrayList<>(snapshot.getAuthChain());
                        List<IEvent> state = new ArrayList<>(snapshot.getState());
                        global.getRoomMgr().discoverRoom(roomId, state, authChain, sEv);
                        log.info("Room was discovered successfully");
                    } else {
                        rOpt.get().inject(sEv);
                    }
                }
            } catch (RuntimeException e) {
                log.warn("Inbound transaction {} from {}: Failure during processing of event {}: {}", transaction.getId(), transaction.getOrigin(), sEv.getId(), e.getMessage());
            }
        }));
    }

}
