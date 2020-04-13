/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.pivotal.mtlssample.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.stereotype.Component;

@Component
public class AppContainerCustomizer implements WebServerFactoryCustomizer<ConfigurableWebServerFactory> {
    private final Integer httpPort;
    private final Integer httpsPort;

    public AppContainerCustomizer(@Autowired HttpServerProperties httpProperties, @Autowired ServerProperties serverProperties){
        this.httpPort = httpProperties.getPort();
        this.httpsPort = serverProperties.getPort();
    }

    @Override
    public void customize(ConfigurableWebServerFactory factory) {
        if (this.httpPort != null) {
            if (this.httpsPort != null) {
                factory.setPort(this.httpsPort.equals(this.httpPort) ? 8443 : this.httpsPort);
            }
        }
    }
}
