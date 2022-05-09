package it.pagopa.pn.deliverypush.legalfacts;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.Notification;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipient;
import NotificationPathChooseDetails;
import SendDigitalFeedback;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;

import java.time.Instant;
import java.util.List;

public interface LegalFactPdfGenerator {
    @Deprecated
    byte[] generateNotificationReceivedLegalFact(Action action, Notification notification);

    byte[] generateNotificationReceivedLegalFact(Notification notification);
    
    @Deprecated
    byte[] generateNotificationViewedLegalFact(Action action, Notification notification);
    
    byte[] generateNotificationViewedLegalFact(String iun, NotificationRecipient recipient, Instant timeStamp);

    @Deprecated
    byte[] generatePecDeliveryWorkflowLegalFact(List<Action> actions, Notification notification, NotificationPathChooseDetails addresses);

    byte[] generatePecDeliveryWorkflowLegalFact(List<SendDigitalFeedback> listFeedbackFromExtChannel, Notification notification, NotificationRecipient recipient);

}
