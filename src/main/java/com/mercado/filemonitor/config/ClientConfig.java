package com.mercado.filemonitor.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app-mercado.remote")
@Data
public class ClientConfig {
    @Value("${app-mercado.remote.host}")
    private String host;
    @Value("${app-mercado.remote.port}")
    private String port;
    @Value("${app-mercado.remote.path}")
    private String path;
    @Value("${app-mercado.remote.endpoint-export}")
    private String endpoint;
    @Value("${app-mercado.remote.produtos-endpoint}")
    private String produtos_endpoint;
}