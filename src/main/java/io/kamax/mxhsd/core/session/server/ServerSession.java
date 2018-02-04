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

import io.kamax.mxhsd.api.federation.ITransaction;
import io.kamax.mxhsd.api.room.IRoom;
import io.kamax.mxhsd.api.session.server.IServerEventManager;
import io.kamax.mxhsd.api.session.server.IServerRoomDirectory;
import io.kamax.mxhsd.api.session.server.IServerSession;
import io.kamax.mxhsd.core.HomeserverState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class ServerSession implements IServerSession {

    private final Logger log = LoggerFactory.getLogger(ServerSession.class);

    private HomeserverState global;

    public ServerSession(HomeserverState global) {
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
        log.info("Processing transaction {} from {} sent at {}", transaction.getId(), transaction.getOrigin(), transaction.getOriginTimestamp());

        // TODO make asynchronous
        transaction.getPdus().forEach(sEv -> {
            log.info("Processing event {} for room {}", sEv.getId(), sEv.getRoomId());
            Optional<IRoom> rOpt = global.getRoomMgr().findRoom(sEv.getRoomId());
            if (!rOpt.isPresent()) {
                log.warn("Got event about unknown room {} from {}", sEv.getRoomId(), transaction.getOrigin());
            } else {
                rOpt.get().inject(sEv);
            }
        });
    }

}
