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

package io.kamax.mxhsd_test.core.crypto;

import io.kamax.matrix.crypto.KeyManager;
import io.kamax.matrix.crypto.SignatureManager;
import io.kamax.mxhsd.core.GlobalStateHolder;
import io.kamax.mxhsd.core.crypto.CryptoManager;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static junit.framework.TestCase.assertTrue;

public class CryptoManagerTest {

    private CryptoManager mgr;

    @Before
    public void before() {
        GlobalStateHolder state = new GlobalStateHolder();
        state.setDomain("localhost");
        state.setKeyMgr(KeyManager.fromMemory());
        state.setSignMgr(new SignatureManager(state.getKeyMgr(), state.getDomain()));
        mgr = new CryptoManager(state);
    }

    @Test
    public void addTlsCertificate() {
        assertTrue(mgr.getTlsKeys().isEmpty());
        mgr.addTlsKey(new File("src/test/resources/crypto/cryptoMgrTest.crt"));
        assertTrue(mgr.getTlsKeys().size() == 1);
        assertTrue(StringUtils.equals("37KmCUkPRv50zS4fl9kcaLXJrZda4WYZb+e6lRpKo2M", mgr.getTlsKeys().get(0).getFingerprint()));
    }

}
