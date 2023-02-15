package it.pagopa.pn.deliverypush.action.it.mockbean;

import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.publicregistry.PublicRegistryResponse;
import it.pagopa.pn.deliverypush.middleware.externalclient.publicregistry.NationalRegistriesClient;
import it.pagopa.pn.deliverypush.middleware.responsehandler.PublicRegistryResponseHandler;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.nationalregistries.generated.openapi.clients.nationalregistries.model.CheckTaxIdOK;
import org.junit.jupiter.api.Assertions;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.awaitility.Awaitility.await;

public class NationalRegistriesClientMock implements NationalRegistriesClient {

    public static final String NOT_VALID = "NOT_VALID";
    public static final String EXCEPTION = "EXCEPTION";
    private final PublicRegistryResponseHandler publicRegistryResponseHandler;
    private ConcurrentMap<String, LegalDigitalAddressInt> digitalAddressResponse;
    private final TimelineService timelineService;


    public NationalRegistriesClientMock(
            PublicRegistryResponseHandler publicRegistryResponseHandler,
            TimelineService timelineService
    ) {
        this.publicRegistryResponseHandler = publicRegistryResponseHandler;
        this.timelineService = timelineService;
    }

    public void clear() {
        this.digitalAddressResponse = new ConcurrentHashMap<>();
    }

    public void addDigital(String key, LegalDigitalAddressInt value) {
        this.digitalAddressResponse.put(key,value);
    }

    @Override
    public void sendRequestForGetDigitalAddress(String taxId, String recipientType, String correlationId) {
        new Thread(() -> {
            // Viene atteso fino a che l'elemento di timeline relativo all'invio verso extChannel sia stato inserito
            //timelineEventId = <CATEGORY_VALUE>#IUN_<IUN_VALUE>#RECINDEX_<RECINDEX_VALUE>
            String iunFromElementId = correlationId.split("#")[1];
            String iun = iunFromElementId.replace("IUN_", "");
            await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                    Assertions.assertTrue(timelineService.getTimelineElement(iun, correlationId).isPresent())
            );

            Assertions.assertDoesNotThrow(() -> {
                simulateDigitalAddressResponse(taxId, correlationId);
            });
        }).start();
    }

    @Override
    public CheckTaxIdOK checkTaxId(String taxId) {
        if(taxId.contains(NOT_VALID)){
            return new CheckTaxIdOK()
                    .taxId(taxId)
                    .isValid(false)
                    .errorCode(CheckTaxIdOK.ErrorCodeEnum.ERR01);
        } else if (taxId.contains(EXCEPTION)){
            throw new RuntimeException("mock exception from server");
        }

        return new CheckTaxIdOK()
                .taxId(taxId)
                .isValid(true);
    }

    private void simulateDigitalAddressResponse(String taxId, String correlationId) {
        LegalDigitalAddressInt address = this.digitalAddressResponse.get(taxId);

        PublicRegistryResponse response = PublicRegistryResponse.builder()
                .correlationId(correlationId)
                .digitalAddress(address)
                .build();
        publicRegistryResponseHandler.handleResponse(response);
    }

}
