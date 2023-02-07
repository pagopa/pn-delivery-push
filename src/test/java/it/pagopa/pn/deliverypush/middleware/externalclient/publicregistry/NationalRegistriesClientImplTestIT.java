package it.pagopa.pn.deliverypush.middleware.externalclient.publicregistry;

import it.pagopa.pn.deliverypush.LocalStackTestConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static it.pagopa.pn.deliverypush.middleware.externalclient.publicregistry.NationalRegistriesClientImpl.PN_NATIONAL_REGISTRIES_CX_ID_VALUE;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "pn.delivery-push.national-registries-base-url=http://localhost:9999"
})
@Import(LocalStackTestConfig.class)
class NationalRegistriesClientImplTestIT {

    private static final String PN_NATIONAL_REGISTRIES_CX_ID = "pn-national-registries-cx-id";

    private static ClientAndServer mockServer;

    @Autowired
    private NationalRegistriesClient nationalRegistriesClient;


    @BeforeAll
    public static void startMockServer() {
        mockServer = startClientAndServer(9999);
    }

    @AfterAll
    public static void stopMockServer() {
        mockServer.stop();
    }

    @Test
    void sendRequestForGetDigitalAddressTest() {

        new MockServerClient("localhost", 9999)
                .when(request()
                        .withMethod("POST")
                        .withPath("/national-registries-private/{recipient-type}/addresses".replace("{recipient-type}", "PF"))
                        .withHeader(PN_NATIONAL_REGISTRIES_CX_ID, PN_NATIONAL_REGISTRIES_CX_ID_VALUE))
                .respond(response()
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withStatusCode(200));

        Assertions.assertDoesNotThrow(
                () -> nationalRegistriesClient.sendRequestForGetDigitalAddress("001", "PF", "002")
        );
    }

}
