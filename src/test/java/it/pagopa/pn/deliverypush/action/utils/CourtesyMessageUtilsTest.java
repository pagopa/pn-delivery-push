package it.pagopa.pn.deliverypush.action.utils;

import it.pagopa.pn.deliverypush.action.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.PhysicalAddressBuilder;
import it.pagopa.pn.deliverypush.dto.address.CourtesyDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.service.AddressBookService;
import it.pagopa.pn.deliverypush.service.ExternalChannelService;
import it.pagopa.pn.deliverypush.service.IoService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.webjars.NotFoundException;

import java.util.Collections;
import java.util.Optional;

import static it.pagopa.pn.deliverypush.action.it.mockbean.ExternalChannelMock.EXTCHANNEL_SEND_SUCCESS;
import static org.mockito.Mockito.doThrow;

class CourtesyMessageUtilsTest {
    @Mock
    private AddressBookService addressBookService;
    @Mock
    private ExternalChannelService externalChannelService;
    @Mock
    private TimelineService timelineService;
    @Mock
    private TimelineUtils timelineUtils;
    @Mock
    private InstantNowSupplier instantNowSupplier;
    @Mock
    private NotificationUtils notificationUtils;
    @Mock
    private IoService iOservice;

    private CourtesyMessageUtils courtesyMessageUtils;

    @BeforeEach
    public void setup() {
        courtesyMessageUtils = new CourtesyMessageUtils(addressBookService, externalChannelService, 
                timelineService, timelineUtils, instantNowSupplier, notificationUtils, iOservice);
    }

    
    @Test
    @ExtendWith(MockitoExtension.class)
    void checkAddressesForSendCourtesyMessage() {
        //GIVEN
        NotificationRecipientInt recipient = getNotificationRecipientInt();
        NotificationInt notification = getNotificationInt(recipient);

        Mockito.when(notificationUtils.getRecipientFromIndex(Mockito.any(NotificationInt.class), Mockito.anyInt())).thenReturn(recipient);

        CourtesyDigitalAddressInt courtesyDigitalAddressInt = CourtesyDigitalAddressInt.builder()
                .type(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.APPIO)
                .address("indirizzo@test.it")
                .build();
        
        Mockito.when(addressBookService.getCourtesyAddress(Mockito.anyString(),Mockito.anyString()))
                .thenReturn(Optional.of(Collections.singletonList(courtesyDigitalAddressInt)));
        
        //WHEN
        courtesyMessageUtils.checkAddressesForSendCourtesyMessage(notification, 0);
        
        //THEN
        Mockito.verify(timelineService).addTimelineElement(Mockito.any(), Mockito.any(NotificationInt.class));
    }

    @Test
    @ExtendWith(MockitoExtension.class)
    void checkAddressesForSendCourtesyMessageCourtesyEmpty() {
        //GIVEN
        NotificationRecipientInt recipient = getNotificationRecipientInt();
        NotificationInt notification = getNotificationInt(recipient);

        Mockito.when(notificationUtils.getRecipientFromIndex(Mockito.any(NotificationInt.class), Mockito.anyInt())).thenReturn(recipient);

        Mockito.when(addressBookService.getCourtesyAddress(Mockito.anyString(),Mockito.anyString()))
                .thenReturn(Optional.empty());
        
        //WHEN
        courtesyMessageUtils.checkAddressesForSendCourtesyMessage(notification, 0);
        
        //THEN
        Mockito.verify(timelineService, Mockito.times(0)).addTimelineElement(Mockito.any(), Mockito.any(NotificationInt.class));
    }
    
    @Test
    @ExtendWith(MockitoExtension.class)
    void checkAddressesForSendCourtesySendMessageError() {
        //GIVEN
        NotificationRecipientInt recipient = getNotificationRecipientInt();
        NotificationInt notification = getNotificationInt(recipient);

        Mockito.when(notificationUtils.getRecipientFromIndex(Mockito.any(NotificationInt.class), Mockito.anyInt())).thenReturn(recipient);

        CourtesyDigitalAddressInt courtesyDigitalAddressInt = CourtesyDigitalAddressInt.builder()
                .type(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.APPIO)
                .address("indirizzo@test.it")
                .build();

        Mockito.when(addressBookService.getCourtesyAddress(Mockito.anyString(),Mockito.anyString()))
                .thenReturn(Optional.of(Collections.singletonList(courtesyDigitalAddressInt)));
        
        doThrow(new NotFoundException("Not found")).when(iOservice).sendIOMessage(Mockito.any(NotificationInt.class), Mockito.anyInt());

        //WHEN
        courtesyMessageUtils.checkAddressesForSendCourtesyMessage(notification, 0);

        //THEN
        Mockito.verify(timelineService, Mockito.times(0)).addTimelineElement(Mockito.any(), Mockito.any(NotificationInt.class));
    }
    
    private NotificationInt getNotificationInt(NotificationRecipientInt recipient) {
        return NotificationTestBuilder.builder()
                .withIun("iun_01")
                .withPaId("paId01")
                .withNotificationRecipient(recipient)
                .build();
    }

    private NotificationRecipientInt getNotificationRecipientInt() {
        String taxId = "TaxId";
        return NotificationRecipientTestBuilder.builder()
                .withTaxId(taxId)
                .withInternalId("ANON_" + taxId)
                .withDigitalDomicile(
                        LegalDigitalAddressInt.builder()
                                .address("address")
                                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                                .build()
                )
                .withPhysicalAddress(
                        PhysicalAddressBuilder.builder()
                                .withAddress(EXTCHANNEL_SEND_SUCCESS + "_Via Nuova")
                                .build()
                )
                .build();
    }

}