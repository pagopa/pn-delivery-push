package it.pagopa.pn.deliverypush.action.iosentmessage;

import it.pagopa.pn.deliverypush.action.utils.CourtesyMessageUtils;
import it.pagopa.pn.deliverypush.dto.address.CourtesyDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
public class IOSentMessageHandler {

    private final NotificationService notificationService;
    private final CourtesyMessageUtils courtesyMessageUtils;

    public IOSentMessageHandler(NotificationService notificationService, CourtesyMessageUtils courtesyMessageUtils) {

        this.notificationService = notificationService;
        this.courtesyMessageUtils = courtesyMessageUtils;
    }

    public void handleIOSentMessage(String iun, int recIndex, Instant sentDate) {
        log.debug("Start handle io sent message - iun={} recIndex={} sentDate={}", iun, recIndex, sentDate);

        NotificationInt notification = notificationService.getNotificationByIun(iun);
        courtesyMessageUtils.addSendCourtesyMessageToTimeline(notification, recIndex, CourtesyDigitalAddressInt.builder()
                .type(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.APPIO).build(), sentDate);

        log.info("IO Sent Message timeline event added - iun={} paTaxId={} ", iun, recIndex);
    }
}