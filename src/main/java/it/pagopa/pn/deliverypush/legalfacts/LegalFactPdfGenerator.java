package it.pagopa.pn.deliverypush.legalfacts;

import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.SendDigitalFeedback;

import java.time.Instant;
import java.util.List;

public interface LegalFactPdfGenerator {
    @Deprecated
    byte[] generateNotificationReceivedLegalFact(Action action, NotificationInt notification);

    byte[] generateNotificationReceivedLegalFact(NotificationInt notification);
    
    @Deprecated
    byte[] generateNotificationViewedLegalFact(Action action, NotificationInt notification);
    
    byte[] generateNotificationViewedLegalFact(String iun, NotificationRecipientInt recipient, Instant timeStamp);

    /*@Deprecated
    byte[] generatePecDeliveryWorkflowLegalFact(List<Action> actions, NotificationInt notification, NotificationPathChooseDetails addresses);
*/
    byte[] generatePecDeliveryWorkflowLegalFact(List<SendDigitalFeedback> listFeedbackFromExtChannel, NotificationInt notification, NotificationRecipientInt recipient);

}
