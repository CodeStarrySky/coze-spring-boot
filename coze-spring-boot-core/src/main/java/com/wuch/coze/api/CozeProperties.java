package com.wuch.coze.api;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;


@ConfigurationProperties(prefix = "coze")
@Data
@NoArgsConstructor
public class CozeProperties {

    private String token;

    private String baseUrl;

    private String botId;

    private Boolean autoSaveHistory = true;

    private Integer readTimeout = 10000;

    private Integer connectTimeout = 10000;

}
