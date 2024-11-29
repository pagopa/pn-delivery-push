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

    private final LegalFactGeneratorTemplatesClient templatesClient;
    private final LegalFactGeneratorDocComposition legalFactGeneratorDocComposition;

    public LegalFactGenerator(LegalFactGeneratorFactory factory) {
        this.templatesClient = factory.createTemplatesClient();
        this.legalFactGeneratorDocComposition = factory.createLegalFactGeneratorDocComposition();
    }

    public byte[] generateNotificationReceivedLegalFact(NotificationInt notification) throws IOException {
        return templatesClient.generateNotificationReceivedLegalFact(notification);
    }

    public byte[] generateNotificationCancelledLegalFact(NotificationInt notification, Instant notificationCancellationRequestDate) throws IOException {
        return templatesClient.generateNotificationCancelledLegalFact(notification, notificationCancellationRequestDate);
    }

    public byte[] generateNotificationViewedLegalFact(String iun, NotificationRecipientInt recipient,
                                                      DelegateInfoInt delegateInfo, Instant timeStamp,
                                                      NotificationInt notification) throws IOException {
        return templatesClient.generateNotificationViewedLegalFact(iun, recipient, delegateInfo, timeStamp, notification);
    }

    public byte[] generatePecDeliveryWorkflowLegalFact(List<SendDigitalFeedbackDetailsInt> feedbackFromExtChannelList,
                                                       NotificationInt notification,
                                                       NotificationRecipientInt recipient,
                                                       EndWorkflowStatus status,
                                                       Instant completionWorkflowDate) throws IOException {
        return templatesClient.generatePecDeliveryWorkflowLegalFact(feedbackFromExtChannelList, notification, recipient,
                status, completionWorkflowDate);
    }


    public byte[] generateAnalogDeliveryFailureWorkflowLegalFact(NotificationInt notification,
                                                                 NotificationRecipientInt recipient,
                                                                 EndWorkflowStatus status,
                                                                 Instant failureWorkflowDate) throws IOException {
        return templatesClient.generateAnalogDeliveryFailureWorkflowLegalFact(notification, recipient, failureWorkflowDate);
    }

    public AARInfo generateNotificationAAR(NotificationInt notification, NotificationRecipientInt recipient, String quickAccessToken) throws IOException {
        return templatesClient.generateNotificationAAR(notification, recipient, quickAccessToken);
    }

    public String generateNotificationAARBody(NotificationInt notification, NotificationRecipientInt recipient, String quickAccesstoken) throws IOException {
        return templatesClient.generateNotificationAARBody(notification, recipient, quickAccesstoken);
    }

    public String generateNotificationAARPECBody(NotificationInt notification, NotificationRecipientInt recipient, String quickAccesstoken) {
        return templatesClient.generateNotificationAARPECBody(notification, recipient, quickAccesstoken);
    }

    public String generateNotificationAARSubject(NotificationInt notification) {
        return templatesClient.generateNotificationAARSubject(notification);
    }

    public String generateNotificationAARForSMS(NotificationInt notification) {
        return templatesClient.generateNotificationAARSubject(notification);
    }
}

