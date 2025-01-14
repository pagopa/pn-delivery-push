package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.commons.utils.FileUtils;
import it.pagopa.pn.deliverypush.action.utils.EndWorkflowStatus;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressSourceInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.*;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.ResponseStatusInt;
import it.pagopa.pn.deliverypush.dto.mandate.DelegateInfoInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendDigitalFeedbackDetailsInt;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.templatesengine.model.*;
import it.pagopa.pn.deliverypush.legalfacts.CustomInstantWriter;
import it.pagopa.pn.deliverypush.legalfacts.PhysicalAddressWriter;
import org.springframework.util.CollectionUtils;

import java.time.Instant;
import java.util.*;

public class TemplatesEngineMapper {

    private TemplatesEngineMapper() {
    }

    /**
     * Constructs a {@link NotificationAarRaddAlt} object for an AAR notification
     * with alternative details.
     *
     * @param notification the {@link NotificationInt} object containing details about the notification,
     *                     including its unique identifier (IUN), subject, and sender information.
     * @param recipient    the {@link NotificationRecipientInt} object representing the recipient of the notification,
     *                     including their tax ID and recipient type.
     * @return a {@link NotificationAarRaddAlt} object containing all the necessary information for the AAR
     *         notification with alternative details.
     * @throws IllegalArgumentException if any required parameter is null or contains invalid data.
     */
    public static NotificationAarRaddAlt notificationAARRADDalt(NotificationInt notification,
                                                                NotificationRecipientInt recipient,
                                                                String qrCodeQuickAccessUrlAarDetail,
                                                                String accessUrl,
                                                                String accessUrlLabel,
                                                                String accessLink,
                                                                String accessLinkLabel,
                                                                String perfezionamentoLink,
                                                                String perfezionamentoLinkLabel,
                                                                String raddPhoneNumber) {
        AarRaddAltSender sender = new AarRaddAltSender()
                .paDenomination(notification.getSender().getPaDenomination());

        AarRaddAltNotification altNotification = new AarRaddAltNotification()
                .iun(notification.getIun())
                .subject(notification.getSubject())
                .sender(sender);

        AarRaddAltRecipient aarRecipient = new AarRaddAltRecipient()
                .recipientType(recipient.getRecipientType().getValue())
                .taxId(recipient.getTaxId())
                .denomination(recipient.getDenomination());

        return new NotificationAarRaddAlt()
                .notification(altNotification)
                .recipient(aarRecipient)
                .piattaformaNotificheURL(accessUrl)
                .piattaformaNotificheURLLabel(accessUrlLabel)
                .sendURL(accessLink)
                .sendURLLAbel(accessLinkLabel)
                .perfezionamentoURL(perfezionamentoLink)
                .perfezionamentoURLLabel(perfezionamentoLinkLabel)
                .qrCodeQuickAccessLink(qrCodeQuickAccessUrlAarDetail)
                .raddPhoneNumber(raddPhoneNumber);
    }

    /**
     * Constructs a {@link NotificationAar} object for an Acknowledgment of Receipt (AAR) notification.
     *
     * @param notification the {@link NotificationInt} object containing the details about the notification,
     *                     including its unique identifier (IUN), subject, and sender information.
     * @param recipient    the {@link NotificationRecipientInt} object representing the recipient of the notification,
     *                     including their tax ID and recipient type.
     * @param qrCodeQuickAccessUrlAarDetail a {@link String} representing the token used to generate the quick access QR code link
     *                         for the notification details.
     * @return a {@link NotificationAar} object containing all the necessary information for the AAR notification.
     * @throws IllegalArgumentException if any required parameter is null or contains invalid data.
     */
    public static NotificationAar notificationAAR(NotificationInt notification,
                                                  NotificationRecipientInt recipient,
                                                  String qrCodeQuickAccessUrlAarDetail,
                                                  String accessUrl,
                                                  String accessUrlLabel,
                                                  String perfezionamentoLink,
                                                  String perfezionamentoLinkLabel) {
        AarSender sender = new AarSender()
                .paDenomination(notification.getSender().getPaDenomination());

        AarNotification aarNotification = new AarNotification()
                .iun(notification.getIun())
                .subject(notification.getSubject())
                .sender(sender);

        AarRecipient aarRecipient = new AarRecipient()
                .recipientType(recipient.getRecipientType().getValue())
                .taxId(recipient.getTaxId());

        return new NotificationAar()
                .notification(aarNotification)
                .recipient(aarRecipient)
                .piattaformaNotificheURL(accessUrl)
                .piattaformaNotificheURLLabel(accessUrlLabel)
                .perfezionamentoURL(perfezionamentoLink)
                .perfezionamentoURLLabel(perfezionamentoLinkLabel)
                .qrCodeQuickAccessLink(qrCodeQuickAccessUrlAarDetail);
    }

    public static NotificationAarForSms notificationAarForSms(NotificationInt notification) {
        AarForSmsSender sender = new AarForSmsSender()
                .paDenomination(notification.getSender().getPaDenomination());

        AarForSmsNotification aarForSmsNotification = new AarForSmsNotification()
                .iun(notification.getIun())
                .sender(sender);

        return new NotificationAarForSms()
                .notification(aarForSmsNotification);
    }

    public static NotificationAarForPec notificationAarForPec(NotificationInt notification,
                                                              NotificationRecipientInt recipient,
                                                              String qrCodeQuickAccessUrlAarDetail,
                                                              String perfezionamentoLink,
                                                              String faqSendURL,
                                                              String accessUrl,
                                                              String recipientTypeForHTMLTemplate) {
        AarForPecSender sender = new AarForPecSender()
                .paDenomination(notification.getSender().getPaDenomination());

        AarForPecNotification pecNotification = new AarForPecNotification()
                .iun(notification.getIun())
                .subject(notification.getSubject())
                .sender(sender);

        AarForPecRecipient aarForPecRecipient = new AarForPecRecipient()
                .taxId(recipient.getTaxId());

        return new NotificationAarForPec()
                .perfezionamentoURL(perfezionamentoLink)
                .quickAccessLink(qrCodeQuickAccessUrlAarDetail)
                .pnFaqSendURL(faqSendURL)
                .piattaformaNotificheURL(accessUrl)
                .notification(pecNotification)
                .recipient(aarForPecRecipient)
                .recipientType(recipientTypeForHTMLTemplate);
    }

    public static NotificationAarForEmail notificationAarForEmail(NotificationInt notification,
                                                                  String perfezionamentoLink,
                                                                  String qrCodeQuickAccessUrlAarDetail,
                                                                  String faqSendURL,
                                                                  String accessUrl) {
        AarForEmailSender sender = new AarForEmailSender()
                .paDenomination(notification.getSender().getPaDenomination());

        AarForEmailNotification aarForEmailNotification = new AarForEmailNotification()
                .iun(notification.getIun())
                .sender(sender);

        return new NotificationAarForEmail()
                .perfezionamentoURL(perfezionamentoLink)
                .quickAccessLink(qrCodeQuickAccessUrlAarDetail)
                .pnFaqSendURL(faqSendURL)
                .piattaformaNotificheURL(accessUrl)
                .notification(aarForEmailNotification);
    }

    public static NotificationAarForSubject notificationAARSubject(NotificationInt notification) {
        AarForSubjectSender sender = new AarForSubjectSender()
                .paDenomination(notification.getSender().getPaDenomination());

        AarForSubjectNotification aarForSubjectNotification = new AarForSubjectNotification()
                .sender(sender)
                .iun(notification.getIun());

        return new NotificationAarForSubject()
                .notification(aarForSubjectNotification);
    }

    public static NotificationCancelledLegalFact cancelledLegalFact(NotificationInt notification,
                                                                    Instant notificationCancellationRequestDate,
                                                                    CustomInstantWriter instantWriter) {
        NotificationCancelledSender sender = new NotificationCancelledSender()
                .paDenomination(notification.getSender().getPaDenomination());

        List<NotificationCancelledRecipient> recipients = notification.getRecipients()
                .stream()
                .map(recipientInt -> new NotificationCancelledRecipient()
                        .denomination(recipientInt.getDenomination())
                        .taxId(recipientInt.getTaxId()))
                .toList();

        NotificationCancelledNotification notificationCancelledNotification = new NotificationCancelledNotification()
                .iun(notification.getIun())
                .recipients(recipients)
                .sender(sender);

        return new NotificationCancelledLegalFact()
                .notificationCancelledDate(instantWriter.instantToDate(notificationCancellationRequestDate))
                .notification(notificationCancelledNotification);
    }

    public static AnalogDeliveryWorkflowFailureLegalFact analogDeliveryWorkflowFailureLegalFact(NotificationInt notification,
                                                                                                NotificationRecipientInt recipient,
                                                                                                Instant failureWorkflowDate,
                                                                                                CustomInstantWriter instantWriter) {
        AnalogDeliveryWorkflowFailureRecipient analogDeliveryWorkflowFailureRecipient = new AnalogDeliveryWorkflowFailureRecipient()
                .denomination(recipient.getDenomination())
                .taxId(recipient.getTaxId());

        return new AnalogDeliveryWorkflowFailureLegalFact()
                .iun(notification.getIun())
                .recipient(analogDeliveryWorkflowFailureRecipient)
                .endWorkflowDate(instantWriter.instantToDate(failureWorkflowDate, true))
                .endWorkflowTime(instantWriter.instantToTime(failureWorkflowDate));
    }

    public static PecDeliveryWorkflowLegalFact pecDeliveryWorkflowLegalFact(List<SendDigitalFeedbackDetailsInt> feedbackFromExtChannelList,
                                                                            NotificationInt notification,
                                                                            NotificationRecipientInt recipient,
                                                                            EndWorkflowStatus status,
                                                                            Instant completionWorkflowDate,
                                                                            CustomInstantWriter instantWriter) {
        List<PecDeliveryWorkflowDelivery> pecDeliveries = feedbackFromExtChannelList.stream()
                .map(feedbackFromExtChannel -> {
                    ResponseStatusInt sentPecStatus = feedbackFromExtChannel.getResponseStatus();
                    Instant notificationDate = feedbackFromExtChannel.getNotificationDate();
                    String addressSource = Optional.ofNullable(feedbackFromExtChannel.getDigitalAddressSource())
                            .map(DigitalAddressSourceInt::getValue)
                            .orElse(null);
                    return new PecDeliveryWorkflowDelivery()
                            .denomination(recipient.getDenomination())
                            .taxId(recipient.getTaxId())
                            .address(feedbackFromExtChannel.getDigitalAddress().getAddress())
                            .addressSource(addressSource)
                            .type(feedbackFromExtChannel.getDigitalAddress().getType().getValue())
                            .responseDate(instantWriter.instantToDate(notificationDate))
                            .ok(ResponseStatusInt.OK.equals(sentPecStatus));
                })
                .sorted(Comparator.comparing(PecDeliveryWorkflowDelivery::getResponseDate))
                .toList();

        return new PecDeliveryWorkflowLegalFact()
                .iun(notification.getIun())
                .endWorkflowStatus(status.toString())
                .deliveries(pecDeliveries)
                .endWorkflowDate(instantWriter.instantToDate(completionWorkflowDate));
    }

    public static NotificationViewedLegalFact notificationViewedLegalFact(String iun,
                                                                          NotificationRecipientInt recipient,
                                                                          DelegateInfoInt delegateInfo,
                                                                          Instant timeStamp,
                                                                          CustomInstantWriter instantWriter) {
        NotificationViewedRecipient notificationViewedRecipient = new NotificationViewedRecipient()
                .denomination(recipient.getDenomination())
                .taxId(recipient.getTaxId());

        return new NotificationViewedLegalFact()
                .recipient(notificationViewedRecipient)
                .iun(iun)
                .delegate(notificationViewedDelegate(delegateInfo))
                .when(instantWriter.instantToDate(timeStamp));
    }

    private static NotificationViewedDelegate notificationViewedDelegate(DelegateInfoInt delegateInfo) {
        return delegateInfo != null ?
                new NotificationViewedDelegate()
                        .denomination(delegateInfo.getDenomination())
                        .taxId(delegateInfo.getTaxId())
                : null;
    }

    public static NotificationReceivedLegalFact notificationReceivedLegalFact(NotificationInt notification,
                                                                              PhysicalAddressWriter physicalAddressWriter,
                                                                              CustomInstantWriter instantWriter) {
        String physicalAddressAndDenomination;
        List<NotificationRecipientInt> recipients = Optional.of(notification)
                .map(NotificationInt::getRecipients)
                .orElse(new ArrayList<>());

        List<NotificationReceivedRecipient> receivedRecipients = new ArrayList<>();
        for (var recipientInt : recipients) {
            String denomination = recipientInt.getDenomination();
            physicalAddressAndDenomination = physicalAddressWriter.nullSafePhysicalAddressToString(
                    recipientInt.getPhysicalAddress(), denomination, "<br/>");
            NotificationReceivedRecipient notificationReceivedNotification = notificationReceivedNotification(physicalAddressAndDenomination,
                    recipientInt);
            receivedRecipients.add(notificationReceivedNotification);
        }

        NotificationReceivedNotification notificationReceivedNotification = new NotificationReceivedNotification()
                .iun(notification.getIun())
                .recipients(receivedRecipients)
                .sender(sender(notification));

        return new NotificationReceivedLegalFact()
                .sendDate(instantWriter.instantToDate(notification.getSentAt()))
                .subject(notification.getSubject())
                .notification(notificationReceivedNotification)
                .digests(extractNotificationAttachmentDigests(notification));
    }

    private static NotificationReceivedRecipient notificationReceivedNotification(String physicalAddressAndDenomination,
                                                                                  NotificationRecipientInt recipientInt) {
        return recipientInt != null ?
                new NotificationReceivedRecipient()
                        .physicalAddressAndDenomination(physicalAddressAndDenomination)
                        .denomination(recipientInt.getDenomination())
                        .taxId(recipientInt.getTaxId())
                        .digitalDomicile(digitalDomicile(recipientInt.getDigitalDomicile())) : null;
    }

    private static NotificationReceivedDigitalDomicile digitalDomicile(LegalDigitalAddressInt domicile) {
        return domicile != null ? new NotificationReceivedDigitalDomicile().address(domicile.getAddress()) : null;
    }

    private static NotificationReceivedSender sender(NotificationInt notification) {
        var senderInt = Optional.of(notification).map(NotificationInt::getSender).orElse(null);
        return senderInt != null ?
                new NotificationReceivedSender()
                        .paDenomination(senderInt.getPaDenomination())
                        .paTaxId(senderInt.getPaTaxId())
                : null;
    }

    /**
     * Extracts the SHA-256 digests of the attachments related to a notification.
     *
     * @param notification the {@link NotificationInt} object containing the details of the notification,
     *                     including its attached documents and recipients with payment information.
     * @return a {@link List} of {@link String} representing the SHA-256 digests (in hexadecimal uppercase)
     * of all relevant attachments from the notification.
     */
    private static List<String> extractNotificationAttachmentDigests(NotificationInt notification) {
        List<String> digests = new ArrayList<>();
        // - Documents digests
        for (NotificationDocumentInt attachment : notification.getDocuments()) {
            digests.add(FileUtils.convertBase64toHexUppercase(attachment.getDigests().getSha256()));
        }
        // F24 digests
        for (NotificationRecipientInt recipient : notification.getRecipients()) {
            //add digests for v21
            addDigestsForMultiPayments(recipient.getPayments(), digests);
        }
        return digests;
    }

    /**
     * Adds the SHA-256 digests of the attachments related to the payments made by the recipient.
     *
     * @param payments a {@link List} of {@link NotificationPaymentInfoInt} objects representing the payments
     *                 made by the recipient, potentially containing attachments.
     * @param digests  a {@link List} of {@link String} where the extracted digests will be added.
     */
    private static void addDigestsForMultiPayments(List<NotificationPaymentInfoInt> payments, List<String> digests) {
        if (!CollectionUtils.isEmpty(payments)) {
            payments.forEach(payment -> {
                if (payment.getPagoPA() != null && payment.getPagoPA().getAttachment() != null) {
                    digests.add(FileUtils.convertBase64toHexUppercase(payment.getPagoPA().getAttachment().getDigests().getSha256()));
                }
                if (payment.getF24() != null && payment.getF24().getMetadataAttachment() != null) {
                    digests.add(FileUtils.convertBase64toHexUppercase(payment.getF24().getMetadataAttachment().getDigests().getSha256()));
                }
            });
        }
    }
}
