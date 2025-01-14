package it.pagopa.pn.deliverypush.legalfacts;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.utils.FileUtils;
import it.pagopa.pn.deliverypush.action.utils.EndWorkflowStatus;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressSourceInt;
import it.pagopa.pn.deliverypush.dto.ext.datavault.RecipientTypeInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.*;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.ResponseStatusInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.AARInfo;
import it.pagopa.pn.deliverypush.dto.mandate.DelegateInfoInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendDigitalFeedbackDetailsInt;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.templatesengine.model.*;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.templatesengine.TemplatesClient;
import it.pagopa.pn.deliverypush.utils.PnSendMode;
import it.pagopa.pn.deliverypush.utils.PnSendModeUtils;
import it.pagopa.pn.deliverypush.utils.QrCodeUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Base64Utils;
import org.springframework.util.CollectionUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_CONFIGURATION_NOT_FOUND;
import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_INVALID_TEMPLATE;

@Slf4j
@AllArgsConstructor
public class LegalFactGeneratorTemplates implements LegalFactGenerator {

    private final CustomInstantWriter instantWriter;
    private final PhysicalAddressWriter physicalAddressWriter;
    private final PnDeliveryPushConfigs pnDeliveryPushConfigs;
    private final PnSendModeUtils pnSendModeUtils;
    private final TemplatesClient templatesClient;

    /**
     * Generates the legal fact for a received notification.
     *
     * @param notification the {@link NotificationInt} object containing notification details,
     *                     including sender, recipients, and metadata.
     * @return a byte[] representing the generated pdf notification received legal fact.
     * @throws IllegalArgumentException if the notification is null or contains incomplete data.
     *
     * <p><strong>Note:</strong></p>
     * Ensure the {@code templatesClient} is properly configured to handle the generated
     * {@link NotificationReceivedLegalFact} and return the expected byte array.
     */
    @Override
    public byte[] generateNotificationReceivedLegalFact(NotificationInt notification) {
        log.info("retrieve NotificationReceivedLegalFact template for iun {}", notification.getIun());
        String physicalAddressAndDenomination;
        List<NotificationRecipientInt> recipients = Optional.of(notification)
                .map(NotificationInt::getRecipients)
                .orElse(new ArrayList<>());

        List<NotificationReceivedRecipient> receivedRecipients = new ArrayList<>();
        for (var recipientInt : recipients) {
            String denomination = recipientInt.getDenomination();
            physicalAddressAndDenomination = physicalAddressWriter.nullSafePhysicalAddressToString(
                    recipientInt.getPhysicalAddress(), denomination, "<br/>");
            NotificationReceivedDigitalDomicile digitalDomicile = new NotificationReceivedDigitalDomicile()
                    .address( Optional.of(recipientInt).map(NotificationRecipientInt::getDigitalDomicile)
                            .map(DigitalAddressInt::getAddress).orElse(null));

            NotificationReceivedRecipient notificationReceivedRecipient = new NotificationReceivedRecipient()
                    .physicalAddressAndDenomination(physicalAddressAndDenomination)
                    .denomination(recipientInt.getDenomination())
                    .taxId(recipientInt.getTaxId())
                    .digitalDomicile(digitalDomicile);

            receivedRecipients.add(notificationReceivedRecipient);
        }

        var senderInt = Optional.of(notification).map(NotificationInt::getSender).orElse(new NotificationSenderInt());
        NotificationReceivedSender sender = new NotificationReceivedSender()
                .paDenomination(senderInt.getPaDenomination())
                .paTaxId(senderInt.getPaTaxId());

        NotificationReceivedNotification notificationReceivedNotification = new NotificationReceivedNotification()
                .iun(notification.getIun())
                .recipients(receivedRecipients)
                .sender(sender);

        NotificationReceivedLegalFact legalFact = new NotificationReceivedLegalFact()
                .sendDate(instantWriter.instantToDate(notification.getSentAt()))
                .subject(notification.getSubject())
                .notification(notificationReceivedNotification)
                .digests(extractNotificationAttachmentDigests(notification));

        LanguageEnum language = getLanguage(notification.getAdditionalLanguages());
        return templatesClient.notificationReceivedLegalFact(language, legalFact);
    }

    /**
     * Generates the legal fact for the viewing of a notification.
     *
     * @param iun           the unique identifier of the notification (IUN).
     * @param recipient     the recipient of the notification, represented by a
     *                      {@link NotificationRecipientInt} object containing information such
     *                      as name (denomination) and tax ID.
     * @param delegateInfo  the delegate's information (if present), represented by a
     *                      {@link DelegateInfoInt} object containing name and tax ID.
     * @param timeStamp     the timestamp of when the notification was viewed, as an {@link Instant} object.
     * @param notification  the {@link NotificationInt} object representing the full notification,
     *                      from which additional information such as additional languages is extracted.
     * @return a byte array representing the pdf legal fact of the notification viewing.
     * @throws IllegalArgumentException if any required parameter is null or contains incomplete data.
     *
     * <p><strong>Note:</strong></p>
     * Ensure that {@code templatesClient} is properly configured to handle the generated
     * {@link NotificationViewedLegalFact} object and return the expected byte array.
     */
    @Override
    public byte[] generateNotificationViewedLegalFact(String iun,
                                                      NotificationRecipientInt recipient,
                                                      DelegateInfoInt delegateInfo,
                                                      Instant timeStamp,
                                                      NotificationInt notification) {
        log.info("retrieve NotificationViewedLegalFact template for iun {}", iun);
        NotificationViewedRecipient notificationViewedRecipient = new NotificationViewedRecipient()
                .denomination(recipient.getDenomination())
                .taxId(recipient.getTaxId());

        NotificationViewedLegalFact notificationViewedLegalFact = new NotificationViewedLegalFact()
                .recipient(notificationViewedRecipient)
                .iun(iun)
                .delegate(notificationViewedDelegate(delegateInfo))
                .when(instantWriter.instantToDate(timeStamp));

        LanguageEnum language = getLanguage(notification.getAdditionalLanguages());
        return templatesClient.notificationViewedLegalFact(language, notificationViewedLegalFact);
    }

    /**
     * Generates the legal fact for the PEC delivery workflow.
     *
     * @param feedbackFromExtChannelList the list of {@link SendDigitalFeedbackDetailsInt} objects
     *                                   representing feedback from external digital channels, including
     *                                   delivery status and notification dates.
     * @param notification               the {@link NotificationInt} object containing details of the
     *                                   notification, such as its unique identifier (IUN).
     * @param recipient                  the recipient of the notification, represented by a
     *                                   {@link NotificationRecipientInt} object, containing information
     *                                   such as name (denomination) and tax ID.
     * @param status                     the {@link EndWorkflowStatus} representing the final status
     *                                   of the PEC delivery workflow (e.g., completed, failed).
     * @param completionWorkflowDate     the {@link Instant} representing the timestamp when the
     *                                   PEC delivery workflow was completed.
     * @return a byte array representing the pdf PEC delivery workflow legal fact.
     * @throws IllegalArgumentException if any required parameter is null or contains invalid data.
     *
     * <p><strong>Note:</strong></p>
     * Ensure that {@code templatesClient} is correctly configured to handle the generated
     * {@link PecDeliveryWorkflowLegalFact} object and return the expected byte array.
     */
    @Override
    public byte[] generatePecDeliveryWorkflowLegalFact(List<SendDigitalFeedbackDetailsInt> feedbackFromExtChannelList,
                                                       NotificationInt notification,
                                                       NotificationRecipientInt recipient,
                                                       EndWorkflowStatus status,
                                                       Instant completionWorkflowDate) {
        log.info("retrieve PecDeliveryWorkflowLegalFact template for iun {}", notification.getIun());
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

        PecDeliveryWorkflowLegalFact pecDeliveryWorkflowLegalFact = new PecDeliveryWorkflowLegalFact()
                .iun(notification.getIun())
                .endWorkflowStatus(status.toString())
                .deliveries(pecDeliveries)
                .endWorkflowDate(instantWriter.instantToDate(completionWorkflowDate));

        LanguageEnum language = getLanguage(notification.getAdditionalLanguages());
        return templatesClient.pecDeliveryWorkflowLegalFact(language, pecDeliveryWorkflowLegalFact);
    }

    /**
     * Generates the legal fact for the failure of the analog delivery workflow.
     *
     * @param notification       the {@link NotificationInt} object containing the details of the notification,
     *                           such as its unique identifier (IUN).
     * @param recipient          the recipient of the notification, represented by a
     *                           {@link NotificationRecipientInt} object, which includes information such
     *                           as name (denomination) and tax ID.
     * @param status             the {@link EndWorkflowStatus} representing the final status of the analog
     *                           delivery workflow (e.g., failed).
     * @param failureWorkflowDate the {@link Instant} representing the timestamp when the analog delivery
     *                            workflow failure occurred.
     * @return a byte array representing the pdf legal fact for the analog delivery workflow failure.
     * @throws IllegalArgumentException if any required parameter is null or contains invalid data.
     *
     * <p><strong>Note:</strong></p>
     * Ensure that {@code templatesClient} is properly configured to handle the generated
     * {@link AnalogDeliveryWorkflowFailureLegalFact} object and return the expected byte array.
     */
    @Override
    public byte[] generateAnalogDeliveryFailureWorkflowLegalFact(NotificationInt notification,
                                                                 NotificationRecipientInt recipient,
                                                                 EndWorkflowStatus status,
                                                                 Instant failureWorkflowDate) {
        log.info("retrieve AnalogDeliveryFailureWorkflowLegalFact template for iun {}", notification.getIun());
        AnalogDeliveryWorkflowFailureRecipient analogDeliveryWorkflowFailureRecipient = new AnalogDeliveryWorkflowFailureRecipient()
                .denomination(recipient.getDenomination())
                .taxId(recipient.getTaxId());

        AnalogDeliveryWorkflowFailureLegalFact analogDeliveryWorkflowFailureLegalFact = new AnalogDeliveryWorkflowFailureLegalFact()
                .iun(notification.getIun())
                .recipient(analogDeliveryWorkflowFailureRecipient)
                .endWorkflowDate(instantWriter.instantToDate(failureWorkflowDate, true))
                .endWorkflowTime(instantWriter.instantToTime(failureWorkflowDate));

        LanguageEnum language = getLanguage(notification.getAdditionalLanguages());
        return templatesClient.analogDeliveryWorkflowFailureLegalFact(language, analogDeliveryWorkflowFailureLegalFact);
    }

    /**
     * Generates the legal fact for a cancelled notification.
     *
     * @param notification                      the {@link NotificationInt} object containing details
     *                                          about the notification, including its unique identifier (IUN),
     *                                          sender information, and recipients.
     * @param notificationCancellationRequestDate the {@link Instant} representing the timestamp when
     *                                            the notification cancellation request was made.
     * @return a byte array representing the legal fact for the cancelled notification.
     *
     * @throws IllegalArgumentException if any required parameter is null or contains invalid data.
     *
     * <p><strong>Note:</strong></p>
     * Ensure that {@code templatesClient} is properly configured to handle the generated
     * {@link NotificationCancelledLegalFact} object and return the expected pdf byte array.
     */
    @Override
    public byte[] generateNotificationCancelledLegalFact(NotificationInt notification, Instant notificationCancellationRequestDate) {
        log.info("retrieve NotificationCancelledLegalFact template for iun {}", notification.getIun());
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

        NotificationCancelledLegalFact cancelledLegalFact = new NotificationCancelledLegalFact()
                .notificationCancelledDate(instantWriter.instantToDate(notificationCancellationRequestDate))
                .notification(notificationCancelledNotification);

        LanguageEnum language = getLanguage(notification.getAdditionalLanguages());
        return templatesClient.notificationCancelledLegalFact(language, cancelledLegalFact);
    }

    /**
     * Generates the AAR subject for a notification.
     *
     * @param notification the {@link NotificationInt} object containing details about the notification,
     *                     including its unique identifier (IUN) and sender information.
     * @return a {@link String} representing the subject line for the AAR of the notification.
     * @throws IllegalArgumentException if any required parameter is null or contains invalid data.
     *
     * <p><strong>Note:</strong></p>
     * Ensure that {@code templatesClient} is properly configured to handle the generated
     * {@link NotificationAarForSubject} object and return the expected subject string.
     */
    @Override
    public String generateNotificationAARSubject(NotificationInt notification) {
        log.info("retrieve NotificationAARSubject template for iun {}", notification.getIun());
        AarForSubjectSender sender = new AarForSubjectSender()
                .paDenomination(notification.getSender().getPaDenomination());

        AarForSubjectNotification aarForSubjectNotification = new AarForSubjectNotification()
                .sender(sender)
                .iun(notification.getIun());

        NotificationAarForSubject notificationAARSubject = new NotificationAarForSubject()
                .notification(aarForSubjectNotification);

        LanguageEnum language = getLanguage(notification.getAdditionalLanguages());
        return templatesClient.notificationAarForSubject(language, notificationAARSubject);
    }

    /**
     * Extracts the SHA-256 digests of the attachments related to a notification.
     *
     * @param notification the {@link NotificationInt} object containing the details of the notification,
     *                     including its attached documents and recipients with payment information.
     * @return a {@link List} of {@link String} representing the SHA-256 digests (in hexadecimal uppercase)
     *         of all relevant attachments from the notification.
     */
    private List<String> extractNotificationAttachmentDigests(NotificationInt notification) {
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
    private void addDigestsForMultiPayments(List<NotificationPaymentInfoInt> payments, List<String> digests) {
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

    /**
     * Generates the AAR subject for a notification.
     *
     * @param notification the {@link NotificationInt} object containing details
     *                     about the notification, including its unique identifier (IUN)
     *                     and sender information.
     * @return a {@link AARInfo} representing the AAR info for the notification.
     *
     * @throws IllegalArgumentException if the notification is null or contains invalid data.
     *
     * <p><strong>Note:</strong></p>
     * Ensure that {@code templatesClient} is properly configured to handle the generated
     * {@link NotificationAarForSubject} object and return the expected subject string.
     */
    @Override
    public AARInfo generateNotificationAAR(NotificationInt notification, NotificationRecipientInt recipient, String quickAccessToken) {
        LanguageEnum language = getLanguage(notification.getAdditionalLanguages());
        PnSendMode pnSendMode = pnSendModeUtils.getPnSendMode(notification.getSentAt());
        if (pnSendMode != null) {
            final AarTemplateChooseStrategy aarTemplateTypeChooseStrategy = pnSendMode.getAarTemplateTypeChooseStrategy();
            final AarTemplateType aarTemplateType = aarTemplateTypeChooseStrategy.choose(recipient.getPhysicalAddress());
            log.debug("aarTemplateType generated is ={} - iun={}", aarTemplateType, notification.getIun());
            byte[] bytesArrayGeneratedAar = new byte[0];
            switch (aarTemplateType) {
                case AAR_NOTIFICATION -> {
                    log.info("retrieve NotificationAAR template for iun {}", notification.getIun());
                    NotificationAar notificationAAR = getNotificationAAR(notification, recipient, quickAccessToken);
                    bytesArrayGeneratedAar = templatesClient.notificationAar(language, notificationAAR);
                }
                case AAR_NOTIFICATION_RADD_ALT -> {
                    log.info("retrieve NotificationAARRADDalt template for iun {}", notification.getIun());
                    NotificationAarRaddAlt notificationAARRADDalt = getNotificationAARRADDalt(notification, recipient, quickAccessToken);
                    bytesArrayGeneratedAar = templatesClient.notificationAarRaddAlt(language, notificationAARRADDalt);
                }
                case AAR_NOTIFICATION_RADD -> //TODO da vedere
                        throw new PnInternalException("NotificationAAR_RADD not implemented", ERROR_CODE_DELIVERYPUSH_INVALID_TEMPLATE);
            }
            return AARInfo.builder()
                    .bytesArrayGeneratedAar(bytesArrayGeneratedAar)
                    .templateType(aarTemplateType)
                    .build();
        } else {
            String msg = String.format("There isn't correct AAR configuration for date=%s - iun=%s", notification.getSentAt(), notification.getIun());
            log.error(msg);
            throw new PnInternalException(msg, ERROR_CODE_DELIVERYPUSH_CONFIGURATION_NOT_FOUND);
        }
    }

    /**
     * Generates the AAR body for a notification.
     *
     * @param notification      the {@link NotificationInt} object containing details about the notification,
     *                          including its unique identifier (IUN).
     * @param recipient         the {@link NotificationRecipientInt} object representing the recipient of the notification,
     *                          including relevant details such as contact information.
     * @param quickAccessToken  a {@link String} representing the token used to generate the quick access URL
     *                          for the notification details.
     * @return a {@link String} representing the body of the AAR email for the notification.
     *
     * @throws IllegalArgumentException if any required parameter is null or contains invalid data.
     *
     * <p><strong>Note:</strong></p>
     * Ensure that {@code templatesClient} is properly configured to handle the generated
     * {@link NotificationAarForEmail} object and return the expected email body string.
     */
    @Override
    public String generateNotificationAARBody(NotificationInt notification, NotificationRecipientInt recipient, String quickAccessToken) {
        log.info("retrieve NotificationAARBody template for iun {}", notification.getIun());
        String qrCodeQuickAccessUrlAarDetail = this.getQrCodeQuickAccessUrlAarDetail(recipient, quickAccessToken);

        AarForEmailSender sender = new AarForEmailSender()
                .paDenomination(notification.getSender().getPaDenomination());

        AarForEmailNotification aarForEmailNotification = new AarForEmailNotification()
                .iun(notification.getIun())
                .sender(sender);

        NotificationAarForEmail notificationAAR = new NotificationAarForEmail()
                .perfezionamentoURL(this.getPerfezionamentoLink())
                .quickAccessLink(qrCodeQuickAccessUrlAarDetail)
                .pnFaqSendURL(this.getFAQSendURL())
                .piattaformaNotificheURL(this.getAccessUrl(recipient))
                .notification(aarForEmailNotification);

        LanguageEnum language = getLanguage(notification.getAdditionalLanguages());
        return templatesClient.notificationAarForEmail(language, notificationAAR);
    }

    /**
     * Generates the AAR body for a PEC notification.
     *
     * @param notification      the {@link NotificationInt} object containing details about the notification,
     *                          including its unique identifier (IUN) and sender information.
     * @param recipient         the {@link NotificationRecipientInt} object representing the recipient of the notification,
     *                          including their tax ID and other relevant details.
     * @param quickAccessToken  a {@link String} representing the token used to generate the quick access URL
     *                          for the notification details.
     * @return a {@link String} representing the PEC email body for the AAR of the notification.
     * @throws IllegalArgumentException if any required parameter is null or contains invalid data.
     *
     * <p><strong>Note:</strong></p>
     * Ensure that {@code templatesClient} is properly configured to handle the generated
     * {@link NotificationAarForPec} object and return the expected PEC email body string.
     */
    @Override
    public String generateNotificationAARPECBody(NotificationInt notification,
                                                 NotificationRecipientInt recipient,
                                                 String quickAccessToken) {
        log.info("retrieve NotificationAARPECBody template for iun {}", notification.getIun());
        String qrCodeQuickAccessUrlAarDetail = this.getQrCodeQuickAccessUrlAarDetail(recipient, quickAccessToken);

        AarForPecSender sender = new AarForPecSender()
                .paDenomination(notification.getSender().getPaDenomination());

        AarForPecNotification pecNotification = new AarForPecNotification()
                .iun(notification.getIun())
                .subject(notification.getSubject())
                .sender(sender);

        AarForPecRecipient aarForPecRecipient = new AarForPecRecipient()
                .taxId(recipient.getTaxId());

        NotificationAarForPec notificationAAR = new NotificationAarForPec()
                .perfezionamentoURL(this.getPerfezionamentoLink())
                .quickAccessLink(qrCodeQuickAccessUrlAarDetail)
                .pnFaqSendURL(this.getFAQSendURL())
                .piattaformaNotificheURL(this.getAccessUrl(recipient))
                .notification(pecNotification)
                .recipient(aarForPecRecipient)
                .recipientType(this.getRecipientTypeForHTMLTemplate(recipient));

        LanguageEnum language = getLanguage(notification.getAdditionalLanguages());
        return templatesClient.notificationAarForPec(language, notificationAAR);
    }

    /**
     * Generates the AAR for an SMS notification.
     *
     * @param notification the {@link NotificationInt} object containing details about the notification,
     *                     including its unique identifier (IUN) and sender information.
     * @return a {@link String} representing the SMS body for the AAR of the notification.
     * @throws IllegalArgumentException if any required parameter is null or contains invalid data.
     *
     * <p><strong>Note:</strong></p>
     * Ensure that {@code templatesClient} is properly configured to handle the generated
     * {@link NotificationAarForSms} object and return the expected SMS body string.
     */
    @Override
    public String generateNotificationAARForSMS(NotificationInt notification) {
        log.info("retrieve NotificationAARForSMS template for iun {}", notification.getIun());
        AarForSmsSender sender = new AarForSmsSender()
                .paDenomination(notification.getSender().getPaDenomination());

        AarForSmsNotification aarForSmsNotification = new AarForSmsNotification()
                .iun(notification.getIun())
                .sender(sender);

        NotificationAarForSms notificationAARForSMS = new NotificationAarForSms()
                .notification(aarForSmsNotification);

        LanguageEnum language = getLanguage(notification.getAdditionalLanguages());
        return templatesClient.notificationAarForSms(language, notificationAARForSMS);
    }

    /**
     * Retrieves the label for the access URL associated with a notification recipient.
     *
     * @param recipient the {@link NotificationRecipientInt} object representing the recipient of the notification,
     *                  used to retrieve the access URL.
     * @return a {@link String} representing the label of the access URL, typically the host without the "www." prefix.
     *         If the URL is invalid, returns the full access URL.
     */
    private String getAccessUrlLabel(NotificationRecipientInt recipient) {
        try {
            String host = new URL(getAccessUrl(recipient)).getHost();
            return host.startsWith("www.") ? host.substring(4) : host;
        } catch (MalformedURLException e) {
            log.warn("cannot get host", e);
            return getAccessUrl(recipient);
        }
    }

    /**
     * Generates a Base64-encoded QR code image for quick access, using the provided recipient and access token.
     *
     * @param recipient the {@link NotificationRecipientInt} object representing the recipient of the notification,
     *                  used to generate the quick access URL.
     * @param quickAccessToken the token used to generate the quick access URL, ensuring secure access to the resource.
     * @return a {@link String} representing the Base64-encoded QR code image in a data URI format.
     */
    private String getQrCodeQuickAccessUrlAarDetail(NotificationRecipientInt recipient, String quickAccessToken) {
        String url = getQuickAccessLink(recipient, quickAccessToken);
        // Definire altezza e larghezza del qrcode
        return "data:image/png;base64, " .concat(Base64Utils.encodeToString(QrCodeUtils.generateQRCodeImage(url, 180, 180,
                pnDeliveryPushConfigs.getErrorCorrectionLevelQrCode())));
    }

    /**
     * Generates a quick access link URL for a given recipient, appending the quick access token to the base URL.
     *
     * @param recipient the {@link NotificationRecipientInt} object representing the recipient of the notification,
     *                  used to retrieve the base access URL.
     * @param quickAccessToken the token used to generate the quick access link, typically used for secure access.
     * @return a {@link String} representing the full quick access URL, including the token as a query parameter.
     */
    private String getQuickAccessLink(NotificationRecipientInt recipient, String quickAccessToken) {
        String templateUrl = getAccessUrl(recipient) + pnDeliveryPushConfigs.getWebapp().getQuickAccessUrlAarDetailSuffix();
        log.debug("getQrCodeQuickAccessUrlAarDetail templateUrl {} quickAccessLink {}", templateUrl, quickAccessToken);
        return templateUrl + '=' + quickAccessToken;
    }

    /**
     * Generates a link to the "perfezionamento" page of the web application.
     *
     * @return a {@link String} representing the complete URL for the "perfezionamento" page.
     */
    private String getPerfezionamentoLink() {
        return pnDeliveryPushConfigs.getWebapp().getLandingUrl() + "perfezionamento";
    }

    /**
     * Generates the label for the "perfezionamento" link by appending the "perfezionamento" path
     * to the base access link label.
     *
     * @return a {@link String} representing the complete label for the "perfezionamento" link.
     */
    private String getPerfezionamentoLinkLabel() {
        return this.getAccessLinkLabel() + "/perfezionamento";
    }

    private String getAccessLink() {
        return pnDeliveryPushConfigs.getWebapp().getLandingUrl();
    }

    /**
     * Retrieves the host name from the base landing URL configured in the application settings,
     * and removes the "www." prefix if it exists.
     *
     * @return a {@link String} representing the host name from the landing URL, with "www." removed if present.
     */
    private String getAccessLinkLabel() {
        try {
            String host = new URL(pnDeliveryPushConfigs.getWebapp().getLandingUrl()).getHost();
            return host.startsWith("www.") ? host.substring(4) : host;
        } catch (MalformedURLException e) {
            log.warn("cannot get host", e);
            return pnDeliveryPushConfigs.getWebapp().getLandingUrl();
        }
    }

    private String getFAQAccessLink() {
        return pnDeliveryPushConfigs.getWebapp().getLandingUrl() + pnDeliveryPushConfigs.getWebapp().getFaqUrlTemplateSuffix();
    }

    private String getFAQSendURL() {
        return this.getFAQAccessLink() + "#" + pnDeliveryPushConfigs.getWebapp().getFaqSendHash();
    }


    /**
     * Determines the recipient type for an HTML template based on the recipient's type.
     *
     * @param recipientInt the recipient object containing the recipient's type.
     * @return a {@link String} representing the recipient type for use in an HTML template,
     * either "giuridica" or "fisica".
     */
    private String getRecipientTypeForHTMLTemplate(NotificationRecipientInt recipientInt) {
        return recipientInt.getRecipientType() == RecipientTypeInt.PG ? "giuridica" : "fisica";
    }

    /**
     * Returns the appropriate access URL for a recipient based on their type.
     *
     * @param recipient the recipient object containing information about the recipient's type.
     * @return a {@link String} representing the access URL based on the recipient's type.
     */
    private String getAccessUrl(NotificationRecipientInt recipient) {
        return RecipientTypeInt.PF == recipient.getRecipientType()
                ? pnDeliveryPushConfigs.getWebapp().getDirectAccessUrlTemplatePhysical()
                : pnDeliveryPushConfigs.getWebapp().getDirectAccessUrlTemplateLegal();
    }

    /**
     * Constructs a {@link NotificationAar} object for an Acknowledgment of Receipt (AAR) notification.
     *
     * @param notification the {@link NotificationInt} object containing the details about the notification,
     *                     including its unique identifier (IUN), subject, and sender information.
     * @param recipient    the {@link NotificationRecipientInt} object representing the recipient of the notification,
     *                     including their tax ID and recipient type.
     * @param quickAccessToken a {@link String} representing the token used to generate the quick access QR code link
     *                         for the notification details.
     * @return a {@link NotificationAar} object containing all the necessary information for the AAR notification.
     * @throws IllegalArgumentException if any required parameter is null or contains invalid data.
     */
    private NotificationAar getNotificationAAR(NotificationInt notification,
                                               NotificationRecipientInt recipient,
                                               String quickAccessToken) {
        String qrCodeQuickAccessUrlAarDetail = this.getQrCodeQuickAccessUrlAarDetail(recipient, quickAccessToken);

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
                .piattaformaNotificheURL(this.getAccessUrl(recipient))
                .piattaformaNotificheURLLabel(this.getAccessUrlLabel(recipient))
                .perfezionamentoURL(this.getPerfezionamentoLink())
                .perfezionamentoURLLabel(this.getPerfezionamentoLinkLabel())
                .qrCodeQuickAccessLink(qrCodeQuickAccessUrlAarDetail);
    }

    /**
     * Constructs a {@link NotificationAarRaddAlt} object for an AAR notification
     * with alternative details.
     *
     * @param notification the {@link NotificationInt} object containing details about the notification,
     *                     including its unique identifier (IUN), subject, and sender information.
     * @param recipient    the {@link NotificationRecipientInt} object representing the recipient of the notification,
     *                     including their tax ID and recipient type.
     * @param quickAccessToken a {@link String} representing the token used to generate the quick access QR code
     *                         link for the notification details.
     * @return a {@link NotificationAarRaddAlt} object containing all the necessary information for the AAR
     *         notification with alternative details.
     * @throws IllegalArgumentException if any required parameter is null or contains invalid data.
     */
    private NotificationAarRaddAlt getNotificationAARRADDalt(NotificationInt notification,
                                                             NotificationRecipientInt recipient,
                                                             String quickAccessToken) {
        String qrCodeQuickAccessUrlAarDetail = this.getQrCodeQuickAccessUrlAarDetail(recipient, quickAccessToken);

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
                .piattaformaNotificheURL(this.getAccessUrl(recipient))
                .piattaformaNotificheURLLabel(this.getAccessUrlLabel(recipient))
                .sendURL(this.getAccessLink())
                .sendURLLAbel(this.getAccessLinkLabel())
                .perfezionamentoURL(this.getPerfezionamentoLink())
                .perfezionamentoURLLabel(this.getPerfezionamentoLinkLabel())
                .qrCodeQuickAccessLink(qrCodeQuickAccessUrlAarDetail)
                .raddPhoneNumber(pnDeliveryPushConfigs.getWebapp().getRaddPhoneNumber());
    }

    /**
     * Determines the language to be used for the notification based on the provided list of additional languages.
     *
     * @param additionalLanguages a {@link List} of {@link String} representing the additional languages to be considered.
     *                            If the list is empty or null, the default language (Italian) is returned.
     * @return a {@link LanguageEnum} representing the selected language. It returns {@link LanguageEnum#IT}
     *         if no additional languages are available or enabled, otherwise the first language from the list.
     * @throws IllegalArgumentException if the provided list contains invalid language values.
     */
    private LanguageEnum getLanguage(List<String> additionalLanguages) {
        return (!pnDeliveryPushConfigs.isAdditionalLangsEnabled() || CollectionUtils.isEmpty(additionalLanguages))
                ? LanguageEnum.IT : LanguageEnum.fromValue(additionalLanguages.get(0));
    }

    private NotificationViewedDelegate notificationViewedDelegate(DelegateInfoInt delegateInfo) {
        return delegateInfo != null ?
                new NotificationViewedDelegate()
                        .denomination(delegateInfo.getDenomination())
                        .taxId(delegateInfo.getTaxId())
                : null;
    }
}

