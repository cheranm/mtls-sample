package io.pivotal.mtlssample.server;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "server.http")
@Data
public class HttpServerProperties {
    /**
     * Server HTTP port.
     */
    private Integer port;

    public HttpServerProperties() {
    }
}