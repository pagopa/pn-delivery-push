package it.pagopa.pn.deliverypush.legalfacts;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.utils.FileUtils;
import it.pagopa.pn.deliverypush.action.utils.EndWorkflowStatus;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressSourceInt;
import it.pagopa.pn.deliverypush.dto.ext.datavault.RecipientTypeInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationDocumentInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationPaymentInfoInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
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

        String physicalAddressAndDenomination;
        List<NotificationRecipientInt> recipients = Optional.of(notification)
                .map(NotificationInt::getRecipients)
                .orElse(new ArrayList<>());

        List<NotificationReceivedRecipient> receivedRecipients = new ArrayList<>();
        for (var recipientInt : recipients) {
            String denomination = recipientInt.getDenomination();
            physicalAddressAndDenomination = physicalAddressWriter.nullSafePhysicalAddressToString(
                    recipientInt.getPhysicalAddress(), denomination, "'<br />'");

            NotificationReceivedDigitalDomicile digitalDomicile = new NotificationReceivedDigitalDomicile()
                    .address(recipientInt.getDigitalDomicile().getAddress());

            NotificationReceivedRecipient notificationReceivedRecipient = new NotificationReceivedRecipient()
                    .physicalAddressAndDenomination(physicalAddressAndDenomination)
                    .denomination(recipientInt.getDenomination())
                    .taxId(recipientInt.getTaxId())
                    .digitalDomicile(digitalDomicile);

            receivedRecipients.add(notificationReceivedRecipient);
        }

        NotificationReceivedSender sender = new NotificationReceivedSender()
                .paDenomination(notification.getSender().getPaDenomination())
                .paTaxId(notification.getSender().getPaTaxId());

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
        return convertFileMonoToBytes(templateEngineClient.notificationReceivedLegalFact(language, legalFact));
    }

    @Override
    public byte[] generateNotificationViewedLegalFact(String iun,
                                                      NotificationRecipientInt recipient,
                                                      DelegateInfoInt delegateInfo,
                                                      Instant timeStamp,
                                                      NotificationInt notification) {

        NotificationViewedRecipient notificationViewedRecipient = new NotificationViewedRecipient()
                .denomination(recipient.getDenomination())
                .taxId(recipient.getTaxId());

        NotificationViewedDelegate delegate = new NotificationViewedDelegate()
                .denomination(delegateInfo.getDenomination())
                .taxId(delegateInfo.getTaxId());

        NotificationViewedLegalFact notificationViewedLegalFact = new NotificationViewedLegalFact()
                .recipient(notificationViewedRecipient)
                .iun(iun)
                .delegate(delegate)
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
        return convertFileMonoToBytes(templateEngineClient.pecDeliveryWorkflowLegalFact(language, pecDeliveryWorkflowLegalFact));
    }

    @Override
    public byte[] generateAnalogDeliveryFailureWorkflowLegalFact(NotificationInt notification,
                                                                 NotificationRecipientInt recipient,
                                                                 EndWorkflowStatus status,
                                                                 Instant failureWorkflowDate) {

        AnalogDeliveryWorkflowFailureRecipient analogDeliveryWorkflowFailureRecipient = new AnalogDeliveryWorkflowFailureRecipient()
                .denomination(recipient.getDenomination())
                .taxId(recipient.getTaxId());

        AnalogDeliveryWorkflowFailureLegalFact analogDeliveryWorkflowFailureLegalFact = new AnalogDeliveryWorkflowFailureLegalFact()
                .iun(notification.getIun())
                .recipient(analogDeliveryWorkflowFailureRecipient)
                .endWorkflowDate(instantWriter.instantToDate(failureWorkflowDate, true))
                .endWorkflowTime(instantWriter.instantToTime(failureWorkflowDate));

        LanguageEnum language = getLanguage(notification.getAdditionalLanguages());
        return convertFileMonoToBytes(templateEngineClient.analogDeliveryWorkflowFailureLegalFact(language, analogDeliveryWorkflowFailureLegalFact));
    }

    @Override
    public byte[] generateNotificationCancelledLegalFact(NotificationInt notification, Instant notificationCancellationRequestDate) {

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
        return convertFileMonoToBytes(templateEngineClient.notificationCancelledLegalFact(language, cancelledLegalFact));
    }

    @Override
    public String generateNotificationAARSubject(NotificationInt notification) {
        AarForSubjectSender sender = new AarForSubjectSender()
                .paDenomination(notification.getSender().getPaDenomination());

        AarForSubjectNotification aarForSubjectNotification = new AarForSubjectNotification()
                .sender(sender);

        NotificationAarForSubject notificationAARSubject = new NotificationAarForSubject()
                .notification(aarForSubjectNotification);

        LanguageEnum language = getLanguage(notification.getAdditionalLanguages());
        return templateEngineClient.notificationAarForSubject(language, notificationAARSubject);
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
                    NotificationAar notificationAAR = getNotificationAAR(notification, recipient, quickAccessToken);
                    bytesArrayGeneratedAar = convertFileMonoToBytes(templateEngineClient.notificationAar(language, notificationAAR));
                }
                case AAR_NOTIFICATION_RADD_ALT -> {
                    NotificationAarRaddAlt notificationAARRADDalt = getNotificationAARRADDalt(notification, recipient, quickAccessToken);
                    bytesArrayGeneratedAar = convertFileMonoToBytes(templateEngineClient.notificationAarRaddAlt(language, notificationAARRADDalt));
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

        NotificationAarForEmail notificationAAR = new NotificationAarForEmail()
                .perfezionamentoURL(this.getPerfezionamentoLink())
                .quickAccessLink(qrCodeQuickAccessUrlAarDetail)
                .pnFaqSendURL(this.getFAQSendURL())
                .piattaformaNotificheURL(this.getAccessUrl(recipient));

        LanguageEnum language = getLanguage(notification.getAdditionalLanguages());
        return templateEngineClient.notificationAarForEmail(language, notificationAAR);
    }

    @Override
    public String generateNotificationAARPECBody(NotificationInt notification,
                                                 NotificationRecipientInt recipient,
                                                 String quickAccessToken) {
        String qrCodeQuickAccessUrlAarDetail = this.getQrCodeQuickAccessUrlAarDetail(recipient, quickAccessToken);

        AarForPecSender sender = new AarForPecSender()
                .paDenomination(notification.getSender().getPaDenomination());

        AarForPecNotification pecNotification = new AarForPecNotification()
                .iun(notification.getIun())
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
        return templateEngineClient.notificationAarForPec(language, notificationAAR);
    }

    @Override
    public String generateNotificationAARForSMS(NotificationInt notification) {
        AarForSmsSender sender = new AarForSmsSender()
                .paDenomination(notification.getSender().getPaDenomination());

        AarForSmsNotification aarForSmsNotification = new AarForSmsNotification()
                .iun(notification.getIun())
                .sender(sender);

        NotificationAarForSms notificationAARForSMS = new NotificationAarForSms()
                .notification(aarForSmsNotification);

        LanguageEnum language = getLanguage(notification.getAdditionalLanguages());
        return templateEngineClient.notificationAarForSms(language, notificationAARForSMS);
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
                .taxId(recipient.getTaxId());

        return new NotificationAarRaddAlt()
                .notification(altNotification)
                .recipient(aarRecipient)
                .piattaformaNotificheURL(this.getAccessUrl(recipient))
                .piattaformaNotificheURLLabel(this.getAccessUrlLabel(recipient))
                .sendURL(this.getAccessLink())
                .sendURLLAbel(this.getAccessLinkLabel())
                .perfezionamentoURL(this.getPerfezionamentoLink())
                .perfezionamentoURLLabel(this.getPerfezionamentoLinkLabel())
                .qrCodeQuickAccessLink(qrCodeQuickAccessUrlAarDetail);
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

