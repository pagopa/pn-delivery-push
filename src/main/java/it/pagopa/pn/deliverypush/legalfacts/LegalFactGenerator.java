package it.pagopa.pn.deliverypush.legalfacts;

import it.pagopa.pn.deliverypush.action.utils.EndWorkflowStatus;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.AARInfo;
import it.pagopa.pn.deliverypush.dto.mandate.DelegateInfoInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendDigitalFeedbackDetailsInt;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

public interface LegalFactGenerator {

    /**
     * Generates the legal fact for a received notification.
     *
     * @param notification the notification object containing details about the notification.
     * @return a byte array representing the pdf legal fact for the received notification.
     */
    byte[] generateNotificationReceivedLegalFact(NotificationInt notification) throws IOException;

    /**
     * Generates the legal fact for a cancelled notification.
     *
     * @param notification the notification object containing details about the notification.
     * @param notificationCancellationRequestDate the timestamp of the cancellation request.
     * @return a byte array representing the pdf legal fact for the cancelled notification.
     */
    byte[] generateNotificationCancelledLegalFact(NotificationInt notification, Instant notificationCancellationRequestDate) throws IOException;

    /**
     * Generates the legal fact for a viewed notification.
     *
     * @param iun the unique identifier of the notification.
     * @param recipient the recipient of the notification.
     * @param delegateInfo additional delegate information (if any).
     * @param timeStamp the timestamp when the notification was viewed.
     * @param notification the notification object containing details about the notification.
     * @return a byte array representing the pdf legal fact for the viewed notification.
     */
    byte[] generateNotificationViewedLegalFact(String iun, NotificationRecipientInt recipient,
                                                      DelegateInfoInt delegateInfo,
                                                      Instant timeStamp,
                                                      NotificationInt notification) throws IOException;

    /**
     * Generates the legal fact for a PEC delivery workflow.
     *
     * @param feedbackFromExtChannelList a list of feedback details from external channels.
     * @param notification the notification object containing details about the notification.
     * @param recipient the recipient of the notification.
     * @param status the end workflow status.
     * @param completionWorkflowDate the timestamp when the workflow was completed.
     * @return a byte array representing the pdf legal fact for the PEC delivery workflow.
     */
    byte[] generatePecDeliveryWorkflowLegalFact(List<SendDigitalFeedbackDetailsInt> feedbackFromExtChannelList,
                                                       NotificationInt notification,
                                                       NotificationRecipientInt recipient,
                                                       EndWorkflowStatus status,
                                                       Instant completionWorkflowDate) throws IOException;

    /**
     * Generates the legal fact for an analog delivery failure workflow.
     *
     * @param notification the notification object containing details about the notification.
     * @param recipient the recipient of the notification.
     * @param status the end workflow status.
     * @param failureWorkflowDate the timestamp when the failure workflow occurred.
     * @return a byte array representing the pdf legal fact for the analog delivery failure workflow.
     */
    byte[] generateAnalogDeliveryFailureWorkflowLegalFact(NotificationInt notification,
                                                                 NotificationRecipientInt recipient,
                                                                 EndWorkflowStatus status,
                                                                 Instant failureWorkflowDate) throws IOException;

    /**
     * Generates an AAR information for a notification.
     *
     * @param notification the notification object containing details about the notification.
     * @param recipient the recipient of the notification.
     * @param quickAccessToken a token for quick access to the AAR.
     * @return an {@link AARInfo} object containing the AAR information.
     */
    AARInfo generateNotificationAAR(NotificationInt notification, NotificationRecipientInt recipient, String quickAccessToken) throws IOException;

    /**
     * Generates the body of the AAR notification.
     *
     * @param notification the notification object containing details about the notification.
     * @param recipient the recipient of the notification.
     * @param quickAccessToken a token for quick access to the AAR.
     * @return a {@link String} representing the body of the AAR notification.
     */
    String generateNotificationAARBody(NotificationInt notification, NotificationRecipientInt recipient, String quickAccessToken);

    /**
     * Generates the body of the AAR PEC notification.
     *
     * @param notification the notification object containing details about the notification.
     * @param recipient the recipient of the notification.
     * @param quickAccessToken a token for quick access to the AAR PEC.
     * @return a {@link String} representing the body of the AAR PEC notification.
     */
    String generateNotificationAARPECBody(NotificationInt notification, NotificationRecipientInt recipient, String quickAccessToken);

    /**
     * Generates the subject for the AAR notification.
     *
     * @param notification the notification object containing details about the notification.
     * @return a {@link String} representing the subject of the AAR notification.
     */
    String generateNotificationAARSubject(NotificationInt notification);

    /**
     * Generates the AAR notification for SMS.
     *
     * @param notification the notification object containing details about the notification.
     * @return a {@link String} representing the AAR notification for SMS.
     */
    String generateNotificationAARForSMS(NotificationInt notification);

    /**
     * Generates a PDF based on AnalogDeliveryWorkflowTimeoutLegalFact template.
     *
     * @param notification the notification object containing details about the notification.
     * @param timeoutDate analog delivery workflow timeout date and time
     * @return A byte array representing the generated legal fact for the analog delivery workflow timeout.
     */
    byte[] generateAnalogDeliveryWorkflowTimeoutLegalFact(NotificationInt notification,
                                                          NotificationRecipientInt recipient,
                                                          PhysicalAddressInt physicalAddress,
                                                          String sentAttemptMade,
                                                          Instant timeoutDate) throws IOException;
}
