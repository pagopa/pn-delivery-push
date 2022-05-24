package it.pagopa.pn.deliverypush.legalfacts;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.SendDigitalFeedback;
import org.springframework.stereotype.Component;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationDocumentInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationPaymentInfoInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.ResponseStatus;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class LegalFactGenerator {

    private final DocumentComposition documentComposition;
    private final CustomInstantWriter instantWriter;
    private final PhysicalAddressWriter physicalAddressWriter;


    public LegalFactGenerator(
            DocumentComposition documentComposition,
            CustomInstantWriter instantWriter,
            PhysicalAddressWriter physicalAddressWriter) {
        this.documentComposition = documentComposition;
        this.instantWriter = instantWriter;
        this.physicalAddressWriter = physicalAddressWriter;
    }


    public byte[] generateNotificationReceivedLegalFact(NotificationInt notification) throws IOException {

        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put("sendDate", instantWriter.instantToDate( notification.getSentAt() ) );
        templateModel.put("sendDateNoTime", instantWriter.instantToDate( notification.getSentAt(), true ) );
        templateModel.put("notification", notification.toBuilder()
                .sender( notification.getSender().toBuilder()
                        .paDenomination( "DenominationOfPA_" + notification.getSender().getPaId() )
                        .paTaxId( "TaxIdOfPA_" + notification.getSender().getPaId())
                        .build()
                )
                .build()
            );
        templateModel.put("digests", extractNotificationAttachmentDigests( notification ) );
        templateModel.put("addressWriter", this.physicalAddressWriter );

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
        templateModel.put("iun", iun);
        templateModel.put("recipient", recipient);
        templateModel.put("when", instantWriter.instantToDate( timeStamp) );
        templateModel.put("addressWriter", this.physicalAddressWriter );
        templateModel.put("sendDateNoTime", instantWriter.instantToDate( timeStamp, true));

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

    public byte[] generatePecDeliveryWorkflowLegalFact(List<SendDigitalFeedback> feedbackFromExtChannelList, NotificationInt notification, NotificationRecipientInt recipient) throws IOException {

        List<PecDeliveryInfo> pecDeliveries = feedbackFromExtChannelList.stream()
                .map( feedbackFromExtChannel -> {

                    ResponseStatus status = feedbackFromExtChannel.getResponseStatus();
                    Instant notificationDate = feedbackFromExtChannel.getNotificationDate();

                    return new PecDeliveryInfo(
                            recipient.getDenomination(),
                            recipient.getTaxId(),
                            feedbackFromExtChannel.getDigitalAddress().getAddress(),
                            notificationDate,
                            instantWriter.instantToDate(notificationDate),
                            ResponseStatus.OK.equals( status )
                    );
                })
                .sorted( Comparator.comparing( PecDeliveryInfo::getOrderBy ))
                .collect(Collectors.toList());

        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put("sendDateNoTime", instantWriter.instantToDate( notification.getSentAt(), true ) );
        templateModel.put("iun", notification.getIun() );
        templateModel.put("deliveries", pecDeliveries);

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
     * @return
     * @throws IOException
     */
    public byte[] generateFileCompliance(String pdfFileName, String signature, Instant timeReference) throws IOException {

        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put("signature", signature);
        templateModel.put("timeReference", timeReference);
        templateModel.put("pdfFileName", pdfFileName );
        templateModel.put("sendDate", instantWriter.instantToDate( Instant.now()/*, true*/ ) );

        return documentComposition.executePdfTemplate(
                DocumentComposition.TemplateType.FILE_COMPLIANCE,
                templateModel
        );
    }

    public byte[] generateNotificationAAR(NotificationInt notification) throws IOException {

        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put("sendDate", instantWriter.instantToDate( notification.getSentAt() ) );
        templateModel.put("sendDateNoTime", instantWriter.instantToDate( notification.getSentAt(), true ) );
        templateModel.put("notification", notification.toBuilder().build());
        templateModel.put("addressWriter", this.physicalAddressWriter );

        return documentComposition.executePdfTemplate(
                DocumentComposition.TemplateType.AAR_NOTIFICATION,
                templateModel
            );

    }
}

