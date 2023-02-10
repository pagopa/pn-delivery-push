package it.pagopa.pn.deliverypush.action.it.mockbean;

import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.publicregistry.PublicRegistryResponse;
import it.pagopa.pn.deliverypush.middleware.externalclient.publicregistry.PublicRegistry;
import it.pagopa.pn.deliverypush.middleware.responsehandler.PublicRegistryResponseHandler;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.junit.jupiter.api.Assertions;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.awaitility.Awaitility.await;

public class PublicRegistryMock implements PublicRegistry {

    private final PublicRegistryResponseHandler publicRegistryResponseHandler;
    private ConcurrentMap<String, LegalDigitalAddressInt> digitalAddressResponse;
    private final TimelineService timelineService;


    public PublicRegistryMock(
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
            //public_registry_call-IUN_123456789-RECINDEX_1-DELIVERYMODE_DIGITAL-CONTACTPHASE_CHOOSE_DELIVERY-SENTATTEMPTMADE_1-
            String iunFromElementId = correlationId.split("-")[1];
            String iun = iunFromElementId.replace("IUN_", "");
            await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                    Assertions.assertTrue(timelineService.getTimelineElement(iun, correlationId).isPresent())
            );

            Assertions.assertDoesNotThrow(() -> {
                simulateDigitalAddressResponse(taxId, correlationId);
            });
        }).start();
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
