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

package io.kamax.mxhsd.spring.federation;

import io.kamax.mxhsd.spring.common.service.HomeserverService;
import io.kamax.mxhsd.spring.federation.config.FederationConnectorConfig;
import org.apache.coyote.http11.Http11NioProtocol;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;

import java.io.File;

@SpringBootApplication
public class MxhsdFederationApp {

    private FederationConnectorConfig cfg;

    @Autowired
    public MxhsdFederationApp(FederationConnectorConfig cfg, HomeserverService hsSvc) {
        this.cfg = cfg;
        hsSvc.getState().getCryptoMgr().addTlsKey(new File(cfg.getCert()));
    }

    @Bean
    public EmbeddedServletContainerFactory servletContainer() {
        TomcatEmbeddedServletContainerFactory tomcat = new TomcatEmbeddedServletContainerFactory();

        tomcat.addConnectorCustomizers((TomcatConnectorCustomizer) connector -> {
            connector.setScheme("https");
            connector.setPort(cfg.getPort());
            Http11NioProtocol protocol = (Http11NioProtocol) connector.getProtocolHandler();
            protocol.setSSLEnabled(true);
            protocol.setSSLCertificateKeyFile(cfg.getKey());
            protocol.setSSLCertificateFile(cfg.getCert());
            cfg.getChain().ifPresent(protocol::setSSLCertificateChainFile);
        });

        return tomcat;
    }

}
