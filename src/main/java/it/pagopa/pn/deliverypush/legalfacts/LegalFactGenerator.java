package it.pagopa.pn.deliverypush.legalfacts;

import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationDocumentInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationPaymentInfoInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.ResponseStatusInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendDigitalFeedbackDetailsInt;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
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
    public static final String FIELD_WHEN = "when";
    public static final String FIELD_PIATTAFORMA_NOTIFICHE_URL = "piattaformaNotificheURL";
    public static final String FIELD_PIATTAFORMA_NOTIFICHE_URL_LABEL = "piattaformaNotificheURLLabel";
    public static final String FIELD_PN_FAQ_URL = "PNFaqURL";
    private final DocumentComposition documentComposition;
    private final CustomInstantWriter instantWriter;
    private final PhysicalAddressWriter physicalAddressWriter;
    private final PnDeliveryPushConfigs pnDeliveryPushConfigs;

    public LegalFactGenerator(
            DocumentComposition documentComposition,
            CustomInstantWriter instantWriter,
            PhysicalAddressWriter physicalAddressWriter,
            PnDeliveryPushConfigs pnDeliveryPushConfigs) {
        this.documentComposition = documentComposition;
        this.instantWriter = instantWriter;
        this.physicalAddressWriter = physicalAddressWriter;
        this.pnDeliveryPushConfigs = pnDeliveryPushConfigs;
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

        return documentComposition.executePdfTemplate(
                DocumentComposition.TemplateType.REQUEST_ACCEPTED,
                templateModel
            );

    }

    private List<String> extractNotificationAttachmentDigests(NotificationInt notification) {
        List<String> digests = new ArrayList<>();

        // - Documents digests
        for(NotificationDocumentInt attachment: notification.getDocuments() ) {
            digests.add( attachment.getDigests().getSha256() );
        }

        // F24 digests
        for(NotificationRecipientInt recipient : notification.getRecipients()) {

            NotificationPaymentInfoInt recipientPayment = recipient.getPayment();
            if (recipientPayment != null ) {

                NotificationDocumentInt pagoPaForm = recipientPayment.getPagoPaForm();
                if ( pagoPaForm != null ) {
                    digests.add( pagoPaForm.getDigests().getSha256() );
                }

                NotificationDocumentInt flatRateF24 = recipientPayment.getF24flatRate();
                if ( flatRateF24 != null ) {
                    digests.add(flatRateF24.getDigests().getSha256() );
                }
                
            }
        }


        return digests;
    }


    public byte[] generateNotificationViewedLegalFact(String iun, NotificationRecipientInt recipient, Instant timeStamp) throws IOException {

        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put(FIELD_IUN, iun);
        templateModel.put(FIELD_RECIPIENT, recipient);
        templateModel.put(FIELD_WHEN, instantWriter.instantToDate( timeStamp) );
        templateModel.put(FIELD_ADDRESS_WRITER, this.physicalAddressWriter );
        templateModel.put(FIELD_SEND_DATE_NO_TIME, instantWriter.instantToDate( timeStamp, true));

        return documentComposition.executePdfTemplate(
                DocumentComposition.TemplateType.NOTIFICATION_VIEWED,
                templateModel
        );
    }

    @Value
    public static class PecDeliveryInfo {
        private String denomination;
        private String taxId;
        private String address;
        private Instant orderBy;
        private String responseDate;
        private boolean ok;
    }
    
    /*
    @Deprecated
    public byte[] generatePecDeliveryWorkflowLegalFact(List<Action> actions, NotificationInt notification, NotificationPathChooseDetails addresses) throws IOException {

        List<PecDeliveryInfo> pecDeliveries = actions.stream()
                .map( action -> {

                    DigitalAddress address;
                    switch ( action.getDigitalAddressSource() ) {
                        case PLATFORM: address = addresses.getPlatform(); break;
                        case SPECIAL: address = addresses.getSpecial(); break;
                        case GENERAL: address = addresses.getGeneral(); break;
                        default: throw new PnInternalException("Address source not supported " + action.getDigitalAddressSource());
                    }

                    NotificationRecipientInt recipient = notification.getRecipients().get(action.getRecipientIndex());
                    PnExtChnProgressStatus status = action.getResponseStatus();

                    return new PecDeliveryInfo(
                            recipient.getDenomination(),
                            recipient.getTaxId(),
                            address.getAddress(),
                            action.getNotBefore(),
                            instantWriter.instantToDate( action.getNotBefore() ),
                            PnExtChnProgressStatus.OK.equals( status )
                        );
                })
                .sorted( Comparator.comparing( PecDeliveryInfo::getOrderBy ))
                .collect(Collectors.toList());

        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put("iun", notification.getIun() );
        templateModel.put("deliveries", pecDeliveries);

        return documentComposition.executePdfTemplate(
                DocumentComposition.TemplateType.DIGITAL_NOTIFICATION_WORKFLOW,
                templateModel
        );
    }
    
     */

    public byte[] generatePecDeliveryWorkflowLegalFact(List<SendDigitalFeedbackDetailsInt> feedbackFromExtChannelList, NotificationInt notification, NotificationRecipientInt recipient) throws IOException {

        List<PecDeliveryInfo> pecDeliveries = feedbackFromExtChannelList.stream()
                .map( feedbackFromExtChannel -> {

                    ResponseStatusInt status = feedbackFromExtChannel.getResponseStatus();
                    Instant notificationDate = feedbackFromExtChannel.getNotificationDate();

                    return new PecDeliveryInfo(
                            recipient.getDenomination(),
                            recipient.getTaxId(),
                            feedbackFromExtChannel.getDigitalAddress().getAddress(),
                            notificationDate,
                            instantWriter.instantToDate(notificationDate),
                            ResponseStatusInt.OK.equals( status )
                    );
                })
                .sorted( Comparator.comparing( PecDeliveryInfo::getOrderBy ))
                .collect(Collectors.toList());

        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put(FIELD_SEND_DATE_NO_TIME, instantWriter.instantToDate( notification.getSentAt(), true ) );
        templateModel.put(FIELD_IUN, notification.getIun() );
        templateModel.put(FIELD_DELIVERIES, pecDeliveries);

        return documentComposition.executePdfTemplate(
                DocumentComposition.TemplateType.DIGITAL_NOTIFICATION_WORKFLOW,
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

    public byte[] generateNotificationAAR(NotificationInt notification, NotificationRecipientInt recipient) throws IOException {

        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put(FIELD_SEND_DATE, instantWriter.instantToDate( notification.getSentAt() ) );
        templateModel.put(FIELD_SEND_DATE_NO_TIME, instantWriter.instantToDate( notification.getSentAt(), true ) );
        templateModel.put(FIELD_NOTIFICATION, notification);
        templateModel.put(FIELD_RECIPIENT, recipient);
        templateModel.put(FIELD_ADDRESS_WRITER, this.physicalAddressWriter );

        return documentComposition.executePdfTemplate(
                DocumentComposition.TemplateType.AAR_NOTIFICATION,
                templateModel
        );

    }

    public String generateNotificationAARBody(NotificationInt notification, NotificationRecipientInt recipient) {

        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put(FIELD_SEND_DATE, instantWriter.instantToDate( notification.getSentAt() ) );
        templateModel.put(FIELD_SEND_DATE_NO_TIME, instantWriter.instantToDate( notification.getSentAt(), true ) );
        templateModel.put(FIELD_NOTIFICATION, notification);
        templateModel.put(FIELD_RECIPIENT, recipient);
        templateModel.put(FIELD_ADDRESS_WRITER, this.physicalAddressWriter );
        templateModel.put(FIELD_PIATTAFORMA_NOTIFICHE_URL, this.getAccessUrl(notification.getIun()) );
        templateModel.put(FIELD_PIATTAFORMA_NOTIFICHE_URL_LABEL, this.getAccessUrlLabel() );
        templateModel.put(FIELD_PN_FAQ_URL, this.pnDeliveryPushConfigs.getWebapp().getFaqUrlTemplate() );


        return documentComposition.executeTextTemplate(
                DocumentComposition.TemplateType.AAR_NOTIFICATION_EMAIL,
                templateModel
            );

    }

    public String generateNotificationAARPECBody(NotificationInt notification, NotificationRecipientInt recipient) {

        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put(FIELD_SEND_DATE, instantWriter.instantToDate( notification.getSentAt() ) );
        templateModel.put(FIELD_SEND_DATE_NO_TIME, instantWriter.instantToDate( notification.getSentAt(), true ) );
        templateModel.put(FIELD_NOTIFICATION, notification);
        templateModel.put(FIELD_RECIPIENT, recipient);
        templateModel.put(FIELD_ADDRESS_WRITER, this.physicalAddressWriter );
        templateModel.put(FIELD_PIATTAFORMA_NOTIFICHE_URL, this.getAccessUrl(notification.getIun()) );
        templateModel.put(FIELD_PIATTAFORMA_NOTIFICHE_URL_LABEL, this.getAccessUrlLabel() );
        templateModel.put(FIELD_PN_FAQ_URL, this.pnDeliveryPushConfigs.getWebapp().getFaqUrlTemplate() );

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


    private String getAccessUrl(String iun) {
        return String.format(pnDeliveryPushConfigs.getWebapp().getDirectAccessUrlTemplate(), iun);
    }

    private String getAccessUrlLabel() {
        try {
            return new URL(pnDeliveryPushConfigs.getWebapp().getDirectAccessUrlTemplate()).getHost();
        } catch (MalformedURLException e) {
            log.warn("cannot get host", e);
            return pnDeliveryPushConfigs.getWebapp().getDirectAccessUrlTemplate();
        }
    }


}

