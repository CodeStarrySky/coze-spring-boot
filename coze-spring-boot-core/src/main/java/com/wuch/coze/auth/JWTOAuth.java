package com.wuch.coze.auth;

import com.coze.openapi.service.auth.Auth;
import com.coze.openapi.service.auth.JWTOAuthClient;
import com.wuch.coze.api.CozeProperties;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@RequiredArgsConstructor
public class JWTOAuth implements CozeAuth{

    private static final Logger log = LoggerFactory.getLogger(JWTOAuth.class);

    private final CozeProperties properties;


    @Override
    public Auth getAuth() {
        CozeProperties.Auth auth = properties.getAuth();
        JWTOAuthClient oauth = null;
        String privateKey = null;
        try {
            privateKey = Files.readString(Paths.get(auth.getPrivateKeyFilePath()));
        } catch (IOException e) {
            log.error("Failed to read the private key file！", e);
        }
        try {
            oauth = new JWTOAuthClient.JWTOAuthBuilder()
                    .clientID(auth.getClientId())
                    .privateKey(privateKey)
                    .publicKey(auth.getPublicKey())
                    .baseURL(properties.getBaseUrl())
                    .build();
        } catch (Exception e) {
            log.error("OAuth Client construction failed！", e);
        }
        assert oauth != null : "The obtained token is null";
        return com.coze.openapi.service.auth.JWTOAuth.builder().jwtClient(oauth).ttl(auth.getTtl()).build();
    }


}
