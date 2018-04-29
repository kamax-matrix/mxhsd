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

package io.kamax.mxhsd.api.federation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.kamax.matrix._MatrixID;
import io.kamax.mxhsd.api.event.IEvent;

import java.util.Map;

public interface IFederationClient {

    JsonObject send(String domain, String method, String url, Map<String, String> parameters, JsonElement body);

    JsonObject makeJoin(String residentHsDomain, String roomId, _MatrixID joiner);

    JsonObject sendJoin(String residentHsDomain, IEvent ev);

    JsonObject sendTransaction(String domain, String id, JsonObject o);

    JsonObject getRoomState(String domain, String roomId, String eventId);

    JsonObject getRoomStateIds(String domain, String roomId, String eventId);

    JsonObject getEvent(String domain, String id);

    JsonObject backfill(String domain, String fromEventId, long limit);

    JsonObject frontfill(String domain, String fromEventId, long limit);

    JsonObject query(String domain, String type, Map<String, String> parameters);

}
