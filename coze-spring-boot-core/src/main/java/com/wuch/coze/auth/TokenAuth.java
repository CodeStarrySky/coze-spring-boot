package com.wuch.coze.auth;

import com.coze.openapi.service.auth.Auth;
import com.wuch.coze.api.CozeProperties;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TokenAuth implements CozeAuth{

    private final CozeProperties properties;

    @Override
    public Auth getAuth() {
        return new com.coze.openapi.service.auth.TokenAuth(properties.getAuth().getToken());
    }

}
