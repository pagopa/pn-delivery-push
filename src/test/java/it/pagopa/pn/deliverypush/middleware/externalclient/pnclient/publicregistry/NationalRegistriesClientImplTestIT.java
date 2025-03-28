package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.publicregistry;

import static it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.nationalregistries.NationalRegistriesClientImpl.PN_NATIONAL_REGISTRIES_CX_ID_VALUE;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import it.pagopa.pn.deliverypush.MockAWSObjectsTest;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.nationalregistries.NationalRegistriesClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "pn.delivery-push.national-registries-base-url=http://localhost:9999"
})
class NationalRegistriesClientImplTestIT extends MockAWSObjectsTest{

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
                () -> nationalRegistriesClient.sendRequestForGetDigitalAddress("001", "PF", "002", Instant.now()));
    }

}
