package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.mandate;

import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.mandate.generated.openapi.clients.mandate.ApiClient;
import it.pagopa.pn.mandate.generated.openapi.clients.mandate.api.MandatePrivateServiceApi;
import it.pagopa.pn.mandate.generated.openapi.clients.mandate.model.InternalMandateDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;

class PnMandateClientImplTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private PnDeliveryPushConfigs cfg;

    @Mock
    private MandatePrivateServiceApi mandatesApi;

    private PnMandateClientImpl pnMandateClient;

    @BeforeEach
    void setUp() {
        cfg = mock(PnDeliveryPushConfigs.class);
        Mockito.when(cfg.getMandateBaseUrl()).thenReturn("http://localhost:8080");

        restTemplate = Mockito.mock(RestTemplate.class);
        ApiClient apiClient = new ApiClient(restTemplate);
        apiClient.setBasePath(cfg.getMandateBaseUrl());

        mandatesApi = new MandatePrivateServiceApi(apiClient);

        pnMandateClient = new PnMandateClientImpl(restTemplate, cfg);
    }

    @Test
    void listMandatesByDelegate() {
        String delegated = "001";
        String mandateId = "002";
        InternalMandateDto internalMandateDto = new InternalMandateDto();
        internalMandateDto.setMandateId(mandateId);
        internalMandateDto.setDelegate(delegated);
        List<InternalMandateDto> internalMandateDtos = new ArrayList<>();
        internalMandateDtos.add(internalMandateDto);

        /*
        Mockito.when(restTemplate.exchange(Mockito.any(RequestEntity.class), Mockito.any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(""));

        Mockito.when(mandatesApi.listMandatesByDelegate(delegated, mandateId)).thenReturn(internalMandateDtos);

        List<InternalMandateDto> actual = pnMandateClient.listMandatesByDelegate(delegated, mandateId);

        Assertions.assertEquals(1, actual.size());
        */
    }
}