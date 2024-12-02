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
import it.pagopa.pn.deliverypush.exceptions.PnReadFileException;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.templatesengine.api.TemplateApi;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.templatesengine.model.*;
import it.pagopa.pn.deliverypush.utils.PnSendMode;
import it.pagopa.pn.deliverypush.utils.PnSendModeUtils;
import it.pagopa.pn.deliverypush.utils.QrCodeUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Base64Utils;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_CONFIGURATION_NOT_FOUND;
import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_INVALID_TEMPLATE;

@Slf4j
@AllArgsConstructor
public class LegalFactGeneratorTemplatesClient implements LegalFactGenerator {

    private final CustomInstantWriter instantWriter;
    private final PhysicalAddressWriter physicalAddressWriter;
    private final PnDeliveryPushConfigs pnDeliveryPushConfigs;
    private final PnSendModeUtils pnSendModeUtils;
    private final TemplateApi templateEngineClient;

    @Override
    public byte[] generateNotificationReceivedLegalFact(NotificationInt notification) {
        String physicalAddressAndDenomination = null;
        List<NotificationRecipientInt> recipients = Optional.of(notification)
                .map(NotificationInt::getRecipients)
                .orElse(new ArrayList<>());

        for (var recipientInt : recipients) {
            String denomination = recipientInt.getDenomination();
            physicalAddressAndDenomination = physicalAddressWriter.nullSafePhysicalAddressToString(
                    recipientInt.getPhysicalAddress(), denomination, "'<br />'");
        }

        NotificationReceiverLegalFact legalFact = new NotificationReceiverLegalFact()
                .sendDate(instantWriter.instantToDate(notification.getSentAt()))
                .subject(notification.getSubject())
                .notification(notificationTemplate(notification))
                .digests(extractNotificationAttachmentDigests(notification))
                .physicalAddressAndDenomination(physicalAddressAndDenomination);

        LanguageEnum language = getLanguage(notification.getAdditionalLanguages());
        return convertFileMonoToBytes(templateEngineClient.notificationReceivedLegalFact(language, legalFact));
    }

    @Override
    public byte[] generateNotificationViewedLegalFact(String iun,
                                                      NotificationRecipientInt recipient,
                                                      DelegateInfoInt delegateInfo,
                                                      Instant timeStamp,
                                                      NotificationInt notification) {
        NotificationViewedLegalFact notificationViewedLegalFact = new NotificationViewedLegalFact()
                .iun(iun)
                .recipient(recipientTemplate(recipient))
                .delegate(delegateTemplate(delegateInfo))
                .when(instantWriter.instantToDate(timeStamp));
        LanguageEnum language = getLanguage(notification.getAdditionalLanguages());
        return convertFileMonoToBytes(templateEngineClient.notificationViewedLegalFact(language, notificationViewedLegalFact));
    }

    @Override
    public byte[] generatePecDeliveryWorkflowLegalFact(List<SendDigitalFeedbackDetailsInt> feedbackFromExtChannelList,
                                                       NotificationInt notification,
                                                       NotificationRecipientInt recipient,
                                                       EndWorkflowStatus status,
                                                       Instant completionWorkflowDate) {
        List<Delivery> pecDeliveries = feedbackFromExtChannelList.stream()
                .map(feedbackFromExtChannel -> {
                    ResponseStatusInt sentPecStatus = feedbackFromExtChannel.getResponseStatus();
                    Instant notificationDate = feedbackFromExtChannel.getNotificationDate();
                    String addressSource = Optional.ofNullable(feedbackFromExtChannel.getDigitalAddressSource())
                            .map(DigitalAddressSourceInt::getValue)
                            .orElse(null);
                    return new Delivery()
                            .denomination(recipient.getDenomination())
                            .taxId(recipient.getTaxId())
                            .address(feedbackFromExtChannel.getDigitalAddress().getAddress())
                            .addressSource(addressSource)
                            .type(feedbackFromExtChannel.getDigitalAddress().getType().getValue())
                            .responseDate(instantWriter.instantToDate(notificationDate))
                            .ok(ResponseStatusInt.OK.equals(sentPecStatus));
                })
                .sorted(Comparator.comparing(Delivery::getResponseDate))
                .toList();

        PecDeliveryWorkflowLegalFact pecDeliveryWorkflowLegalFact = new PecDeliveryWorkflowLegalFact()
                .deliveries(pecDeliveries)
                .iun(notification.getIun())
                .endWorkflowStatus(status.toString())
                .endWorkflowDate(instantWriter.instantToDate(completionWorkflowDate));
        LanguageEnum language = getLanguage(notification.getAdditionalLanguages());
        return convertFileMonoToBytes(templateEngineClient.pecDeliveryWorkflowLegalFact(language, pecDeliveryWorkflowLegalFact));
    }

    @Override
    public byte[] generateAnalogDeliveryFailureWorkflowLegalFact(NotificationInt notification,
                                                                 NotificationRecipientInt recipient,
                                                                 EndWorkflowStatus status,
                                                                 Instant failureWorkflowDate) {
        AnalogDeliveryWorkflowFailureLegalFact analogDeliveryWorkflowFailureLegalFact = new AnalogDeliveryWorkflowFailureLegalFact()
                .iun(notification.getIun())
                .endWorkflowDate(instantWriter.instantToDate(failureWorkflowDate, true))
                .endWorkflowTime(instantWriter.instantToTime(failureWorkflowDate))
                .recipient(recipientTemplate(recipient));
        LanguageEnum language = getLanguage(notification.getAdditionalLanguages());
        return convertFileMonoToBytes(templateEngineClient.analogDeliveryWorkflowFailureLegalFact(language, analogDeliveryWorkflowFailureLegalFact));
    }

    @Override
    public byte[] generateNotificationCancelledLegalFact(NotificationInt notification, Instant notificationCancellationRequestDate) {
        NotificationCancelledLegalFact cancelledLegalFact = new NotificationCancelledLegalFact()
                .notification(notificationTemplate(notification))
                .notificationCancelledDate(instantWriter.instantToDate(notificationCancellationRequestDate));
        LanguageEnum language = getLanguage(notification.getAdditionalLanguages());
        return convertFileMonoToBytes(templateEngineClient.notificationCancelledLegalFact(language, cancelledLegalFact));
    }

    @Override
    public String generateNotificationAARSubject(NotificationInt notification) {
        NotificationAARSubject notificationAARSubject = new NotificationAARSubject()
                .notification(notificationTemplate(notification));
        LanguageEnum language = getLanguage(notification.getAdditionalLanguages());
        return templateEngineClient.notificationAARSubject(language, notificationAARSubject);
    }

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

    @Override
    public AARInfo generateNotificationAAR(NotificationInt notification, NotificationRecipientInt recipient, String quickAccessToken) throws IOException {
        LanguageEnum language = getLanguage(notification.getAdditionalLanguages());
        PnSendMode pnSendMode = pnSendModeUtils.getPnSendMode(notification.getSentAt());
        if (pnSendMode != null) {
            final AarTemplateChooseStrategy aarTemplateTypeChooseStrategy = pnSendMode.getAarTemplateTypeChooseStrategy();
            final AarTemplateType aarTemplateType = aarTemplateTypeChooseStrategy.choose(recipient.getPhysicalAddress());
            log.debug("aarTemplateType generated is ={} - iun={}", aarTemplateType, notification.getIun());
            byte[] bytesArrayGeneratedAar = new byte[0];
            switch (aarTemplateType) {
                case AAR_NOTIFICATION -> {
                    NotificationAAR notificationAAR = getNotificationAAR(notification, recipient, quickAccessToken);
                    bytesArrayGeneratedAar = convertFileMonoToBytes(templateEngineClient.notificationAAR(language, notificationAAR));
                }
                case AAR_NOTIFICATION_RADD_ALT -> {
                    NotificationAARRADDalt notificationAARRADDalt = getNotificationAARRADDalt(notification, recipient, quickAccessToken);
                    bytesArrayGeneratedAar = convertFileMonoToBytes(templateEngineClient.notificationAARRADDalt(language, notificationAARRADDalt));
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

    @Override
    public String generateNotificationAARBody(NotificationInt notification, NotificationRecipientInt recipient, String quickAccessToken) {
        String qrCodeQuickAccessUrlAarDetail = this.getQrCodeQuickAccessUrlAarDetail(recipient, quickAccessToken);
        NotificationAARForEMAIL notificationAAR = new NotificationAARForEMAIL()
                .perfezionamentoURL(this.getPerfezionamentoLink())
                .quickAccessLink(qrCodeQuickAccessUrlAarDetail)
                .pnFaqSendURL(this.getFAQSendURL())
                .piattaformaNotificheURL(this.getAccessUrl(recipient))
                .notification(notificationTemplate(notification));
        LanguageEnum language = getLanguage(notification.getAdditionalLanguages());
        return templateEngineClient.notificationAARForEMAIL(language, notificationAAR);
    }

    @Override
    public String generateNotificationAARPECBody(NotificationInt notification, NotificationRecipientInt recipient, String quickAccessToken) {
        String qrCodeQuickAccessUrlAarDetail = this.getQrCodeQuickAccessUrlAarDetail(recipient, quickAccessToken);
        NotificationAARForPEC notificationAAR = new NotificationAARForPEC()
                .perfezionamentoURL(this.getPerfezionamentoLink())
                .quickAccessLink(qrCodeQuickAccessUrlAarDetail)
                .pnFaqSendURL(this.getFAQSendURL())
                .piattaformaNotificheURL(this.getAccessUrl(recipient))
                .notification(notificationTemplate(notification))
                .recipient(recipientTemplate(recipient))
                .recipientType(this.getRecipientTypeForHTMLTemplate(recipient));
        LanguageEnum language = getLanguage(notification.getAdditionalLanguages());
        return templateEngineClient.notificationAARForPEC(language, notificationAAR);
    }

    @Override
    public String generateNotificationAARForSMS(NotificationInt notification) {
        NotificationAARForSMS notificationAARForSMS = new NotificationAARForSMS()
                .notification(notificationTemplate(notification));
        LanguageEnum language = getLanguage(notification.getAdditionalLanguages());
        return templateEngineClient.notificationAARForSMS(language, notificationAARForSMS);
    }

    private String getAccessUrlLabel(NotificationRecipientInt recipient) {
        try {
            String host = new URL(getAccessUrl(recipient)).getHost();
            return host.startsWith("www.") ? host.substring(4) : host;
        } catch (MalformedURLException e) {
            log.warn("cannot get host", e);
            return getAccessUrl(recipient);
        }
    }

    private String getQrCodeQuickAccessUrlAarDetail(NotificationRecipientInt recipient, String quickAccessToken) {
        String url = getQuickAccessLink(recipient, quickAccessToken);
        // Definire altezza e larghezza del qrcode
        return "data:image/png;base64, ".concat(Base64Utils.encodeToString(QrCodeUtils.generateQRCodeImage(url, 180, 180,
                pnDeliveryPushConfigs.getErrorCorrectionLevelQrCode())));
    }

    private String getQuickAccessLink(NotificationRecipientInt recipient, String quickAccessToken) {
        String templateUrl = getAccessUrl(recipient) + pnDeliveryPushConfigs.getWebapp().getQuickAccessUrlAarDetailSuffix();
        log.debug("getQrCodeQuickAccessUrlAarDetail templateUrl {} quickAccessLink {}", templateUrl, quickAccessToken);
        return templateUrl + '=' + quickAccessToken;
    }

    private String getPerfezionamentoLink() {
        return pnDeliveryPushConfigs.getWebapp().getLandingUrl() + "perfezionamento";
    }

    private String getPerfezionamentoLinkLabel() {
        return this.getAccessLinkLabel() + "/perfezionamento";
    }

    private String getAccessLink() {
        return pnDeliveryPushConfigs.getWebapp().getLandingUrl();
    }

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


    private String getRecipientTypeForHTMLTemplate(NotificationRecipientInt recipientInt) {
        return recipientInt.getRecipientType() == RecipientTypeInt.PG ? "giuridica" : "fisica";
    }

    private String getAccessUrl(NotificationRecipientInt recipient) {
        return RecipientTypeInt.PF == recipient.getRecipientType()
                ? pnDeliveryPushConfigs.getWebapp().getDirectAccessUrlTemplatePhysical()
                : pnDeliveryPushConfigs.getWebapp().getDirectAccessUrlTemplateLegal();
    }

    private NotificationAAR getNotificationAAR(NotificationInt notification,
                                               NotificationRecipientInt recipient, String quickAccessToken) {
        String qrCodeQuickAccessUrlAarDetail = this.getQrCodeQuickAccessUrlAarDetail(recipient, quickAccessToken);
        return new NotificationAAR()
                .notification(notificationTemplate(notification))
                .recipient(recipientTemplate(recipient))
                .piattaformaNotificheURL(this.getAccessUrl(recipient))
                .piattaformaNotificheURLLabel(this.getAccessUrlLabel(recipient))
                .sendURL(this.getAccessLink())
                .sendURLLAbel(this.getAccessLinkLabel())
                .perfezionamentoURL(this.getPerfezionamentoLink())
                .perfezionamentoURLLabel(this.getPerfezionamentoLinkLabel())
                .qrCodeQuickAccessLink(qrCodeQuickAccessUrlAarDetail);
    }

    private NotificationAARRADDalt getNotificationAARRADDalt(NotificationInt notification,
                                                             NotificationRecipientInt recipient, String quickAccessToken) {
        String qrCodeQuickAccessUrlAarDetail = this.getQrCodeQuickAccessUrlAarDetail(recipient, quickAccessToken);
        return new NotificationAARRADDalt()
                .notification(notificationTemplate(notification))
                .recipient(recipientTemplate(recipient))
                .piattaformaNotificheURL(this.getAccessUrl(recipient))
                .piattaformaNotificheURLLabel(this.getAccessUrlLabel(recipient))
                .sendURL(this.getAccessLink())
                .sendURLLAbel(this.getAccessLinkLabel())
                .perfezionamentoURL(this.getPerfezionamentoLink())
                .perfezionamentoURLLabel(this.getPerfezionamentoLinkLabel())
                .qrCodeQuickAccessLink(qrCodeQuickAccessUrlAarDetail);
    }

    private Notification notificationTemplate(NotificationInt notification) {
        var sender = Optional.ofNullable(notification).map(NotificationInt::getSender).orElse(new NotificationSenderInt());
        return new Notification()
                .iun(notification.getIun())
                .subject(notification.getSubject())
                .recipients(recipientsTemplate(notification.getRecipients()))
                .sender(notificationSenderTemplate(sender));
    }

    private NotificationSender notificationSenderTemplate(NotificationSenderInt sender) {
        return new NotificationSender()
                .paDenomination(sender.getPaDenomination())
                .paTaxId(sender.getPaTaxId())
                .paId(sender.getPaId());
    }

    private List<Recipient> recipientsTemplate(List<NotificationRecipientInt> recipients) {
        return recipients.stream().map(this::recipientTemplate).toList();
    }

    private Recipient recipientTemplate(NotificationRecipientInt recipient) {
        DigitalDomicile digitalDomicile = Optional.ofNullable(recipient.getDigitalDomicile())
                .map(DigitalAddressInt::getAddress)
                .map(address -> new DigitalDomicile().address(address)).orElse(null);
        String recipientType = Optional.ofNullable(recipient.getRecipientType()).map(RecipientTypeInt::getValue).orElse(null);
        return new Recipient()
                .denomination(recipient.getDenomination())
                .recipientType(recipientType)
                .taxId(recipient.getTaxId())
                .digitalDomicile(digitalDomicile);
    }

    private Delegate delegateTemplate(DelegateInfoInt delegateInfo) {
        return new Delegate()
                .denomination(delegateInfo.getDenomination())
                .taxId(delegateInfo.getTaxId());
    }

    public byte[] convertFileMonoToBytes(File fileResponse) {
        try {
            return Files.readAllBytes(fileResponse.toPath());
        } catch (IOException e) {
            throw new PnReadFileException("Failed to convert to byte[]", e);
        }
    }

    private LanguageEnum getLanguage(List<String> additionalLanguages) {
        return (!pnDeliveryPushConfigs.isAdditionalLangsEnabled() || CollectionUtils.isEmpty(additionalLanguages))
                ? LanguageEnum.IT : LanguageEnum.fromValue(additionalLanguages.get(0));
    }
}

