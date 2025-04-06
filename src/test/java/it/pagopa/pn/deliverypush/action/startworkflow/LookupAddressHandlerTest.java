package it.pagopa.pn.deliverypush.action.startworkflow;

import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.datavault.NotificationRecipientAddressesDtoInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.publicregistry.NationalRegistriesResponse;
import it.pagopa.pn.deliverypush.exceptions.PnLookupAddressNotFoundException;
import it.pagopa.pn.deliverypush.service.ConfidentialInformationService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

class LookupAddressHandlerTest {

    @Mock
    private TimelineService timelineService;

    @Mock
    private TimelineUtils timelineUtils;

    @Mock
    private ConfidentialInformationService confidentialInformationService;

    private LookupAddressHandler lookupAddressHandler;


    @BeforeEach
    void setUp() {
        timelineService = mock(TimelineService.class);
        timelineUtils = mock(TimelineUtils.class);
        confidentialInformationService = mock(ConfidentialInformationService.class);
        lookupAddressHandler = new LookupAddressHandler(timelineService, timelineUtils, confidentialInformationService);
    }
    @Test
    void testValidateAddresses_withValidAddresses() {
        lookupAddressHandler.validateAddresses(List.of(getNationalRegistriesResponse()));
    }

    @Test
    void testValidateAddresses_withInvalidAddresses() {
        NationalRegistriesResponse response = NationalRegistriesResponse.builder()
                .correlationId("correlationId")
                .physicalAddress(null)
                .build();

        List<NationalRegistriesResponse> responses = List.of(response);

        assertThrows(PnLookupAddressNotFoundException.class, () -> lookupAddressHandler.validateAddresses(responses));
    }

    @Test
    void testSaveAddresses_withValidData() {
        NotificationInt notification = mock(NotificationInt.class);
        NotificationRecipientInt recipient = mock(NotificationRecipientInt.class);
        List<NotificationRecipientAddressesDtoInt> recipientAddressesDtoList = new ArrayList<>();
        PhysicalAddressInt physicalAddressInt = getNationalRegistriesResponse().getPhysicalAddress();
        physicalAddressInt.setFullname("denomination");
        recipientAddressesDtoList.add(NotificationRecipientAddressesDtoInt.builder()
                .denomination("denomination")
                .digitalAddress(null)
                .physicalAddress(physicalAddressInt)
                .recIndex(0)
                .build());

        when(notification.getRecipients()).thenReturn(List.of(recipient));
        when(notification.getIun()).thenReturn("iun");
        when(notification.getRecipients()).thenReturn(List.of(recipient));
        when(recipient.getDenomination()).thenReturn("denomination");

        List<NationalRegistriesResponse> responses = List.of(getNationalRegistriesResponse());

        lookupAddressHandler.saveAddresses(responses, notification);

        Mockito.verify(timelineService, times(1)).addTimelineElement(any(), eq(notification));
        Mockito.verify(confidentialInformationService, times(1)).updateNotificationAddresses(notification.getIun(), false, recipientAddressesDtoList);
    }

    public static NationalRegistriesResponse getNationalRegistriesResponse() {
        PhysicalAddressInt physicalAddress = PhysicalAddressInt.builder()
                .addressDetails("addressDetails")
                .zip("zip")
                .municipality("municipality")
                .build();
        return NationalRegistriesResponse.builder()
                .correlationId("correlationId")
                .physicalAddress(physicalAddress)
                .recIndex(0)
                .registry("registry")
                .addressResolutionStart(Instant.now())
                .addressResolutionEnd(Instant.now())
                .build();
    }
}
