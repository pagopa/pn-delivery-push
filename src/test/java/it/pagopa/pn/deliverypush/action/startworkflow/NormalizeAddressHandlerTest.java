package it.pagopa.pn.deliverypush.action.startworkflow;

import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.datavault.NotificationRecipientAddressesDtoInt;
import it.pagopa.pn.deliverypush.dto.ext.datavault.RecipientTypeInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationPaymentInfoInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.addressmanager.model.AcceptedResponse;
import it.pagopa.pn.deliverypush.action.it.utils.TestUtils;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.addressmanager.NormalizeItemsResultInt;
import it.pagopa.pn.deliverypush.dto.ext.addressmanager.NormalizeResultInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.exceptions.PnValidationNotValidAddressException;
import it.pagopa.pn.deliverypush.service.AddressManagerService;
import it.pagopa.pn.deliverypush.service.ConfidentialInformationService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opensaml.xmlsec.signature.P;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;


class NormalizeAddressHandlerTest {

    @Mock
    private TimelineService timelineService;
    @Mock
    private NotificationUtils notificationUtils;
    @Mock
    private TimelineUtils timelineUtils;
    @Mock
    private ConfidentialInformationService confidentialInformationService;

    private NormalizeItemsResultInt normalizeItemsResult;
    private NormalizeAddressHandler normalizeAddressHandler;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        normalizeAddressHandler = new NormalizeAddressHandler(timelineService, notificationUtils, timelineUtils,confidentialInformationService);
    }

    @Test
    void testHandleNormalizedAddressResponse() {

        //GIVEN
        NormalizeItemsResultInt normalizeItemsResult = NormalizeItemsResultInt.builder()
                .correlationId("testCorrId")
                .resultItems(getNormalizeResultIntUnsortedList())
                .build();

        NotificationInt notification = TestUtils.getNotificationMultiRecipient();
        NotificationRecipientInt notificationRecipientInt =
                new NotificationRecipientInt("","","",new LegalDigitalAddressInt(),
                        new PhysicalAddressInt("","","","","","","","",""),
                        Arrays.asList(new NotificationPaymentInfoInt()), RecipientTypeInt.PF);
        Mockito.when(notificationUtils.getRecipientFromIndex(Mockito.any(),Mockito.anyInt())).thenReturn(notificationRecipientInt);

        Mockito.when(confidentialInformationService.updateNotificationAddresses(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.empty());

        normalizeAddressHandler.handleNormalizedAddressResponse(notification, normalizeItemsResult);

        //WHEN
        ArgumentCaptor<List<NotificationRecipientAddressesDtoInt>> captor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(confidentialInformationService).updateNotificationAddresses(Mockito.any(),Mockito.any(), captor.capture());
        List<NotificationRecipientAddressesDtoInt> capturedList = captor.getValue();


        //THEN
        PhysicalAddressInt capturedPhysicalAddressAtPosition0 = capturedList.get(0).getPhysicalAddress();
        PhysicalAddressInt originPhysicalAddressAtPosition1 = getNormalizeResultIntUnsortedList().get(1).getNormalizedAddress();
        assertEquals(capturedPhysicalAddressAtPosition0.getAddressDetails(),originPhysicalAddressAtPosition1.getAddressDetails());
        assertEquals(capturedPhysicalAddressAtPosition0.getZip(),originPhysicalAddressAtPosition1.getZip());
        assertEquals(capturedPhysicalAddressAtPosition0.getMunicipality(),originPhysicalAddressAtPosition1.getMunicipality());
        assertEquals(capturedPhysicalAddressAtPosition0.getMunicipalityDetails(),originPhysicalAddressAtPosition1.getMunicipalityDetails());
        assertEquals(capturedPhysicalAddressAtPosition0.getProvince(),originPhysicalAddressAtPosition1.getProvince());

        PhysicalAddressInt capturedPhysicalAddressAtPosition1 = capturedList.get(1).getPhysicalAddress();
        PhysicalAddressInt originPhysicalAddressAtPosition0 = getNormalizeResultIntUnsortedList().get(0).getNormalizedAddress();
        assertEquals(capturedPhysicalAddressAtPosition1.getAddressDetails(),originPhysicalAddressAtPosition0.getAddressDetails());
        assertEquals(capturedPhysicalAddressAtPosition1.getZip(),originPhysicalAddressAtPosition0.getZip());
        assertEquals(capturedPhysicalAddressAtPosition1.getMunicipality(),originPhysicalAddressAtPosition0.getMunicipality());
        assertEquals(capturedPhysicalAddressAtPosition1.getMunicipalityDetails(),originPhysicalAddressAtPosition0.getMunicipalityDetails());
        assertEquals(capturedPhysicalAddressAtPosition1.getProvince(),originPhysicalAddressAtPosition0.getProvince());
    }



    @NotNull
    private static List<NormalizeResultInt> getNormalizeResultIntUnsortedList() {
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
                .id("1")
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
                .id("0")
                .build();
        listNormResult.add(result2);
        return listNormResult;
    }
}
