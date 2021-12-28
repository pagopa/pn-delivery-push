package it.pagopa.pn.deliverypush.action2;


import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.deliverypush.service.CourtesyMessageService;
import it.pagopa.pn.deliverypush.service.LegalFactGeneratorService;
import it.pagopa.pn.deliverypush.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class StartWorkflowHandler {
    private final LegalFactGeneratorService legalFactGenerator;
    private final NotificationService notificationService;
    private final CourtesyMessageService courtesyMessageService;
    private final ChooseDeliveryModeHandler chooseDeliveryType;

    public StartWorkflowHandler(LegalFactGeneratorService legalFactGenerator, NotificationService notificationService, CourtesyMessageService courtesyMessageService,
                                ChooseDeliveryModeHandler chooseDeliveryType) {
        this.legalFactGenerator = legalFactGenerator;
        this.notificationService = notificationService;
        this.courtesyMessageService = courtesyMessageService;
        this.chooseDeliveryType = chooseDeliveryType;
    }

    /**
     * Start new Notification Workflow. For all notification recipient send courtesy message and start choose delivery type
     *
     * @param iun Notification unique identifier
     */
    @StreamListener(condition = "NEW_NOTIFICATION")
    public void startWorkflow(String iun) {
        log.info("Start notification process for IUN {}", iun);

        try {
            Notification notification = notificationService.getNotificationByIun(iun);
            legalFactGenerator.sendAckLegaclFact(notification);

            for (NotificationRecipient recipient : notification.getRecipients()) {
                //Per ogni recipient della notifica viene inviato il messaggio di cortesia ...
                courtesyMessageService.sendCourtesyMessage(notification, recipient);
                //... e viene inizializzato il processo di scelta della tipologia di notificazione
                chooseDeliveryType.chooseDeliveryTypeAndStartWorkflow(notification, recipient);
            }

        } catch (RuntimeException ex) {
            log.error("Exception ex {}", ex.getMessage());
            throw ex;
            //TODO Capire come gestire l'exception se rilanciare l'exception e far ritornare il message in coda, se avvisare dell'avvenuto errore ecc.
        }

    }

}
