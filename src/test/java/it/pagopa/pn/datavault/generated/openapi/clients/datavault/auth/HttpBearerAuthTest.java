package it.pagopa.pn.datavault.generated.openapi.clients.datavault.auth;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HttpBearerAuthTest {

    private HttpBearerAuth bearerAuth;

    private final String scheme = "scheme";

    @BeforeEach
    void setUp() {
        bearerAuth = new HttpBearerAuth(scheme);
        bearerAuth.setBearerToken("token");
    }

    @Test
    void getBearerToken() {
        String token = bearerAuth.getBearerToken();
        Assertions.assertEquals("token", token);
    }
}