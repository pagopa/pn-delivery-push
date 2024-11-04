package it.pagopa.pn.deliverypush.legalfacts;

import com.amazonaws.util.IOUtils;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.utils.FileUtils;
import it.pagopa.pn.deliverypush.action.utils.EndWorkflowStatus;
import it.pagopa.pn.deliverypush.action.utils.InstantNowSupplier;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.ext.datavault.RecipientTypeInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationDocumentInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationPaymentInfoInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.ResponseStatusInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.AARInfo;
import it.pagopa.pn.deliverypush.dto.mandate.DelegateInfoInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendDigitalFeedbackDetailsInt;
import it.pagopa.pn.deliverypush.exceptions.PnInvalidTemplateException;
import it.pagopa.pn.deliverypush.exceptions.PnReadFileException;
import it.pagopa.pn.deliverypush.utils.PnSendMode;
import it.pagopa.pn.deliverypush.utils.PnSendModeUtils;
import it.pagopa.pn.deliverypush.utils.QrCodeUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.Base64Utils;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_CONFIGURATION_NOT_FOUND;
import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_INVALID_TEMPLATE;
import static it.pagopa.pn.deliverypush.legalfacts.DocumentComposition.TemplateType.AAR_NOTIFICATION_RADD;

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
    public static final String FIELD_SENDER_ADDRESS = "senderAddress";
    public static final String FIELD_WHEN = "when";
    public static final String FIELD_PIATTAFORMA_NOTIFICHE_URL = "piattaformaNotificheURL";
    public static final String FIELD_PIATTAFORMA_NOTIFICHE_URL_LABEL = "piattaformaNotificheURLLabel";
    public static final String FIELD_PN_FAQ_COMPLETION_MOMENT_URL = "PNFaqCompletionMomentURL";
    public static final String FIELD_PN_FAQ_COMPLETION_MOMENT_URL_LABEL = "PNFaqCompletionMomentURLLabel";
    public static final String FIELD_SEND_URL = "PNFaqSendURL";
    public static final String FIELD_END_WORKFLOW_STATUS = "endWorkflowStatus";
    public static final String FIELD_END_WORKFLOW_DATE = "endWorkflowDate";
    public static final String FIELD_END_WORKFLOW_TIME = "endWorkflowTime";
    public static final String FIELD_LEGALFACT_CREATION_DATE = "legalFactCreationDate";
    public static final String FIELD_QRCODE_QUICK_ACCESS_LINK = "qrCodeQuickAccessLink";
    public static final String FIELD_QUICK_ACCESS_LINK = "quickAccessLink";
    public static final String FIELD_RECIPIENT_TYPE = "recipientType";
    public static final String FIELD_DELEGATE = "delegate";
    public static final String FIELD_PERFEZIONAMENTO = "perfezionamentoURL";
    public static final String FIELD_PERFEZIONAMENTO_LABEL = "perfezionamentoURLLabel";
    public static final String FIELD_LOGO_LINK = "sendLogoLink";

    public static final String FIELD_SENDURL = "sendURL";
    public static final String FIELD_SENDURL_LABEL = "sendURLLAbel";
    public static final String FIELD_LOGO = "logoBase64";
    private static final String FIELD_ADDITIONAL = "additional_";
    private static final String FIELD_DISCLAIMER = "disclaimer";
    public static final String FIELD_SUBJECT = "subject";
    private static final String FIELD_RADDPHONENUMBER = "raddPhoneNumber";
    public static final String FIELD_NOTIFICATION_CANCELLED_DATE = "notificationCancelledDate";
    private final DocumentComposition documentComposition;
    private final CustomInstantWriter instantWriter;
    private final PhysicalAddressWriter physicalAddressWriter;
    private final PnDeliveryPushConfigs pnDeliveryPushConfigs;
    private final InstantNowSupplier instantNowSupplier;
    private final PnSendModeUtils pnSendModeUtils;
    private static final String TEMPLATES_DIR_NAME = "documents_composition_templates";

    private static final String SEND_LOGO_BASE64 =  readLocalImagesInBase64(TEMPLATES_DIR_NAME + "/images/aar-logo-short-small.png");

    public LegalFactGenerator(
            DocumentComposition documentComposition,
            CustomInstantWriter instantWriter,
            PhysicalAddressWriter physicalAddressWriter,
            PnDeliveryPushConfigs pnDeliveryPushConfigs,
            InstantNowSupplier instantNowSupplier,
            PnSendModeUtils pnSendModeUtils) {
        this.documentComposition = documentComposition;
        this.instantWriter = instantWriter;
        this.physicalAddressWriter = physicalAddressWriter;
        this.pnDeliveryPushConfigs = pnDeliveryPushConfigs;
        this.instantNowSupplier = instantNowSupplier;
        this.pnSendModeUtils = pnSendModeUtils;
    }


    public byte[] generateNotificationReceivedLegalFact(NotificationInt notification) throws IOException {

        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put(FIELD_SEND_DATE, instantWriter.instantToDate( notification.getSentAt() ) );
        templateModel.put(FIELD_SEND_DATE_NO_TIME, instantWriter.instantToDate( notification.getSentAt(), true ) );
        templateModel.put(FIELD_NOTIFICATION, notification.toBuilder()
                .sender( notification.getSender().toBuilder()
                        .paDenomination( notification.getSender().getPaDenomination() )
                        .paTaxId( notification.getSender().getPaTaxId())
                        .build()
                )
                .build()
            );
        templateModel.put(FIELD_DIGESTS, extractNotificationAttachmentDigests( notification ) );
        templateModel.put(FIELD_ADDRESS_WRITER, this.physicalAddressWriter );
        templateModel.put(FIELD_LEGALFACT_CREATION_DATE, instantWriter.instantToDate( instantNowSupplier.get() ) );
        templateModel.put(FIELD_SUBJECT, notification.getSubject());

        return documentComposition.executePdfTemplate(
                retrieveTemplateFromLang(DocumentComposition.TemplateType.REQUEST_ACCEPTED, notification.getAdditionalLanguages()),
                templateModel
            );

    }

    private List<String> extractNotificationAttachmentDigests(NotificationInt notification) {
        List<String> digests = new ArrayList<>();

        // - Documents digests
        for(NotificationDocumentInt attachment: notification.getDocuments() ) {
            digests.add( FileUtils.convertBase64toHexUppercase(attachment.getDigests().getSha256()) );
        }

        // F24 digests
        for(NotificationRecipientInt recipient : notification.getRecipients()) {

            //add digests for v21
            addDigestsForMultiPayments(recipient.getPayments(), digests);

        }


        return digests;
    }

    private void addDigestsForMultiPayments(List<NotificationPaymentInfoInt> payments, List<String> digests) {
        if(!CollectionUtils.isEmpty(payments)){
            payments.forEach(payment -> {
                if(payment.getPagoPA() != null && payment.getPagoPA().getAttachment() != null){
                    digests.add(FileUtils.convertBase64toHexUppercase(payment.getPagoPA().getAttachment().getDigests().getSha256()));
                }
                if(payment.getF24() != null && payment.getF24().getMetadataAttachment() != null){
                    digests.add(FileUtils.convertBase64toHexUppercase(payment.getF24().getMetadataAttachment().getDigests().getSha256()));
                }
            });
        }
    }

    public byte[] generateNotificationViewedLegalFact(String iun, NotificationRecipientInt recipient, DelegateInfoInt delegateInfo, Instant timeStamp, NotificationInt notification) throws IOException {

        Map<String, Object> templateModel = new HashMap<>();
        
        templateModel.put(FIELD_IUN, iun);
        templateModel.put(FIELD_RECIPIENT, recipient);
        templateModel.put(FIELD_DELEGATE, delegateInfo);
        templateModel.put(FIELD_WHEN, instantWriter.instantToDate( timeStamp) );
        templateModel.put(FIELD_ADDRESS_WRITER, this.physicalAddressWriter );
        templateModel.put(FIELD_SEND_DATE_NO_TIME, instantWriter.instantToDate( timeStamp, true));
        templateModel.put(FIELD_LEGALFACT_CREATION_DATE, instantWriter.instantToDate( instantNowSupplier.get() ) );

        return documentComposition.executePdfTemplate(
                retrieveTemplateFromLang(DocumentComposition.TemplateType.NOTIFICATION_VIEWED, notification.getAdditionalLanguages()),
                templateModel
        );
    }

    private DocumentComposition.TemplateType retrieveTemplateFromLang(DocumentComposition.TemplateType italianTemplateType, List<String> additionalLanguages) {
        if(!pnDeliveryPushConfigs.isAdditionalLangsEnabled()
                || checkIfRequiredItalianTemplate(additionalLanguages)
                || AAR_NOTIFICATION_RADD.equals(italianTemplateType)){
            return italianTemplateType;
        }
        return additionalLanguages.stream()
                .map(lang -> DocumentComposition.retrieveTemplateFromLang(italianTemplateType, lang))
                .findFirst() /* è possibile avere solo una lingua aggiuntiva */
                .orElseThrow(() -> new PnInvalidTemplateException("Error During retrieve template","TemplateType enum not found for given additional lang",ERROR_CODE_DELIVERYPUSH_INVALID_TEMPLATE));
    }

    private boolean checkIfRequiredItalianTemplate(List<String> additionalLanguages) {
        return CollectionUtils.isEmpty(additionalLanguages);
    }

    @Value
    @Builder
    @AllArgsConstructor
    @Jacksonized
    public static class PecDeliveryInfo {
        private String denomination;
        private String taxId;
        private RecipientTypeInt recipientType;
        private String address;
        private String addressSource;
        private String type;
        private Instant orderBy;
        private String responseDate;
        private boolean ok;
    }

    public byte[] generatePecDeliveryWorkflowLegalFact(List<SendDigitalFeedbackDetailsInt> feedbackFromExtChannelList,
                                                       NotificationInt notification,
                                                       NotificationRecipientInt recipient,
                                                       EndWorkflowStatus status,
                                                       Instant completionWorkflowDate) throws IOException {

        List<PecDeliveryInfo> pecDeliveries = feedbackFromExtChannelList.stream()
                .map( feedbackFromExtChannel -> {

                    ResponseStatusInt sentPecStatus = feedbackFromExtChannel.getResponseStatus();
                    Instant notificationDate = feedbackFromExtChannel.getNotificationDate();

                    return new PecDeliveryInfo(
                            recipient.getDenomination(),
                            recipient.getTaxId(),
                            recipient.getRecipientType(),
                            feedbackFromExtChannel.getDigitalAddress().getAddress(),
                            feedbackFromExtChannel.getDigitalAddressSource() != null ? feedbackFromExtChannel.getDigitalAddressSource().getValue() : null,
                            feedbackFromExtChannel.getDigitalAddress().getType().getValue(),
                            notificationDate,
                            instantWriter.instantToDate(notificationDate),
                            ResponseStatusInt.OK.equals( sentPecStatus )
                    );
                })
                .sorted( Comparator.comparing( PecDeliveryInfo::getOrderBy ))
                .collect(Collectors.toList());

        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put(FIELD_SEND_DATE_NO_TIME, instantWriter.instantToDate( notification.getSentAt(), true ) );
        templateModel.put(FIELD_IUN, notification.getIun() );
        templateModel.put(FIELD_DELIVERIES, pecDeliveries);
        templateModel.put(FIELD_END_WORKFLOW_STATUS, status.toString() );
        templateModel.put(FIELD_END_WORKFLOW_DATE, instantWriter.instantToDate( completionWorkflowDate ) );
        templateModel.put(FIELD_RECIPIENT, recipient);
        templateModel.put(FIELD_ADDRESS_WRITER, this.physicalAddressWriter );
        templateModel.put(FIELD_LEGALFACT_CREATION_DATE, instantWriter.instantToDate( instantNowSupplier.get() ) );

        return documentComposition.executePdfTemplate(
                retrieveTemplateFromLang(DocumentComposition.TemplateType.DIGITAL_NOTIFICATION_WORKFLOW, notification.getAdditionalLanguages()),
                templateModel
        );
    }



    public byte[] generateAnalogDeliveryFailureWorkflowLegalFact(NotificationInt notification,
                                                                 NotificationRecipientInt recipient,
                                                                 EndWorkflowStatus status,
                                                                 Instant failureWorkflowDate) throws IOException {


        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put(FIELD_SEND_DATE_NO_TIME, instantWriter.instantToDate( notification.getSentAt(), true ) );
        templateModel.put(FIELD_IUN, notification.getIun() );
        templateModel.put(FIELD_END_WORKFLOW_STATUS, status.toString() );
        templateModel.put(FIELD_END_WORKFLOW_DATE, instantWriter.instantToDate( failureWorkflowDate, true ) );
        templateModel.put(FIELD_END_WORKFLOW_TIME, instantWriter.instantToTime( failureWorkflowDate ) );
        templateModel.put(FIELD_RECIPIENT, recipient);
        templateModel.put(FIELD_ADDRESS_WRITER, this.physicalAddressWriter );
        templateModel.put(FIELD_LEGALFACT_CREATION_DATE, instantWriter.instantToDate( instantNowSupplier.get() ) );

        return documentComposition.executePdfTemplate(
                retrieveTemplateFromLang(DocumentComposition.TemplateType.ANALOG_NOTIFICATION_WORKFLOW_FAILURE, notification.getAdditionalLanguages()),
                templateModel
        );
    }

    public AARInfo generateNotificationAAR(NotificationInt notification, NotificationRecipientInt recipient, String quickAccessToken) throws IOException {

        Map<String, Object> templateModel = prepareTemplateModelParams(notification, recipient, quickAccessToken);
        
        PnSendMode pnSendMode = pnSendModeUtils.getPnSendMode(notification.getSentAt());
        
        if(pnSendMode != null){
            final AarTemplateChooseStrategy aarTemplateTypeChooseStrategy = pnSendMode.getAarTemplateTypeChooseStrategy();
            final AarTemplateType aarTemplateType = aarTemplateTypeChooseStrategy.choose(recipient.getPhysicalAddress());
            log.debug("aarTemplateType generated is ={} - iun={}", aarTemplateType, notification.getIun());
            byte[] bytesArrayGeneratedAar = documentComposition.executePdfTemplate(
                    retrieveTemplateFromLang(aarTemplateType.getTemplateType(), notification.getAdditionalLanguages()),
                    templateModel
            );

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

    public String generateNotificationAARBody(NotificationInt notification, NotificationRecipientInt recipient, String quickAccesstoken) {

        Map<String, Object> templateModel = prepareTemplateModelParams(notification, recipient, quickAccesstoken);
        templateModel.put(FIELD_LOGO, SEND_LOGO_BASE64);

        return documentComposition.executeTextTemplate(
                retrieveTemplateFromLang(DocumentComposition.TemplateType.AAR_NOTIFICATION_EMAIL, notification.getAdditionalLanguages()),
                templateModel
            );

    }

    public String generateNotificationAARPECBody(NotificationInt notification, NotificationRecipientInt recipient, String quickAccesstoken) {

        Map<String, Object> templateModel = prepareTemplateModelParams(notification, recipient, quickAccesstoken);
        templateModel.put(FIELD_LOGO, SEND_LOGO_BASE64);

        return documentComposition.executeTextTemplate(
                retrieveTemplateFromLang(DocumentComposition.TemplateType.AAR_NOTIFICATION_PEC, notification.getAdditionalLanguages()),
                templateModel
        );

    }


    public String generateNotificationAARSubject(NotificationInt notification) {

        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put(FIELD_NOTIFICATION, notification);

        return documentComposition.executeTextTemplate(
                DocumentComposition.TemplateType.AAR_NOTIFICATION_SUBJECT,
                templateModel
        );

    }


    public String generateNotificationAARForSMS(NotificationInt notification) {

        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put(FIELD_NOTIFICATION, notification);

        return documentComposition.executeTextTemplate(
                DocumentComposition.TemplateType.AAR_NOTIFICATION_SMS,
                templateModel
        );
    }

    public int getNumberOfPages( byte[] pdfBytes ) {
        return documentComposition.getNumberOfPageFromPdfBytes( pdfBytes );
    }


    @NotNull
    private Map<String, Object> prepareTemplateModelParams(NotificationInt notification, NotificationRecipientInt recipient, String quickAccesstoken) {
        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put(FIELD_SEND_DATE, instantWriter.instantToDate( notification.getSentAt() ) );
        templateModel.put(FIELD_SEND_DATE_NO_TIME, instantWriter.instantToDate( notification.getSentAt(), true ) );
        templateModel.put(FIELD_NOTIFICATION, notification);
        templateModel.put(FIELD_RECIPIENT, recipient);
        templateModel.put(FIELD_SENDER_ADDRESS, pnDeliveryPushConfigs.getPaperChannel().getSenderPhysicalAddress());
        templateModel.put(FIELD_ADDRESS_WRITER, this.physicalAddressWriter );
        templateModel.put(FIELD_PIATTAFORMA_NOTIFICHE_URL, this.getAccessUrl(recipient) );
        templateModel.put(FIELD_PIATTAFORMA_NOTIFICHE_URL_LABEL, this.getAccessUrlLabel(recipient) );
        templateModel.put(FIELD_PN_FAQ_COMPLETION_MOMENT_URL, this.getFAQCompletionMomentAccessLink());
        templateModel.put(FIELD_PN_FAQ_COMPLETION_MOMENT_URL_LABEL, this.getFAQCompletionMomentAccessLinkLabel());
        templateModel.put(FIELD_SEND_URL, this.getFAQSendURL());
        templateModel.put(FIELD_QUICK_ACCESS_LINK, this.getQuickAccessLink(recipient, quickAccesstoken) );
        templateModel.put(FIELD_RECIPIENT_TYPE, this.getRecipientTypeForHTMLTemplate(recipient));
        templateModel.put(FIELD_SENDURL, this.getAccessLink());
        templateModel.put(FIELD_SENDURL_LABEL, this.getAccessLinkLabel());
        templateModel.put(FIELD_PERFEZIONAMENTO, this.getPerfezionamentoLink());
        templateModel.put(FIELD_PERFEZIONAMENTO_LABEL, this.getPerfezionamentoLinkLabel());
        templateModel.put(FIELD_LOGO_LINK, this.getLogoLink());
        templateModel.put(FIELD_RADDPHONENUMBER, this.getRaddPhoneNumber());
        addAdditional(templateModel);

        String qrCodeQuickAccessUrlAarDetail = this.getQrCodeQuickAccessUrlAarDetail(recipient, quickAccesstoken);
        log.debug( "generateNotificationAAR iun {} quickAccessUrl {}", notification.getIun(), qrCodeQuickAccessUrlAarDetail );
        templateModel.put(FIELD_QRCODE_QUICK_ACCESS_LINK, qrCodeQuickAccessUrlAarDetail);

        return templateModel;
    }

    private void addAdditional(Map<String, Object> templateModel) {
        if(this.pnDeliveryPushConfigs.getWebapp().getAdditional() != null) {
            this.pnDeliveryPushConfigs.getWebapp().getAdditional()
                    .forEach((key, value) -> templateModel.put(FIELD_ADDITIONAL + key, value));
        }
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
      return "data:image/png;base64, ".concat(Base64Utils.encodeToString(QrCodeUtils.generateQRCodeImage(url, 180, 180, pnDeliveryPushConfigs.getErrorCorrectionLevelQrCode())));
    }

    private String getQuickAccessLink(NotificationRecipientInt recipient, String quickAccessToken) {
        String templateUrl = getAccessUrl(recipient) + pnDeliveryPushConfigs.getWebapp().getQuickAccessUrlAarDetailSuffix() ;

        log.debug( "getQrCodeQuickAccessUrlAarDetail templateUrl {} quickAccessLink {}", templateUrl, quickAccessToken );
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

    private String getAssetsLink() {
        return pnDeliveryPushConfigs.getWebapp().getLandingUrl() + "static/generic_assets/";
    }

    private String getFAQCompletionMomentAccessLink() {
        return this.getFAQAccessLink() + "#" + pnDeliveryPushConfigs.getWebapp().getFaqCompletionMomentHash();
    }

    private String getFAQCompletionMomentAccessLinkLabel() {
        return this.getAccessLinkLabel() + '#' + pnDeliveryPushConfigs.getWebapp().getFaqCompletionMomentHash();
    }

    private String getFAQSendURL() {
        return this.getFAQAccessLink() + "#" + pnDeliveryPushConfigs.getWebapp().getFaqSendHash();
    }

    private String getLogoLink() {
        return this.getAssetsLink() + "aar-logo-short-small.png";
    }

    private String getRaddPhoneNumber() {
        return pnDeliveryPushConfigs.getWebapp().getRaddPhoneNumber();
    }

    private String getRecipientTypeForHTMLTemplate(NotificationRecipientInt recipientInt) {
        return recipientInt.getRecipientType() == RecipientTypeInt.PG ? "giuridica" : "fisica";
    }

    private String getAccessUrl(NotificationRecipientInt recipient) {

        return RecipientTypeInt.PF == recipient.getRecipientType()
                ? pnDeliveryPushConfigs.getWebapp().getDirectAccessUrlTemplatePhysical()
                : pnDeliveryPushConfigs.getWebapp().getDirectAccessUrlTemplateLegal();
    }

     private static String readLocalImagesInBase64(String classPath) {
        try (InputStream ioStream = new ClassPathResource(classPath).getInputStream()) {
            byte[] bytes = IOUtils.toByteArray(ioStream);
            return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            throw new PnReadFileException("error during file conversion", e);
        }

    }

}

