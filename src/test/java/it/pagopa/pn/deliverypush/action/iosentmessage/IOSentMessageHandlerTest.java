package it.pagopa.pn.deliverypush.action.iosentmessage;

import it.pagopa.pn.deliverypush.action.utils.CourtesyMessageUtils;
import it.pagopa.pn.deliverypush.dto.address.CourtesyDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.time.Instant;

class IOSentMessageHandlerTest {


    @Mock
    private CourtesyMessageUtils courtesyMessageUtils;

    @Mock
    private NotificationService notificationService;

    private IOSentMessageHandler handler;

    @BeforeEach
    public void setup() {

        notificationService = Mockito.mock(NotificationService.class);
        courtesyMessageUtils = Mockito.mock(CourtesyMessageUtils.class);

        handler = new IOSentMessageHandler(notificationService, courtesyMessageUtils);

    }


    @Test
    void handleIOSentMessage() {
        // GIVEN
        Instant instant = Instant.parse("2021-09-16T15:24:00.00Z");

        NotificationInt notificationInt = NotificationInt.builder().iun("001").build();
        CourtesyDigitalAddressInt courtesyAddress = CourtesyDigitalAddressInt.builder()
                .type(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.APPIO)
                .build();

        Mockito.when(notificationService.getNotificationByIun("001")).thenReturn(notificationInt);
        Mockito.doNothing().when(courtesyMessageUtils).addSendCourtesyMessageToTimeline(notificationInt, 2, courtesyAddress, instant);

        // WHEN
        handler.handleIOSentMessage("001", 2, instant);

        // THEN
        Mockito.verify(courtesyMessageUtils, Mockito.times(1)).addSendCourtesyMessageToTimeline(notificationInt, 2, courtesyAddress, instant);
    }


}