package it.pagopa.pn.deliverypush.actions2;


import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.commons_delivery.middleware.NotificationDao;
import org.springframework.cloud.stream.annotation.StreamListener;

import java.util.Optional;

public class StartWorkflow {
    private LegalFactGenerator legalFactGenerator;
    private NotificationDao notificationDao;
    private CourtesyMessageHandler courtesyMessageHandler;
    private ChooseDeliveryMode chooseDeliveryType;

    /**
     * Start new Notification Workflow. For all notification recipient send courtesy message and start choose delivery type
     *
     * @param iun Notification unique identifier
     */
    @StreamListener(condition = "NEW_NOTIFICATION")
    public void startWorkflow(String iun) {

        Optional<Notification> optNotification = notificationDao.getNotificationByIun(iun);

        if (optNotification.isPresent()) {
            Notification notification = optNotification.get();
            legalFactGenerator.sendeAckLegaclFact(notification);

            for (NotificationRecipient recipient : notification.getRecipients()) {
                courtesyMessageHandler.sendCourtesyMessage(iun, recipient.getTaxId());
                chooseDeliveryType.chooseDeliveryTypeAndStartWorkflow(notification, recipient);
            }
        } else {
            //TODO Gestire casistica di errore
        }
    }

}
