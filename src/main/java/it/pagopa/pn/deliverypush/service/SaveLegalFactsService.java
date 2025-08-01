package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.deliverypush.action.utils.EndWorkflowStatus;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.PdfInfo;
import it.pagopa.pn.deliverypush.dto.mandate.DelegateInfoInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendDigitalFeedbackDetailsInt;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

public interface SaveLegalFactsService {
    PdfInfo sendCreationRequestForAAR(NotificationInt notification, NotificationRecipientInt recipient, String quickAccessToken);

    String sendCreationRequestForNotificationReceivedLegalFact(NotificationInt notification);

    String sendCreationRequestForPecDeliveryWorkflowLegalFact(
            List<SendDigitalFeedbackDetailsInt> listFeedbackFromExtChannel,
            NotificationInt notification,
            NotificationRecipientInt recipient,
            EndWorkflowStatus status,
            Instant completionWorkflowDate
    );

    String sendCreationRequestForAnalogDeliveryFailureWorkflowLegalFact(
            NotificationInt notification,
            NotificationRecipientInt recipient,
            EndWorkflowStatus status,
            Instant failureWorkflowDate
    );
    
    Mono<String> sendCreationRequestForNotificationViewedLegalFact(
            NotificationInt notification,
            NotificationRecipientInt recipient,
            DelegateInfoInt delegateInfo,
            Instant timeStamp
    );

    String sendCreationRequestForNotificationCancelledLegalFact(NotificationInt notification, Instant notificationCancellationRequestDate);

    Mono<String> sendCreationRequestForAnalogDeliveryWorkflowTimeoutLegalFact(
            NotificationInt notification,
            NotificationRecipientInt recipient,
            PhysicalAddressInt physicalAddress,
            String sentAttemptMade,
            Instant timeoutDate);

}
