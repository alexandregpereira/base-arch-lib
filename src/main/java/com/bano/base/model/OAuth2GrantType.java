package com.bano.base.model;

/**
 * Created by henrique.oliveira on 11/10/2017.
 */

public enum OAuth2GrantType {
    PASSWORD("password"),
    CLIENT_CREDENTIALS("client_credentials"),
    REFRESH_TOKEN("refresh_token"),
    AUTHORIZATION_CODE("authorization_code");

    private final String property;

    OAuth2GrantType(String property) {
        this.property = property;
    }

    public String getProperty() {
        return property;
    }

    @Override
    public String toString() {
        return this.property;
    }
}
