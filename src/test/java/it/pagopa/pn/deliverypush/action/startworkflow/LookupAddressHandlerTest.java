package it.pagopa.pn.deliverypush.action.startworkflow;

import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.datavault.RecipientTypeInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.publicregistry.NationalRegistriesResponse;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.exceptions.PnLookupAddressValidationFailedException;
import it.pagopa.pn.deliverypush.service.ConfidentialInformationService;
import it.pagopa.pn.deliverypush.service.NationalRegistriesService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Mock
    private NationalRegistriesService nationalRegistriesService;

    private LookupAddressHandler lookupAddressHandler;

    @BeforeEach
    void setUp() {
        timelineService = mock(TimelineService.class);
        timelineUtils = mock(TimelineUtils.class);
        confidentialInformationService = mock(ConfidentialInformationService.class);
        nationalRegistriesService = mock(NationalRegistriesService.class);
        lookupAddressHandler = new LookupAddressHandler(timelineService, timelineUtils, confidentialInformationService, nationalRegistriesService);
    }

    @Test
    void performValidation_success() {
        NotificationInt notification = getNotification();
        NationalRegistriesResponse response = mock(NationalRegistriesResponse.class);
        when(response.getPhysicalAddress()).thenReturn(getPhysicalAddress());
        when(response.getError()).thenReturn(null);
        when(response.getRecIndex()).thenReturn(0);
        when(nationalRegistriesService.getMultiplePhysicalAddress(notification)).thenReturn(List.of(response));

        TimelineElementInternal timelineElement = mock(TimelineElementInternal.class);
        when(timelineUtils.buildNationalRegistryValidationResponse(eq(notification), eq(response))).thenReturn(timelineElement);

        lookupAddressHandler.performValidation(notification);

        verify(nationalRegistriesService, times(1)).getMultiplePhysicalAddress(notification);
        verify(timelineService, times(1)).addTimelineElement(eq(timelineElement), eq(notification));
    }

    @Test
    void performValidation_addressNotFound() {
        // Case ADDRESS_NOT_FOUND
        NotificationInt notification = mock(NotificationInt.class);
        NationalRegistriesResponse response = mock(NationalRegistriesResponse.class);
        when(response.getPhysicalAddress()).thenReturn(null);
        when(response.getError()).thenReturn(null);
        when(nationalRegistriesService.getMultiplePhysicalAddress(notification)).thenReturn(List.of(response));

        assertThrows(PnLookupAddressValidationFailedException.class, () -> lookupAddressHandler.performValidation(notification));
    }

    @Test
    void performValidation_addressSearchFailed() {
        // Case ADDRESS_SEARCH_FAILED
        NotificationInt notification = mock(NotificationInt.class);
        NationalRegistriesResponse response = mock(NationalRegistriesResponse.class);
        when(response.getPhysicalAddress()).thenReturn(getPhysicalAddress());
        when(response.getError()).thenReturn("Error");
        when(nationalRegistriesService.getMultiplePhysicalAddress(notification)).thenReturn(List.of(response));

        assertThrows(PnLookupAddressValidationFailedException.class, () -> lookupAddressHandler.performValidation(notification));
    }

    private static NotificationInt getNotification() {
        List<NotificationRecipientInt> recipients = List.of(
                NotificationRecipientInt.builder()
                        .recipientType(RecipientTypeInt.PF)
                        .taxId("taxId")
                        .physicalAddress(getPhysicalAddress())
                        .build()
        );
        return NotificationInt.builder()
                .iun("testIun")
                .recipients(recipients)
                .build();
    }

    private static PhysicalAddressInt getPhysicalAddress() {
        return PhysicalAddressInt.builder()
                .addressDetails("addressDetails")
                .zip("zip")
                .municipality("municipality")
                .build();
    }

}
