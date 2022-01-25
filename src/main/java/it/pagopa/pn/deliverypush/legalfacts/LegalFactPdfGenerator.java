package it.pagopa.pn.deliverypush.legalfacts;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.timeline.NotificationPathChooseDetails;
import it.pagopa.pn.api.dto.notification.timeline.SendDigitalFeedback;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;

import java.time.Instant;
import java.util.List;

public interface LegalFactPdfGenerator {
    byte[] generateNotificationReceivedLegalFact(Action action, Notification notification);

    byte[] generateNotificationReceivedLegalFact(Notification notification);

    byte[] generateNotificationViewedLegalFact(Action action, Notification notification);
    
    byte[] generateNotificationViewedLegalFact(String iun, NotificationRecipient recipient, Instant timeStamp);

    byte[] generatePecDeliveryWorkflowLegalFact(List<Action> actions, Notification notification, NotificationPathChooseDetails addresses);

    byte[] generatePecDeliveryWorkflowLegalFact(List<SendDigitalFeedback> listFeedbackFromExtChannel, Notification notification, NotificationRecipient recipient);

}
