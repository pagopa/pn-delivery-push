package it.pagopa.pn.deliverypush.legalfacts;

import com.amazonaws.util.IOUtils;
import it.pagopa.pn.commons.configs.MVPParameterConsumer;
import it.pagopa.pn.commons.utils.FileUtils;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.action.utils.EndWorkflowStatus;
import it.pagopa.pn.deliverypush.action.utils.InstantNowSupplier;
import it.pagopa.pn.deliverypush.dto.ext.datavault.RecipientTypeInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationDocumentInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationPaymentInfoInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.ResponseStatusInt;
import it.pagopa.pn.deliverypush.dto.mandate.DelegateInfoInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendDigitalFeedbackDetailsInt;
import it.pagopa.pn.deliverypush.exceptions.PnReadFileException;
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

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

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

    public static final String FIELD_SENDURL = "sendURL";
    public static final String FIELD_LOGO = "logoBase64";

    private final DocumentComposition documentComposition;
    private final CustomInstantWriter instantWriter;
    private final PhysicalAddressWriter physicalAddressWriter;
    private final PnDeliveryPushConfigs pnDeliveryPushConfigs;
    private final InstantNowSupplier instantNowSupplier;
    private final MVPParameterConsumer mvpParameterConsumer;
    private static final String TEMPLATES_DIR_NAME = "documents_composition_templates";

    private static final String SEND_LOGO_BASE64 =  readLocalImagesInBase64(TEMPLATES_DIR_NAME + "/images/aar-logo-short-small.png");

    public LegalFactGenerator(
            DocumentComposition documentComposition,
            CustomInstantWriter instantWriter,
            PhysicalAddressWriter physicalAddressWriter,
            PnDeliveryPushConfigs pnDeliveryPushConfigs,
            InstantNowSupplier instantNowSupplier,
            MVPParameterConsumer mvpParameterConsumer) {
        this.documentComposition = documentComposition;
        this.instantWriter = instantWriter;
        this.physicalAddressWriter = physicalAddressWriter;
        this.pnDeliveryPushConfigs = pnDeliveryPushConfigs;
        this.instantNowSupplier = instantNowSupplier;
        this.mvpParameterConsumer = mvpParameterConsumer;
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

        return documentComposition.executePdfTemplate(
                DocumentComposition.TemplateType.REQUEST_ACCEPTED,
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

            NotificationPaymentInfoInt recipientPayment = recipient.getPayment();
            if (recipientPayment != null ) {

                NotificationDocumentInt pagoPaForm = recipientPayment.getPagoPaForm();
                if ( pagoPaForm != null ) {
                    digests.add( FileUtils.convertBase64toHexUppercase(pagoPaForm.getDigests().getSha256()) );
                }

                NotificationDocumentInt flatRateF24 = recipientPayment.getF24flatRate();
                if ( flatRateF24 != null ) {
                    digests.add( FileUtils.convertBase64toHexUppercase(flatRateF24.getDigests().getSha256()) );
                }

                NotificationDocumentInt f24Standard = recipientPayment.getF24standard();
                if ( f24Standard != null ) {
                    digests.add( FileUtils.convertBase64toHexUppercase(f24Standard.getDigests().getSha256()) );
                }
                
            }
        }


        return digests;
    }
    
    public byte[] generateNotificationViewedLegalFact(String iun, NotificationRecipientInt recipient, DelegateInfoInt delegateInfo, Instant timeStamp) throws IOException {

        Map<String, Object> templateModel = new HashMap<>();
        
        templateModel.put(FIELD_IUN, iun);
        templateModel.put(FIELD_RECIPIENT, recipient);
        templateModel.put(FIELD_DELEGATE, delegateInfo);
        templateModel.put(FIELD_WHEN, instantWriter.instantToDate( timeStamp) );
        templateModel.put(FIELD_ADDRESS_WRITER, this.physicalAddressWriter );
        templateModel.put(FIELD_SEND_DATE_NO_TIME, instantWriter.instantToDate( timeStamp, true));
        templateModel.put(FIELD_LEGALFACT_CREATION_DATE, instantWriter.instantToDate( instantNowSupplier.get() ) );

        return documentComposition.executePdfTemplate(
                DocumentComposition.TemplateType.NOTIFICATION_VIEWED,
                templateModel
        );
    }

    @Value
    @Builder
    @AllArgsConstructor
    @Jacksonized
    public static class PecDeliveryInfo {
        private String denomination;
        private String taxId;
        private String address;
        private String addressSource;
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
                            feedbackFromExtChannel.getDigitalAddress().getAddress(),
                            feedbackFromExtChannel.getDigitalAddressSource() != null ? feedbackFromExtChannel.getDigitalAddressSource().getValue() : null,
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
                DocumentComposition.TemplateType.DIGITAL_NOTIFICATION_WORKFLOW,
                templateModel
        );
    }



    public byte[] generateAnalogDeliveryFailureWorkflowLegalFact(NotificationInt notification,
                                                                 NotificationRecipientInt recipient,
                                                                 EndWorkflowStatus status,
                                                                 Instant completionWorkflowDate) throws IOException {


        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put(FIELD_SEND_DATE_NO_TIME, instantWriter.instantToDate( notification.getSentAt(), true ) );
        templateModel.put(FIELD_IUN, notification.getIun() );
        templateModel.put(FIELD_END_WORKFLOW_STATUS, status.toString() );
        templateModel.put(FIELD_END_WORKFLOW_DATE, instantWriter.instantToDate( completionWorkflowDate, true ) );
        templateModel.put(FIELD_END_WORKFLOW_TIME, instantWriter.instantToTime( completionWorkflowDate ) );
        templateModel.put(FIELD_RECIPIENT, recipient);
        templateModel.put(FIELD_ADDRESS_WRITER, this.physicalAddressWriter );
        templateModel.put(FIELD_LEGALFACT_CREATION_DATE, instantWriter.instantToDate( instantNowSupplier.get() ) );


        return documentComposition.executePdfTemplate(
                DocumentComposition.TemplateType.ANALOG_NOTIFICATION_WORKFLOW_FAILURE,
                templateModel
        );
    }
    
    /**
     * Generate the File Compliance Certificate, according to design 4h of: 
     * https://www.figma.com/file/HjyZhnoAKbzCbxkmQCGsZw/Piattaforma-Notifiche?node-id=13514%3A94002
     * 
     * @param pdfFileName - the fileName to certificate, without extension
     * @param signature - the signature (footprint) of file
     * @param timeReference - file temporal reference
     * @return documento pdf
     * @throws IOException in caso di generazione fallita
     */
    public byte[] generateFileCompliance(String pdfFileName, String signature, Instant timeReference) throws IOException {

        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put(FIELD_SIGNATURE, signature);
        templateModel.put(FIELD_TIME_REFERENCE, timeReference);
        templateModel.put(FIELD_PDF_FILE_NAME, pdfFileName );
        templateModel.put(FIELD_SEND_DATE, instantWriter.instantToDate( Instant.now()/*, true*/ ) );

        return documentComposition.executePdfTemplate(
                DocumentComposition.TemplateType.FILE_COMPLIANCE,
                templateModel
        );
    }

    public byte[] generateNotificationAAR(NotificationInt notification, NotificationRecipientInt recipient, String quickAccessToken) throws IOException {

        Map<String, Object> templateModel = prepareTemplateModelParams(notification, recipient, quickAccessToken);


        if( Boolean.FALSE.equals( mvpParameterConsumer.isMvp( notification.getSender().getPaTaxId() ) ) ){
            return documentComposition.executePdfTemplate(
                    DocumentComposition.TemplateType.AAR_NOTIFICATION,
                    templateModel
            );
        }else {
            //In mvp viene generato un AAR senza QrCode
            return documentComposition.executePdfTemplate(
                    DocumentComposition.TemplateType.AAR_NOTIFICATION_MVP,
                    templateModel
            );
        }

    }

    public String generateNotificationAARBody(NotificationInt notification, NotificationRecipientInt recipient, String quickAccesstoken) {

        Map<String, Object> templateModel = prepareTemplateModelParams(notification, recipient, quickAccesstoken);
        templateModel.put(FIELD_LOGO, SEND_LOGO_BASE64);

        return documentComposition.executeTextTemplate(
                DocumentComposition.TemplateType.AAR_NOTIFICATION_EMAIL,
                templateModel
            );

    }

    public String generateNotificationAARPECBody(NotificationInt notification, NotificationRecipientInt recipient, String quickAccesstoken) {

        Map<String, Object> templateModel = prepareTemplateModelParams(notification, recipient, quickAccesstoken);
        templateModel.put(FIELD_LOGO, SEND_LOGO_BASE64);

        return documentComposition.executeTextTemplate(
                DocumentComposition.TemplateType.AAR_NOTIFICATION_PEC,
                templateModel
        );

    }


    public String generateNotificationAARSubject(NotificationInt notification) {

        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put(FIELD_SEND_DATE_NO_TIME, instantWriter.instantToDate( notification.getSentAt(), true ) );

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
        templateModel.put(FIELD_SEND_URL, this.getFAQSendURL());
        templateModel.put(FIELD_QUICK_ACCESS_LINK, this.getQuickAccessLink(recipient, quickAccesstoken) );
        templateModel.put(FIELD_RECIPIENT_TYPE, this.getRecipientTypeForHTMLTemplate(recipient));

        String sb = this.getAccessUrlLabel(recipient) + "/perfezionamento";
        templateModel.put(FIELD_PERFEZIONAMENTO, sb);

        String qrCodeQuickAccessUrlAarDetail = this.getQrCodeQuickAccessUrlAarDetail(recipient, quickAccesstoken);
        log.debug( "generateNotificationAAR iun {} quickAccessUrl {}", notification.getIun(), qrCodeQuickAccessUrlAarDetail );
        templateModel.put(FIELD_QRCODE_QUICK_ACCESS_LINK, qrCodeQuickAccessUrlAarDetail);

        return templateModel;
    }

    private String getAccessUrlLabel(NotificationRecipientInt recipient) {
        try {
            return new URL(getAccessUrl(recipient)).getHost();
        } catch (MalformedURLException e) {
            log.warn("cannot get host", e);
            return getAccessUrl(recipient);
        }
    }

    private String getQrCodeQuickAccessUrlAarDetail(NotificationRecipientInt recipient, String quickAccessToken) {
      String url = getQuickAccessLink(recipient, quickAccessToken);
      // Definire altezza e larghezza del qrcode
      return "data:image/png;base64, ".concat(Base64Utils.encodeToString(QrCodeUtils.generateQRCodeImage(url, 180, 180)));
    }

    private String getQuickAccessLink(NotificationRecipientInt recipient, String quickAccessToken) {
        String templateUrl = getAccessUrl(recipient) + pnDeliveryPushConfigs.getWebapp().getQuickAccessUrlAarDetailSuffix() ;

        log.debug( "getQrCodeQuickAccessUrlAarDetail templateUrl {} quickAccessLink {}", templateUrl, quickAccessToken );
        return templateUrl + '=' + quickAccessToken;
    }

    private String getFAQAccessLink() {
        return pnDeliveryPushConfigs.getWebapp().getLandingUrl() + "/" + pnDeliveryPushConfigs.getWebapp().getFaqUrlTemplateSuffix();
    }

    private String getFAQCompletionMomentAccessLink() {
        return this.getFAQAccessLink() + "#" + pnDeliveryPushConfigs.getWebapp().getFaqCompletionMomentHash();
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

     private static String readLocalImagesInBase64(String classPath) {
        try (InputStream ioStream = new ClassPathResource(classPath).getInputStream()) {
            byte[] bytes = IOUtils.toByteArray(ioStream);
            return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            throw new PnReadFileException("error during file conversion", e);
        }

    }
}

