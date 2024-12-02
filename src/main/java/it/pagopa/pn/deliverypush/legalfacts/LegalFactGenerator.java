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

    public static final String FIELD_SEND_DATE = "sendDate";
    public static final String FIELD_SEND_DATE_NO_TIME = "sendDateNoTime";
    public static final String FIELD_NOTIFICATION = "notification";
    public static final String FIELD_DIGESTS = "digests";
    public static final String FIELD_ADDRESS_WRITER = "addressWriter";
    public static final String FIELD_SIGNATURE = "signature";
    public static final String FIELD_TIME_REFERENCE = "timeReference";
    public static final String FIELD_PDF_FILE_NAME = "pdfFileName";
    public static final String FIELD_IUN = "iun";
    public static final String FIELD_DELIVERIES = "deliveries";
    public static final String FIELD_RECIPIENT = "recipient";
    public static final String FIELD_WHEN = "when";

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
