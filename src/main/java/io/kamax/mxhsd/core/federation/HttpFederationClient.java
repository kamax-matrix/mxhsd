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

package io.kamax.mxhsd.core.federation;

import com.google.gson.JsonObject;
import io.kamax.matrix._MatrixID;
import io.kamax.mxhsd.GsonUtil;
import io.kamax.mxhsd.api.federation.FederationException;
import io.kamax.mxhsd.api.federation._FederationClient;
import io.kamax.mxhsd.core.HomeserverState;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.DefaultHttpRequestFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Optional;

public class HttpFederationClient implements _FederationClient {

    private HomeserverState global;
    private FederationDomainResolver resolver;
    private CloseableHttpClient client;

    private DefaultHttpRequestFactory reqFactory = new DefaultHttpRequestFactory();

    public HttpFederationClient(HomeserverState global, FederationDomainResolver resolver) {
        this.global = global;
        this.resolver = resolver;

        this.client = HttpClientBuilder.create()
                .setUserAgent(global.getAppName() + "/" + global.getAppVersion())
                .build();
    }

    protected HttpEntity getJsonEntity(Object o) {
        return EntityBuilder.create()
                .setText(GsonUtil.get().toJson(o))
                .setContentType(ContentType.APPLICATION_JSON)
                .build();
    }

    private JsonObject getAuthObj(String remoteDomain, String method, String path) {
        return getAuthObj(remoteDomain, method, path, null);
    }

    private JsonObject getAuthObj(String remoteDomain, String method, String path, JsonObject content) {
        JsonObject authObj = new JsonObject();
        authObj.addProperty("method", "GET");
        authObj.addProperty("uri", path);
        authObj.addProperty("origin", global.getDomain());
        authObj.addProperty("destination", remoteDomain);
        Optional.ofNullable(content).ifPresent(c -> authObj.add("content", c));
        return authObj;
    }

    private JsonObject getBody(HttpEntity entity) throws IOException {
        Charset charset = ContentType.getOrDefault(entity).getCharset();
        String raw = IOUtils.toString(entity.getContent(), charset);
        if (raw.isEmpty()) {
            return new JsonObject();
        }

        return GsonUtil.parseObj(raw);
    }

    private JsonObject sendGet(String domain, String path) {
        JsonObject authObj = getAuthObj(domain, "GET", path);
        String sign = global.getSignMgr().sign(GsonUtil.get().toJson(authObj));
        String key = "ed25519:" + global.getKeyMgr().getCurrentIndex();
        String target = "https://" + resolver.resolve(domain) + path;

        HttpGet req = new HttpGet(target);
        req.setHeader("Authentication",
                "X-Matrix origin=\"" + global.getDomain() + "\",key=\"" + key + "\",sign=\"" + sign + "\"");
        try (CloseableHttpResponse res = client.execute(HttpHost.create(domain), req)) {
            JsonObject body = getBody(res.getEntity());
            int resStatus = res.getStatusLine().getStatusCode();
            if (resStatus == 200) {
                return body;
            } else {
                throw new FederationException(resStatus, body);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private JsonObject sendPost(String domain, String path, JsonObject playload) {
        JsonObject authObj = getAuthObj(domain, "POST", path);
        String sign = global.getSignMgr().sign(GsonUtil.get().toJson(authObj));
        String key = "ed25519:" + global.getKeyMgr().getCurrentIndex();
        String target = "https://" + resolver.resolve(domain) + path;

        HttpPost req = new HttpPost(target);
        req.setEntity(getJsonEntity(path));
        req.setHeader("Authentication",
                "X-Matrix origin=\"" + global.getDomain() + "\",key=\"" + key + "\",sign=\"" + sign + "\"");
        try (CloseableHttpResponse res = client.execute(HttpHost.create(domain), req)) {
            JsonObject body = getBody(res.getEntity());
            int resStatus = res.getStatusLine().getStatusCode();
            if (resStatus == 200) {
                return body;
            } else {
                throw new FederationException(resStatus, body);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private JsonObject sendPut(String domain, String path, JsonObject playload) {
        throw new NotImplementedException("");
    }

    private JsonObject sendDelete(String domain, String path, JsonObject playload) {
        throw new NotImplementedException("");
    }

    @Override
    public JsonObject makeJoin(String residentHsDomain, String roomId, _MatrixID joiner) {
        return sendGet(residentHsDomain, "/_matrix/federation/v1/make_join/" + roomId + "/" + joiner.getId());
    }

    @Override
    public JsonObject sendJoin(JsonObject o) {
        throw new NotImplementedException("");
    }

    @Override
    public JsonObject sendTransaction(JsonObject o) {
        throw new NotImplementedException("");
    }

    @Override
    public JsonObject getRoomState(String roomId) {
        throw new NotImplementedException("");
    }

    @Override
    public JsonObject getEvent(String id) {
        throw new NotImplementedException("");
    }

    @Override
    public JsonObject backfill(String fromEventId, long limit) {
        throw new NotImplementedException("");
    }

    @Override
    public JsonObject frontfill(String fromEventId, long limit) {
        throw new NotImplementedException("");
    }

    @Override
    public JsonObject query(String type, Map<String, String> parameters) {
        throw new NotImplementedException("");
    }

}
