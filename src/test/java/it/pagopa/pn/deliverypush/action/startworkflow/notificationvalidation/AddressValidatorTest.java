package it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation;

import it.pagopa.pn.addressmanager.generated.openapi.clients.addressmanager.model.AcceptedResponse;
import it.pagopa.pn.deliverypush.action.it.utils.TestUtils;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.addressmanager.NormalizeItemsResultInt;
import it.pagopa.pn.deliverypush.dto.ext.addressmanager.NormalizeResultInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.exceptions.PnValidationNotValidAddressException;
import it.pagopa.pn.deliverypush.service.AddressManagerService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

class AddressValidatorTest {

    @Mock
    private AddressManagerService addressManagerService;
    @Mock
    private TimelineUtils timelineUtils;
    @Mock
    private TimelineService timelineService;

    private AddressValidator addressValidator;

    @BeforeEach
    public void setup() {
        addressValidator = new AddressValidator(addressManagerService, timelineUtils, timelineService);
    }

    @ExtendWith(SpringExtension.class)
    @Test
    void requestValidateAndNormalizeAddresses() {
        //GIVEN
        NotificationInt notification = TestUtils.getNotification();

        Mockito.when(addressManagerService.normalizeAddresses(Mockito.eq(notification), Mockito.anyString()))
                .thenReturn(Mono.just(new AcceptedResponse().correlationId("corrId")));

        final TimelineElementInternal timelineElement = TimelineElementInternal.builder().build();
        Mockito.when(timelineUtils.buildValidateAndNormalizeAddressTimelineElement(Mockito.eq(notification), Mockito.anyString()))
                .thenReturn(timelineElement);
        
        //WHEN
        addressValidator.requestValidateAndNormalizeAddresses(notification).block();
        
        //THEN
        Mockito.verify(timelineService).addTimelineElement(timelineElement,notification);
        
    }
    @ExtendWith(SpringExtension.class)
    @Test
    void handleAddressValidationOk() {
        //GIVEN
        String iun = "testIun";

        List<NormalizeResultInt> listNormResult = getNormalizeResultIntList();

        NormalizeItemsResultInt normalizeItemsResult = NormalizeItemsResultInt.builder()
                .correlationId("testCorrId")
                .resultItems(listNormResult)
                .build();
                
        //WHEN
        Assertions.assertDoesNotThrow(() ->  addressValidator.handleAddressValidation(iun, normalizeItemsResult));
        
    }

    @ExtendWith(SpringExtension.class)
    @Test
    void handleAddressValidationKo() {
        //GIVEN
        String iun = "testIun";

        List<NormalizeResultInt> listNormResult = getNormalizeResultIntListError();

        NormalizeItemsResultInt normalizeItemsResult = NormalizeItemsResultInt.builder()
                .correlationId("testCorrId")
                .resultItems(listNormResult)
                .build();

        //WHEN
        Assertions.assertThrows(PnValidationNotValidAddressException.class, () ->  
                addressValidator.handleAddressValidation(iun, normalizeItemsResult));

    }
    
    @NotNull
    private static List<NormalizeResultInt> getNormalizeResultIntList() {
        List<NormalizeResultInt> listNormResult = new ArrayList<>();
        NormalizeResultInt result1 = NormalizeResultInt.builder()
                .normalizedAddress(PhysicalAddressInt.builder()
                        .addressDetails("001")
                        .foreignState("002")
                        .at("003")
                        .province("004")
                        .municipality("005")
                        .zip("006")
                        .municipalityDetails("007")
                        .build())
                .id("0")
                .build();
        listNormResult.add(result1);

        NormalizeResultInt result2 = NormalizeResultInt.builder()
                .normalizedAddress(PhysicalAddressInt.builder()
                        .addressDetails("002")
                        .foreignState("003")
                        .at("004")
                        .province("005")
                        .municipality("006")
                        .zip("007")
                        .municipalityDetails("008")
                        .build())
                .id("1")
                .build();
        listNormResult.add(result2);
        return listNormResult;
    }

    @NotNull
    private static List<NormalizeResultInt> getNormalizeResultIntListError() {
        List<NormalizeResultInt> listNormResult = new ArrayList<>();
        NormalizeResultInt result1 = NormalizeResultInt.builder()
                .normalizedAddress(PhysicalAddressInt.builder()
                        .addressDetails("001")
                        .foreignState("002")
                        .at("003")
                        .province("004")
                        .municipality("005")
                        .zip("006")
                        .municipalityDetails("007")
                        .build())
                .id("0")
                .build();
        listNormResult.add(result1);

        NormalizeResultInt result2 = NormalizeResultInt.builder()
                .normalizedAddress(PhysicalAddressInt.builder()
                        .addressDetails("002")
                        .foreignState("003")
                        .at("004")
                        .province("005")
                        .municipality("006")
                        .zip("007")
                        .municipalityDetails("008")
                        .build())
                .id("1")
                .error("Address is not valid")
                .build();
        listNormResult.add(result2);
        return listNormResult;
    }
}