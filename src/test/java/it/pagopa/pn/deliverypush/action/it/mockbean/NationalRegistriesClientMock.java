package it.pagopa.pn.deliverypush.action.it.mockbean;

import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.publicregistry.NationalRegistriesResponse;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventIdBuilder;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.nationalregistries.model.CheckTaxIdOK;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.nationalregistries.NationalRegistriesClient;
import it.pagopa.pn.deliverypush.middleware.responsehandler.NationalRegistriesResponseHandler;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.utils.ThreadPool;
import org.junit.jupiter.api.Assertions;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.awaitility.Awaitility.await;

public class NationalRegistriesClientMock implements NationalRegistriesClient {
    
    public static final String NOT_VALID = "NOT_VALID";
    public static final String EXCEPTION = "EXCEPTION";
    private final NationalRegistriesResponseHandler nationalRegistriesResponseHandler;
    private ConcurrentMap<String, LegalDigitalAddressInt> digitalAddressResponse;
    private ConcurrentMap<String, LegalDigitalAddressInt> digitalAddressResponseSecondCycle;
    private final TimelineService timelineService;


    public NationalRegistriesClientMock(
            NationalRegistriesResponseHandler nationalRegistriesResponseHandler,
            TimelineService timelineService
    ) {
        this.nationalRegistriesResponseHandler = nationalRegistriesResponseHandler;
        this.timelineService = timelineService;
        this.clear();
    }

    public void clear() {
        this.digitalAddressResponse = new ConcurrentHashMap<>();
        this.digitalAddressResponseSecondCycle = new ConcurrentHashMap<>();
    }

    public void addDigital(String key, LegalDigitalAddressInt value) {
        this.digitalAddressResponse.put(key,value);
        this.digitalAddressResponseSecondCycle.put(key,value);
    }

    public void addDigitalSecondCycle(String key, LegalDigitalAddressInt value) {
        this.digitalAddressResponseSecondCycle.put(key,value);
    }
    
    @Override
    public void sendRequestForGetDigitalAddress(String taxId, String recipientType, String correlationId) {
        ThreadPool.start( new Thread(() -> {
            // Viene atteso fino a che l'elemento di timeline relativo all'invio verso extChannel sia stato inserito
            //timelineEventId = <CATEGORY_VALUE>;IUN_<IUN_VALUE>;RECINDEX_<RECINDEX_VALUE>
            String iunFromElementId = correlationId.split("\\" + TimelineEventIdBuilder.DELIMITER)[1];
            String iun = iunFromElementId.replace("IUN_", "");
            await().atLeast(Duration.ofSeconds(1)).untilAsserted(() ->
                    Assertions.assertTrue(timelineService.getTimelineElement(iun, correlationId).isPresent())
            );

            Assertions.assertDoesNotThrow(() -> {
                simulateDigitalAddressResponse(taxId, correlationId);
            });
        }));
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
        LegalDigitalAddressInt address;
        System.out.println("correlationId "+ correlationId);
        int getNationalRegistriesCalledTimes = Integer.parseInt(correlationId.substring(correlationId.lastIndexOf("ATTEMPT_") + 8)) ;
        
        if(getNationalRegistriesCalledTimes == 0){
            address = this.digitalAddressResponse.get(taxId);
        } else {
            address = this.digitalAddressResponseSecondCycle.get(taxId);
        }
        
        NationalRegistriesResponse response = NationalRegistriesResponse.builder()
                .correlationId(correlationId)
                .digitalAddress(address)
                .build();
        nationalRegistriesResponseHandler.handleResponse(response);
    }

}
