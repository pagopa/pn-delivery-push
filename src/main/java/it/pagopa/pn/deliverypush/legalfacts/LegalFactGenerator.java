package it.pagopa.pn.deliverypush.legalfacts;

import it.pagopa.pn.deliverypush.action.utils.EndWorkflowStatus;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.AARInfo;
import it.pagopa.pn.deliverypush.dto.mandate.DelegateInfoInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendDigitalFeedbackDetailsInt;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

public interface LegalFactGenerator {

    String FIELD_SEND_DATE = "sendDate";
    String FIELD_SEND_DATE_NO_TIME = "sendDateNoTime";
    String FIELD_NOTIFICATION = "notification";
    String FIELD_DIGESTS = "digests";
    String FIELD_ADDRESS_WRITER = "addressWriter";
    String FIELD_SIGNATURE = "signature";
    String FIELD_TIME_REFERENCE = "timeReference";
    String FIELD_PDF_FILE_NAME = "pdfFileName";
    String FIELD_IUN = "iun";
    String FIELD_DELIVERIES = "deliveries";
    String FIELD_RECIPIENT = "recipient";
    String FIELD_WHEN = "when";

    byte[] generateNotificationReceivedLegalFact(NotificationInt notification) throws IOException;

    byte[] generateNotificationCancelledLegalFact(NotificationInt notification, Instant notificationCancellationRequestDate) throws IOException;

    byte[] generateNotificationViewedLegalFact(String iun, NotificationRecipientInt recipient,
                                                      DelegateInfoInt delegateInfo,
                                                      Instant timeStamp,
                                                      NotificationInt notification) throws IOException;

    byte[] generatePecDeliveryWorkflowLegalFact(List<SendDigitalFeedbackDetailsInt> feedbackFromExtChannelList,
                                                       NotificationInt notification,
                                                       NotificationRecipientInt recipient,
                                                       EndWorkflowStatus status,
                                                       Instant completionWorkflowDate) throws IOException;

    byte[] generateAnalogDeliveryFailureWorkflowLegalFact(NotificationInt notification,
                                                                 NotificationRecipientInt recipient,
                                                                 EndWorkflowStatus status,
                                                                 Instant failureWorkflowDate) throws IOException;

    AARInfo generateNotificationAAR(NotificationInt notification, NotificationRecipientInt recipient, String quickAccessToken) throws IOException;

    String generateNotificationAARBody(NotificationInt notification, NotificationRecipientInt recipient, String quickAccessToken);

    String generateNotificationAARPECBody(NotificationInt notification, NotificationRecipientInt recipient, String quickAccessToken);

    String generateNotificationAARSubject(NotificationInt notification);

    String generateNotificationAARForSMS(NotificationInt notification);
}
