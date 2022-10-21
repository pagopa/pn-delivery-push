package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.deliverypush.action.utils.EndWorkflowStatus;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.PdfInfo;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendDigitalFeedbackDetailsInt;

import java.time.Instant;
import java.util.List;

public interface SaveLegalFactsService {
    PdfInfo saveAAR(NotificationInt notification, NotificationRecipientInt recipient);

    String saveNotificationReceivedLegalFact(NotificationInt notification);

    String savePecDeliveryWorkflowLegalFact(
            List<SendDigitalFeedbackDetailsInt> listFeedbackFromExtChannel,
            NotificationInt notification,
            NotificationRecipientInt recipient,
            EndWorkflowStatus status,
            Instant completionWorkflowDate
    );
    
    String saveNotificationViewedLegalFact(
            NotificationInt notification,
            NotificationRecipientInt recipient,
            Instant timeStamp
    );    
}
