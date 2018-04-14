/*
 * mxhsd - Corporate Matrix Homeserver
 * Copyright (C) 2018 Maxime Dor
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

package io.kamax.mxhsd_test.core.event;

import com.google.gson.JsonObject;
import io.kamax.matrix.crypto.KeyManager;
import io.kamax.matrix.crypto.KeyMemoryStore;
import io.kamax.matrix.crypto.SignatureManager;
import io.kamax.matrix.json.MatrixJson;
import io.kamax.mxhsd.GsonUtil;
import io.kamax.mxhsd.core.HomeserverState;
import io.kamax.mxhsd.core.event.EventManager;
import io.kamax.mxhsd_test.TestData;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static junit.framework.TestCase.assertTrue;

public class EventManagerTest {

    private final Logger log = LoggerFactory.getLogger(EventManagerTest.class);
    private String prefix = "src/test/resources/events/";

    private static EventManager evMgr;

    private String loadFrom(String path) throws IOException {
        return IOUtils.toString(new FileInputStream(path), StandardCharsets.UTF_8);
    }

    @BeforeClass
    public static void beforeClass() {
        HomeserverState s = new HomeserverState();
        s.setDomain("synapse.local.kamax.io");
        s.setKeyMgr(new KeyManager(new KeyMemoryStore(TestData.SIGN_KEY_SEED)));
        s.setSignMgr(new SignatureManager(s.getKeyMgr(), s.getDomain()));
        evMgr = new EventManager(s);
    }

    public void testSign(String source, String control) throws IOException {
        JsonObject raw = GsonUtil.parseObj(loadFrom(source));
        String signedGood = MatrixJson.encodeCanonical(loadFrom(control));
        String signed = MatrixJson.encodeCanonical(evMgr.hashAndSign(raw));

        log.info("Signed JSON: {}", signed);
        log.info("Valid JSON: {}", signedGood);
        assertTrue(StringUtils.equals(signed, signedGood));
    }

    @Test
    public void signCreate() throws IOException {
        testSign(prefix + "m.room.create-raw.txt", prefix + "m.room.create-signed.txt");
    }

    @Test
    public void signMessage() throws IOException {
        testSign(prefix + "m.room.message-raw.txt", prefix + "m.room.message-signed.txt");
    }

}
