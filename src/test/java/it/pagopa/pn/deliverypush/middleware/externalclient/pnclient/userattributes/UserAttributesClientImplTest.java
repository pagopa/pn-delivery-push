package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.userattributes;

import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.safestorage.PnSafeStorageClientImpl;
import it.pagopa.pn.userattributes.generated.openapi.clients.userattributes.ApiClient;
import it.pagopa.pn.userattributes.generated.openapi.clients.userattributes.api.CourtesyApi;
import it.pagopa.pn.userattributes.generated.openapi.clients.userattributes.api.LegalApi;
import it.pagopa.pn.userattributes.generated.openapi.clients.userattributes.model.LegalDigitalAddress;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockserver.integration.ClientAndServer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;

class UserAttributesClientImplTest {

    @Mock
    private CourtesyApi courtesyApi;

    @Mock
    private LegalApi legalApi;

    private static ClientAndServer mockServer;
    @BeforeAll
    public static void startMockServer() {
        mockServer = startClientAndServer(9998);
    }

    @AfterAll
    public static void stopMockServer() {
        mockServer.stop();
    }

    @Qualifier("withTracing")
    RestTemplate restTemplate;

    @Mock
    PnDeliveryPushConfigs cfg;

    UserAttributesClientImpl userAttributesClient;

    private PnDeliveryPushConfigs pnDeliveryPushConfigs;

    @BeforeEach
    void setup() {
        this.cfg = mock( PnDeliveryPushConfigs.class );
        Mockito.when( cfg.getSafeStorageBaseUrl() ).thenReturn( "http://localhost:8080" );
        this.userAttributesClient = new UserAttributesClientImpl( restTemplate, cfg );
//        restTemplate = Mockito.mock(RestTemplate.class);
//        pnDeliveryPushConfigs = new PnDeliveryPushConfigs();
//        pnDeliveryPushConfigs.setWebapp(new PnDeliveryPushConfigs.Webapp());
//        pnDeliveryPushConfigs.setUserAttributesBaseUrl("http://localhost:8080");
//
//        ApiClient newApiClient = new ApiClient(restTemplate);
//
//        courtesyApi = new CourtesyApi(newApiClient);
//        legalApi = new LegalApi(newApiClient);
//        client = new UserAttributesClientImpl(restTemplate, pnDeliveryPushConfigs);
    }

    @Test
    void getLegalAddressBySender() {

        ResponseEntity<List<LegalDigitalAddress>> res = userAttributesClient.getLegalAddressBySender("01", "02");

        System.out.println("CLIENT : ");
    }

    @Test
    void getCourtesyAddressBySender() {
    }
}