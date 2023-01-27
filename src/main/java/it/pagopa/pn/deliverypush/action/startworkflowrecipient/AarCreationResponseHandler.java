package it.pagopa.pn.deliverypush.action.startworkflowrecipient;

import it.pagopa.pn.deliverypush.action.utils.AarUtils;
import it.pagopa.pn.deliverypush.action.utils.CourtesyMessageUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.PdfInfo;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@AllArgsConstructor
@Slf4j
public class AarCreationResponseHandler {
    private AarUtils aarUtils;
    private NotificationService notificationService;
    private final CourtesyMessageUtils courtesyMessageUtils;
    private final SchedulerService schedulerService;

    public void handleAarCreationResponse(String iun, int recIndex, String legalFactId) {
        log.info("Start handleReceivedLegalFactCreationResponse recipientWorkflow process - iun={} legalFactId={}", iun, legalFactId);
        NotificationInt notification = notificationService.getNotificationByIun(iun);

        //TODO Capire se ha ancora senso passare il numero di pagine del pdf, se cosi fosse andrà inserito e recuperato dalla timeline
        PdfInfo pdfInfo = PdfInfo.builder().key(legalFactId).build();
        aarUtils.addAarGenerationToTimeline(notification, recIndex, pdfInfo);

        //... Invio messaggio di cortesia ... 
        courtesyMessageUtils.checkAddressesAndSendCourtesyMessage(notification, recIndex);

        //... e viene schedulato il processo di scelta della tipologia di notificazione
        scheduleChooseDeliveryMode(iun, recIndex);
    }

    private void scheduleChooseDeliveryMode(String iun, Integer recIndex) {
        Instant schedulingDate = Instant.now();
        log.info("Scheduling choose delivery mode schedulingDate={} - iun={} id={}", schedulingDate, iun, recIndex);
        schedulerService.scheduleEvent(iun, recIndex, schedulingDate, ActionType.CHOOSE_DELIVERY_MODE);
    }

}
