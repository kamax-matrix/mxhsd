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

import io.kamax.mxhsd.api.event.ISignedEvent;
import io.kamax.mxhsd.api.room.RoomEventType;
import io.kamax.mxhsd.api.room.directory.IRoomAliasLookup;
import io.kamax.mxhsd.api.room.directory.IRoomDirectory;
import io.kamax.mxhsd.api.room.event.RoomAliasEvent;
import io.kamax.mxhsd.core.HomeserverState;
import net.engio.mbassy.listener.Handler;
import org.apache.commons.lang.StringUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RoomDirectory implements IRoomDirectory {

    private HomeserverState global;

    private Map<String, String> mappings = new ConcurrentHashMap<>();

    public RoomDirectory() {
    }

    public RoomDirectory(HomeserverState global) {
        this.global = global;
        global.getEvMgr().addListener(this);
    }

    @Handler
    private void handleEvents(ISignedEvent ev) {
        if (!StringUtils.equals(global.getDomain(), ev.getOrigin())) {
            return;
        }

        List<String> aliases = new ArrayList<>();
        if (RoomEventType.Aliases.is(ev.getId())) {
            aliases.addAll(new RoomAliasEvent(ev).getAliases());
        }

        aliases.forEach(a -> mappings.put(a, ev.getRoomId()));
    }

    @Override
    public Optional<IRoomAliasLookup> lookup(String alias) {
        return Optional.ofNullable(mappings.get(alias))
                // FIXME lookup server list
                .map(id -> new RoomAliasLookup(id, alias, Collections.singletonList(global.getDomain())));
    }

    @Override
    public void add(String alias, String roomId) {
        if (lookup(alias).isPresent()) {
            throw new IllegalStateException("Room alias " + alias + " already assigned to " + roomId);
        }

        mappings.put(alias, roomId);
    }

    @Override
    public void remove(String alias) {
        if (Objects.isNull(mappings.remove(alias))) {
            throw new IllegalArgumentException("room alias " + alias + " does not exist");
        }
    }

}
