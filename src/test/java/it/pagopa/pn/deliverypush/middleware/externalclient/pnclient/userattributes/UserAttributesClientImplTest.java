package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.userattributes;

import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.userattributes.generated.openapi.clients.userattributes.ApiClient;
import it.pagopa.pn.userattributes.generated.openapi.clients.userattributes.api.CourtesyApi;
import it.pagopa.pn.userattributes.generated.openapi.clients.userattributes.api.LegalApi;
import it.pagopa.pn.userattributes.generated.openapi.clients.userattributes.model.CourtesyChannelType;
import it.pagopa.pn.userattributes.generated.openapi.clients.userattributes.model.CourtesyDigitalAddress;
import it.pagopa.pn.userattributes.generated.openapi.clients.userattributes.model.LegalChannelType;
import it.pagopa.pn.userattributes.generated.openapi.clients.userattributes.model.LegalDigitalAddress;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.mock;

class UserAttributesClientImplTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private PnDeliveryPushConfigs cfg;

    @Mock
    private CourtesyApi courtesyApi;

    @Mock
    private LegalApi legalApi;

    private UserAttributesClientImpl client;

    @BeforeEach
    void setup() {
        cfg = mock(PnDeliveryPushConfigs.class);
        Mockito.when(cfg.getUserAttributesBaseUrl()).thenReturn("http://localhost:8080");

        restTemplate = Mockito.mock(RestTemplate.class);
        ApiClient apiClient = new ApiClient(restTemplate);
        apiClient.setBasePath(cfg.getUserAttributesBaseUrl());

        this.courtesyApi = new CourtesyApi(apiClient);
        this.legalApi = new LegalApi(apiClient);

        client = new UserAttributesClientImpl(restTemplate, cfg);
    }

    @Test
    void getLegalAddressBySender() {
        LegalDigitalAddress legalDigitalAddress = new LegalDigitalAddress();
        legalDigitalAddress.setValue("indirizzo@prova.com");
        legalDigitalAddress.setChannelType(LegalChannelType.PEC);
        legalDigitalAddress.recipientId("001");
        legalDigitalAddress.senderId("001");

        List<LegalDigitalAddress> listLegalDigitalAddresses = Collections.singletonList(legalDigitalAddress);

        Mockito.when(restTemplate.exchange(Mockito.any(RequestEntity.class), Mockito.any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(""));

        Mockito.when(legalApi.getLegalAddressBySenderWithHttpInfo("001", "001")).thenReturn(ResponseEntity.ok(listLegalDigitalAddresses));

        ResponseEntity<List<LegalDigitalAddress>> response = client.getLegalAddressBySender("001", "001");

        Assertions.assertEquals(response, ResponseEntity.ok(listLegalDigitalAddresses));
    }

    @Test
    void getCourtesyAddressBySender() {

        CourtesyDigitalAddress courtesyDigitalAddress = new CourtesyDigitalAddress();
        courtesyDigitalAddress.setValue("indirizzo@prova.com");
        courtesyDigitalAddress.setChannelType(CourtesyChannelType.EMAIL);
        courtesyDigitalAddress.recipientId("001");
        courtesyDigitalAddress.senderId("001");

        List<CourtesyDigitalAddress> courtesyDigitalAddressList = Collections.singletonList(courtesyDigitalAddress);

        Mockito.when(restTemplate.exchange(Mockito.any(RequestEntity.class), Mockito.any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(""));

        Mockito.when(courtesyApi.getCourtesyAddressBySenderWithHttpInfo("001", "001")).thenReturn(ResponseEntity.ok(courtesyDigitalAddressList));

        ResponseEntity<List<CourtesyDigitalAddress>> response = client.getCourtesyAddressBySender("001", "001");

        Assertions.assertEquals(response, ResponseEntity.ok(courtesyDigitalAddressList));
    }
}