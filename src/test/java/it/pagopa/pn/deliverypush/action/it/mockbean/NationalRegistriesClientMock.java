package it.pagopa.pn.deliverypush.action.it.mockbean;

import it.pagopa.pn.deliverypush.action.it.utils.MethodExecutor;
import it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation.NotificationValidationActionHandler;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.publicregistry.NationalRegistriesResponse;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventIdBuilder;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.nationalregistries.model.CheckTaxIdOK;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.nationalregistries.model.PhysicalAddressesRequestBody;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.nationalregistries.model.RecipientAddressRequestBody;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.nationalregistries.NationalRegistriesClient;
import it.pagopa.pn.deliverypush.middleware.responsehandler.NationalRegistriesResponseHandler;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.utils.ThreadPool;
import org.junit.jupiter.api.Assertions;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


public class NationalRegistriesClientMock implements NationalRegistriesClient {

    private int getNationalRegistriesCalledTimes = 0;

    public static final String NOT_VALID = "NOT_VALID";
    public static final String EXCEPTION = "EXCEPTION";
    public static final String NOT_FOUND = "NOT_FOUND";
    private final NationalRegistriesResponseHandler nationalRegistriesResponseHandler;
    private final NotificationValidationActionHandler notificationValidationActionHandler;
    private ConcurrentMap<String, LegalDigitalAddressInt> digitalAddressResponse;
    private ConcurrentMap<String, LegalDigitalAddressInt> digitalAddressResponseSecondCycle;
    private final TimelineService timelineService;


    public NationalRegistriesClientMock(
            NationalRegistriesResponseHandler nationalRegistriesResponseHandler,
            NotificationValidationActionHandler notificationValidationActionHandler,
            TimelineService timelineService
    ) {
        this.nationalRegistriesResponseHandler = nationalRegistriesResponseHandler;
        this.notificationValidationActionHandler = notificationValidationActionHandler;
        this.timelineService = timelineService;
    }

    public void clear() {
        this.digitalAddressResponse = new ConcurrentHashMap<>();
        this.digitalAddressResponseSecondCycle = new ConcurrentHashMap<>();
        this.getNationalRegistriesCalledTimes = 0;
    }

    public void addDigital(String key, LegalDigitalAddressInt value) {
        this.digitalAddressResponse.put(key,value);
        this.digitalAddressResponseSecondCycle.put(key,value);
    }

    public void addDigitalSecondCycle(String key, LegalDigitalAddressInt value) {
        this.digitalAddressResponseSecondCycle.put(key,value);
    }
    
    @Override
    public void sendRequestForGetDigitalAddress(String taxId, String recipientType, String correlationId, Instant notificationSentAt) {
        ThreadPool.start( new Thread(() -> {
            // Viene atteso fino a che l'elemento di timeline relativo all'invio verso extChannel sia stato inserito
            //timelineEventId = <CATEGORY_VALUE>;IUN_<IUN_VALUE>;RECINDEX_<RECINDEX_VALUE>
            String iunFromElementId = correlationId.split("\\" + TimelineEventIdBuilder.DELIMITER)[1];
            String iun = iunFromElementId.replace("IUN_", "");

            MethodExecutor.waitForExecution(
                    () -> timelineService.getTimelineElement(iun, correlationId)
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

        if(getNationalRegistriesCalledTimes == 0){
            address = this.digitalAddressResponse.get(taxId);
        } else {
            address = this.digitalAddressResponseSecondCycle.get(taxId);
        }

        getNationalRegistriesCalledTimes += 1;
        
        NationalRegistriesResponse response = NationalRegistriesResponse.builder()
                .correlationId(correlationId)
                .digitalAddress(address)
                .build();
        nationalRegistriesResponseHandler.handleResponse(response);
    }

    @Override
    public void sendRequestForGetPhysicalAddresses(PhysicalAddressesRequestBody physicalAddressesRequestBody) {
        String correlationId = physicalAddressesRequestBody.getCorrelationId();
        ThreadPool.start( new Thread(() -> {
            // Viene atteso fino a che l'elemento di timeline relativo all'invio verso nationalRegistries sia stato inserito
            //timelineEventId = <CATEGORY_VALUE>;IUN_<IUN_VALUE>;RECINDEX_<RECINDEX_VALUE>
            String iunFromElementId = correlationId.split("\\" + TimelineEventIdBuilder.DELIMITER)[1];
            String iun = iunFromElementId.replace("IUN_", "");

            MethodExecutor.waitForExecution(
                    () -> timelineService.getTimelineElement(iun, correlationId)
            );

            List<NationalRegistriesResponse> responses = physicalAddressesRequestBody.getAddresses().stream()
                    .map(RecipientAddressRequestBody::getFilter)
                    .map(filter -> {
                        String taxId = filter.getTaxId();
                        return NationalRegistriesResponse.builder()
                                .correlationId(correlationId)
                                .registry("ANPR")
                                .recIndex(Integer.valueOf(filter.getRecIndex()))
                                .physicalAddress(taxId.contains(NOT_FOUND) ? null : defaultPhysicalAddress())
                                .addressResolutionStart(Instant.now().minus(Duration.ofMinutes(2)))
                                .addressResolutionEnd(Instant.now())
                                .build();
                    }).toList();

            notificationValidationActionHandler.handleValidateNationalRegistriesResponse(physicalAddressesRequestBody.getCorrelationId(), responses);

        }));
    }

    private PhysicalAddressInt defaultPhysicalAddress() {
        return PhysicalAddressInt.builder()
                .address("Test address")
                .at("At")
                .zip("00133")
                .municipality("Test municipality")
                .province("TS")
                .build();
    }
}
