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

package io.kamax.mxhsd.core.crypto;

import io.kamax.matrix.codec.MxSha256;
import io.kamax.mxhsd.api.crypto.ICryptoManager;
import io.kamax.mxhsd.api.crypto.IOldSigningKey;
import io.kamax.mxhsd.api.crypto.ISigningKey;
import io.kamax.mxhsd.api.crypto.ITlsKey;
import io.kamax.mxhsd.core.HomeserverState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CryptoManager implements ICryptoManager {

    private final Logger log = LoggerFactory.getLogger(CryptoManager.class);

    private HomeserverState global;
    private MxSha256 sha256 = new MxSha256();

    private List<ITlsKey> tlsFingerprints;

    public CryptoManager(HomeserverState global) {
        this.global = global;
        this.tlsFingerprints = new ArrayList<>();
    }

    @Override
    public List<ISigningKey> getKeys() {
        Integer index = global.getKeyMgr().getCurrentIndex();
        String pubKey = global.getKeyMgr().getPublicKeyBase64(index);
        return Collections.singletonList(new Ed25519SigningKey(index.toString(), pubKey));
    }

    @Override
    public List<IOldSigningKey> getOldKeys() {
        return Collections.emptyList();
    }

    @Override
    public List<ITlsKey> getTlsKeys() {
        return Collections.unmodifiableList(tlsFingerprints);
    }

    @Override
    public void addTlsKey(File pubCertFile) {
        log.info("Adding TLS key from {}", pubCertFile.getAbsolutePath());
        try {
            CertificateFactory fact = CertificateFactory.getInstance("X.509");
            FileInputStream is = new FileInputStream(pubCertFile);
            X509Certificate cer = (X509Certificate) fact.generateCertificate(is);
            String fingerprint = sha256.hash(cer.getEncoded());
            log.info("Fingerprint: {}", fingerprint);
            tlsFingerprints.add(new TlsKey(fingerprint));
        } catch (CertificateException | IOException e) {
            throw new RuntimeException(e);
        }
    }

}
