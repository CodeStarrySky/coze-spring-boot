package com.wuch.coze.api;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;


@ConfigurationProperties(prefix = "coze")
@Data
@NoArgsConstructor
public class CozeProperties {

    private String baseUrl;

    private String botId;

    private Boolean autoSaveHistory = true;

    private Integer readTimeout = 10000;

    private Integer connectTimeout = 10000;

    @NestedConfigurationProperty
    private Auth auth = new Auth();

    @Data
    @NoArgsConstructor
    public static class Auth {
        public static final String TYPE_TOKEN = "token";

        public static final String TYPE_JWT = "jwt";

        private String type = "token";

        private String token;

        private String clientId;

        private String privateKeyFilePath;

        private String publicKey;

        private Integer ttl = 600;
    }
}
