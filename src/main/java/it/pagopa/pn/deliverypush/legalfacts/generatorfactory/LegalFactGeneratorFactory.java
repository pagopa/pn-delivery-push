package it.pagopa.pn.deliverypush.legalfacts.generatorfactory;

import it.pagopa.pn.deliverypush.action.utils.EndWorkflowStatus;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.AARInfo;
import it.pagopa.pn.deliverypush.dto.mandate.DelegateInfoInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendDigitalFeedbackDetailsInt;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

public interface LegalFactGeneratorFactory {

    public byte[] generateNotificationReceivedLegalFact(NotificationInt notification) throws IOException;

    public byte[] generateNotificationCancelledLegalFact(NotificationInt notification, Instant notificationCancellationRequestDate) throws IOException;

    public byte[] generateNotificationViewedLegalFact(String iun, NotificationRecipientInt recipient,
                                                      DelegateInfoInt delegateInfo,
                                                      Instant timeStamp,
                                                      NotificationInt notification) throws IOException;

    public byte[] generatePecDeliveryWorkflowLegalFact(List<SendDigitalFeedbackDetailsInt> feedbackFromExtChannelList,
                                                       NotificationInt notification,
                                                       NotificationRecipientInt recipient,
                                                       EndWorkflowStatus status,
                                                       Instant completionWorkflowDate) throws IOException;


    public byte[] generateAnalogDeliveryFailureWorkflowLegalFact(NotificationInt notification,
                                                                 NotificationRecipientInt recipient,
                                                                 EndWorkflowStatus status,
                                                                 Instant failureWorkflowDate) throws IOException;

    public AARInfo generateNotificationAAR(NotificationInt notification, NotificationRecipientInt recipient, String quickAccessToken) throws IOException;

    public String generateNotificationAARBody(NotificationInt notification, NotificationRecipientInt recipient, String quickAccessToken);

    public String generateNotificationAARPECBody(NotificationInt notification, NotificationRecipientInt recipient, String quickAccessToken);

    public String generateNotificationAARSubject(NotificationInt notification);

    public String generateNotificationAARForSMS(NotificationInt notification);
}
