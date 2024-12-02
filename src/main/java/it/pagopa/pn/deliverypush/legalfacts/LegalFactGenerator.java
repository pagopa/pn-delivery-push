package it.pagopa.pn.deliverypush.legalfacts;

import it.pagopa.pn.deliverypush.action.utils.EndWorkflowStatus;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.AARInfo;
import it.pagopa.pn.deliverypush.dto.mandate.DelegateInfoInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendDigitalFeedbackDetailsInt;
import it.pagopa.pn.deliverypush.legalfacts.generatorfactory.LegalFactGeneratorFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

@Component
@Slf4j
public class LegalFactGenerator {

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

    private final LegalFactGeneratorFactory legalFactGeneratorFactory;

    public LegalFactGenerator(LegalFactGeneratorFactory legalFactGeneratorFactory) {
        this.legalFactGeneratorFactory = legalFactGeneratorFactory;
    }

    public byte[] generateNotificationReceivedLegalFact(NotificationInt notification) throws IOException {
        return legalFactGeneratorFactory.generateNotificationReceivedLegalFact(notification);
    }

    public byte[] generateNotificationCancelledLegalFact(NotificationInt notification, Instant notificationCancellationRequestDate) throws IOException {
        return legalFactGeneratorFactory.generateNotificationCancelledLegalFact(notification, notificationCancellationRequestDate);
    }

    public byte[] generateNotificationViewedLegalFact(String iun,
                                                      NotificationRecipientInt recipient,
                                                      DelegateInfoInt delegateInfo,
                                                      Instant timeStamp,
                                                      NotificationInt notification) throws IOException {
        return legalFactGeneratorFactory.generateNotificationViewedLegalFact(iun, recipient, delegateInfo, timeStamp, notification);
    }

    public byte[] generatePecDeliveryWorkflowLegalFact(List<SendDigitalFeedbackDetailsInt> feedbackFromExtChannelList,
                                                       NotificationInt notification,
                                                       NotificationRecipientInt recipient,
                                                       EndWorkflowStatus status,
                                                       Instant completionWorkflowDate) throws IOException {
        return legalFactGeneratorFactory.generatePecDeliveryWorkflowLegalFact(feedbackFromExtChannelList, notification,
                recipient, status, completionWorkflowDate);
    }

    public byte[] generateAnalogDeliveryFailureWorkflowLegalFact(NotificationInt notification,
                                                                 NotificationRecipientInt recipient,
                                                                 EndWorkflowStatus status,
                                                                 Instant failureWorkflowDate) throws IOException {
        return legalFactGeneratorFactory.generateAnalogDeliveryFailureWorkflowLegalFact(notification, recipient, status, failureWorkflowDate);
    }

    public AARInfo generateNotificationAAR(NotificationInt notification, NotificationRecipientInt recipient, String quickAccessToken) throws IOException {
        return legalFactGeneratorFactory.generateNotificationAAR(notification, recipient, quickAccessToken);
    }

    public String generateNotificationAARBody(NotificationInt notification, NotificationRecipientInt recipient, String quickAccessToken) {
        return legalFactGeneratorFactory.generateNotificationAARBody(notification, recipient, quickAccessToken);
    }

    public String generateNotificationAARPECBody(NotificationInt notification, NotificationRecipientInt recipient, String quickAccessToken) {
        return legalFactGeneratorFactory.generateNotificationAARPECBody(notification, recipient, quickAccessToken);
    }

    public String generateNotificationAARSubject(NotificationInt notification) {
        return legalFactGeneratorFactory.generateNotificationAARSubject(notification);
    }

    public String generateNotificationAARForSMS(NotificationInt notification) {
        return legalFactGeneratorFactory.generateNotificationAARForSMS(notification);
    }
}

