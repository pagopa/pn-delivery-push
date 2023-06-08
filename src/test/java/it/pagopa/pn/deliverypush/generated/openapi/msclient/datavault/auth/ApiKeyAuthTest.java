package it.pagopa.pn.deliverypush.generated.openapi.msclient.datavault.auth;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


class ApiKeyAuthTest {

    private final String location = "loc";
    private final String paramName = "name";
    private ApiKeyAuth apiKeyAuth;

    @BeforeEach
    void setUp() {
        apiKeyAuth = new ApiKeyAuth(location, paramName);
    }

    @Test
    void getLocation() {
        String loc = apiKeyAuth.getLocation();
        Assertions.assertEquals("loc", loc);
    }

    @Test
    void getParamName() {
        String name = apiKeyAuth.getParamName();
        Assertions.assertEquals("name", name);
    }

    @Test
    void getApiKey() {
        apiKeyAuth.setApiKey("key");
        String key = apiKeyAuth.getApiKey();
        Assertions.assertEquals("key", key);
    }

    @Test
    void getApiKeyPrefix() {
        apiKeyAuth.setApiKeyPrefix("prefix");
        String prefix = apiKeyAuth.getApiKeyPrefix();
        Assertions.assertEquals("prefix", prefix);
    }
}