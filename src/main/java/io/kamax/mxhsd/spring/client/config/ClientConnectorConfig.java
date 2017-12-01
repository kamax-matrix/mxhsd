package io.kamax.mxhsd.spring.client.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("client.connector")
public class ClientConnectorConfig {

    private Integer port = 8008;

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

}
