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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.kamax.matrix._MatrixID;
import io.kamax.matrix.json.MatrixJson;
import io.kamax.mxhsd.GsonUtil;
import io.kamax.mxhsd.api.event.ISignedEvent;
import io.kamax.mxhsd.api.exception.InvalidJsonException;
import io.kamax.mxhsd.api.federation.FederationException;
import io.kamax.mxhsd.api.federation.IFederationClient;
import io.kamax.mxhsd.api.federation.IRemoteAddress;
import io.kamax.mxhsd.core.HomeserverState;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Optional;

public class HttpFederationClient implements IFederationClient {

    private final Logger log = LoggerFactory.getLogger(HttpFederationClient.class);

    private HomeserverState global;
    private FederationDomainResolver resolver;
    private CloseableHttpClient client;

    public HttpFederationClient(HomeserverState global, FederationDomainResolver resolver) {
        this.global = global;
        this.resolver = resolver;

        try {
            // FIXME properly handle SSL context by validating certificate hostname
            SSLContext sslContext = SSLContextBuilder.create().loadTrustMaterial(new TrustAllStrategy()).build();
            HostnameVerifier hostnameVerifier = new NoopHostnameVerifier();
            SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext, hostnameVerifier);
            this.client = HttpClientBuilder.create()
                    .disableAuthCaching()
                    .disableAutomaticRetries()
                    .disableCookieManagement()
                    .disableRedirectHandling()
                    .setSSLSocketFactory(sslSocketFactory)
                    .setDefaultRequestConfig(RequestConfig.custom()
                            .setConnectTimeout(10000) // FIXME make configurable
                            .setConnectionRequestTimeout(60000) // FIXME make configurable
                            .build())
                    .setUserAgent(global.getAppName() + "/" + global.getAppVersion())
                    .build();
        } catch (KeyStoreException | NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException(e);
        }
    }

    protected HttpEntity getJsonEntity(Object o) {
        return EntityBuilder.create()
                .setText(GsonUtil.get().toJson(o))
                .setContentType(ContentType.APPLICATION_JSON)
                .build();
    }

    private String getAuthObj(String remoteDomain, String method, URI target) {
        return getAuthObj(remoteDomain, method, target, null);
    }

    private String getAuthObj(String remoteDomain, String method, URI target, JsonElement content) {
        String uri = target.getRawPath();
        if (StringUtils.isNotBlank(target.getRawQuery())) {
            uri += "?" + target.getRawQuery();
        }

        JsonObject authObj = new JsonObject();
        authObj.addProperty("method", method);
        authObj.addProperty("uri", uri);
        authObj.addProperty("origin", global.getDomain());
        authObj.addProperty("destination", remoteDomain);
        Optional.ofNullable(content).ifPresent(c -> authObj.add("content", c));
        String data = MatrixJson.encodeCanonical(authObj);
        log.debug("Auth object: {}", data);
        return data;
    }

    private JsonObject getBody(HttpEntity entity) throws IOException {
        Charset charset = ContentType.getOrDefault(entity).getCharset();
        String raw = IOUtils.toString(entity.getContent(), charset);
        if (raw.isEmpty()) {
            return new JsonObject();
        }

        try {
            return GsonUtil.parseObj(raw);
        } catch (InvalidJsonException e) {
            return GsonUtil.parse(raw).getAsJsonArray().get(1).getAsJsonObject();
        }
    }

    private JsonObject sendGet(URIBuilder target) {
        try {
            if (!target.getScheme().equals("matrix")) {
                throw new IllegalArgumentException("Scheme can only be matrix");
            }

            String domain = target.getHost();
            target.setScheme("https");
            IRemoteAddress addr = resolver.resolve(target.getHost());
            target.setHost(addr.getHost());
            target.setPort(addr.getPort());

            return sendGet(domain, target.build());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private JsonObject sendGet(String domain, URI target) {
        String authObj = getAuthObj(domain, "GET", target);
        String sign = global.getSignMgr().sign(authObj);
        String key = "ed25519:" + global.getKeyMgr().getCurrentIndex();

        HttpGet req = new HttpGet(target);
        req.setHeader("Host", domain);
        req.setHeader("Authorization",
                "X-Matrix origin=" + global.getDomain() + ",key=\"" + key + "\",sig=\"" + sign + "\"");
        log.info("Calling [{}] {}", domain, req);
        try (CloseableHttpResponse res = client.execute(req)) {
            int resStatus = res.getStatusLine().getStatusCode();
            JsonObject body = getBody(res.getEntity());
            if (resStatus == 200) {
                log.info("Got answer");
                return body;
            } else {
                throw new FederationException(resStatus, body);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private JsonObject sendPost(URIBuilder target, JsonElement payload) {
        try {
            if (!target.getScheme().equals("matrix")) {
                throw new IllegalArgumentException("Scheme can only be matrix");
            }

            String domain = target.getHost();
            target.setScheme("https");
            IRemoteAddress addr = resolver.resolve(target.getHost());
            target.setHost(addr.getHost());
            target.setPort(addr.getPort());

            return sendPost(domain, target.build(), payload);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private JsonObject sendPost(String domain, URI target, JsonElement payload) {
        String authObj = getAuthObj(domain, "POST", target, payload);
        String sign = global.getSignMgr().sign(authObj);
        String key = "ed25519:" + global.getKeyMgr().getCurrentIndex();

        HttpPost req = new HttpPost(target);
        req.setEntity(getJsonEntity(payload));
        req.setHeader("Host", domain);
        req.setHeader("Authorization",
                "X-Matrix origin=" + global.getDomain() + ",key=\"" + key + "\",sig=\"" + sign + "\"");
        log.info("Calling [{}] {}", domain, req);
        log.debug("Payload: {}", GsonUtil.getPrettyForLog(payload));
        try (CloseableHttpResponse res = client.execute(req)) {
            int resStatus = res.getStatusLine().getStatusCode();
            JsonObject body = getBody(res.getEntity());
            if (resStatus == 200) {
                log.info("Got answer");
                return body;
            } else {
                throw new FederationException(resStatus, body);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private JsonObject sendPut(URIBuilder target, JsonElement payload) {
        try {
            if (!target.getScheme().equals("matrix")) {
                throw new IllegalArgumentException("Scheme can only be matrix");
            }

            String domain = target.getHost();
            target.setScheme("https");
            IRemoteAddress addr = resolver.resolve(target.getHost());
            target.setHost(addr.getHost());
            target.setPort(addr.getPort());

            return sendPut(domain, target.build(), payload);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private JsonObject sendPut(String domain, URI target, JsonElement payload) {
        String authObj = getAuthObj(domain, "PUT", target, payload);
        String sign = global.getSignMgr().sign(authObj);
        String key = "ed25519:" + global.getKeyMgr().getCurrentIndex();

        HttpPut req = new HttpPut(target);
        req.setEntity(getJsonEntity(payload));
        req.setHeader("Host", domain);
        req.setHeader("Authorization",
                "X-Matrix origin=" + global.getDomain() + ",key=\"" + key + "\",sig=\"" + sign + "\"");
        log.info("Calling [{}] {}", domain, req);
        log.debug("Payload: {}", GsonUtil.getPrettyForLog(payload));
        try (CloseableHttpResponse res = client.execute(req)) {
            int resStatus = res.getStatusLine().getStatusCode();
            JsonObject body = getBody(res.getEntity());
            if (resStatus == 200) {
                log.info("Got answer");
                return body;
            } else {
                throw new FederationException(resStatus, body);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private JsonObject sendDelete(String domain, String path, JsonObject playload) {
        throw new NotImplementedException("");
    }

    private URIBuilder getUri(String domain, String path) {
        return new URIBuilder(URI.create("matrix://" + domain + path));
    }

    @Override
    public JsonObject send(String domain, String method, String url, Map<String, String> parameters, JsonElement body) {
        URIBuilder b = getUri(domain, url);
        if (parameters != null && !parameters.isEmpty()) {
            parameters.forEach(b::addParameter);
        }

        if (StringUtils.equals("GET", method)) {
            if (body != null) {
                throw new IllegalArgumentException("Cannot send body with GET");
            }

            return sendGet(b);
        } else if (StringUtils.equals("POST", method)) {
            return sendPost(b, body);
        } else if (StringUtils.equals("PUT", method)) {
            return sendPut(b, body);
        } else {
            throw new NotImplementedException(method);
        }
    }

    @Override
    public JsonObject makeJoin(String residentHsDomain, String roomId, _MatrixID joiner) {
        // FIXME refactor URL from Spring classes
        return sendGet(getUri(residentHsDomain, "/_matrix/federation/v1/make_join/" + roomId + "/" + joiner.getId()));
    }

    @Override
    public JsonObject sendJoin(String residentHsDomain, ISignedEvent ev) {
        // FIXME refactor URL from Spring classes
        return sendPut(getUri(residentHsDomain, "/_matrix/federation/v1/send_join/" + ev.getRoomId() + "/" + ev.getId()), ev.getJson());
    }

    @Override
    public JsonObject sendTransaction(String domain, String id, JsonObject o) {
        return sendPut(getUri(domain, "/_matrix/federation/v1/send/" + id + "/"), o);
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
    public JsonObject query(String domain, String type, Map<String, String> parameters) {
        // FIXME refactor URL from Spring classes
        URIBuilder b = getUri(domain, "/_matrix/federation/v1/query/" + type);
        if (parameters != null && !parameters.isEmpty()) {
            parameters.forEach(b::addParameter);
        }
        return sendGet(b);
    }

}
