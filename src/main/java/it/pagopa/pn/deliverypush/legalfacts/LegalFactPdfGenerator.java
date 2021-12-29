package it.pagopa.pn.deliverypush.legalfacts;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.timeline.NotificationPathChooseDetails;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;

import java.util.List;

public interface LegalFactPdfGenerator {
    byte[] generateNotificationReceivedLegalFact(Notification notification);

    byte[] generateNotificationViewedLegalFact(Action action, Notification notification);

    byte[] generatePecDeliveryWorkflowLegalFact(List<Action> actions, Notification notification, NotificationPathChooseDetails addresses);
}
