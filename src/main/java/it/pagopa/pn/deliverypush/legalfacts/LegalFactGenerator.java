package it.pagopa.pn.deliverypush.legalfacts;

import it.pagopa.pn.api.dto.events.PnExtChnProgressStatus;
import it.pagopa.pn.api.dto.extchannel.ExtChannelResponseStatus;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationAttachment;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.timeline.NotificationPathChooseDetails;
import it.pagopa.pn.api.dto.notification.timeline.SendDigitalFeedback;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

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


    public byte[] generateNotificationReceivedLegalFact(Notification notification) throws IOException {

        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put("sendDate", instantWriter.instantToDate( notification.getSentAt() ) );
        templateModel.put("notification", notification.toBuilder()
                .sender( notification.getSender().toBuilder()
                        .paDenomination( "DenominationOfPA_" + notification.getSender().getPaId() )
                        .taxId( "TaxIdOfPA_" + notification.getSender().getPaId())
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

    private List<String> extractNotificationAttachmentDigests(Notification notification) {
        List<String> digests = new ArrayList<>();

        // - Documents digests
        for(NotificationAttachment attachment: notification.getDocuments() ) {
            digests.add( attachment.getDigests().getSha256() );
        }

        // F24 digests
        if (notification.getPayment() != null && notification.getPayment().getF24() != null) {

            NotificationAttachment flatRateF24 = notification.getPayment().getF24().getFlatRate();
            if ( flatRateF24 != null ) {
                digests.add(flatRateF24.getDigests().getSha256() );
            }

            NotificationAttachment digitalF24 = notification.getPayment().getF24().getDigital();
            if ( digitalF24 != null ) {
                digests.add( digitalF24.getDigests().getSha256() );
            }

            NotificationAttachment analogF24 = notification.getPayment().getF24().getAnalog();
            if ( analogF24 != null ) {
                digests.add( analogF24.getDigests().getSha256() );
            }
        }

        return digests;
    }


    public byte[] generateNotificationViewedLegalFact(String iun, NotificationRecipient recipient, Instant timeStamp) throws IOException {

        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put("iun", iun);
        templateModel.put("recipient", recipient);
        templateModel.put("when", instantWriter.instantToDate( timeStamp) );
        templateModel.put("addressWriter", this.physicalAddressWriter );

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

    @Deprecated
    public byte[] generatePecDeliveryWorkflowLegalFact(List<Action> actions, Notification notification, NotificationPathChooseDetails addresses) throws IOException {

        List<PecDeliveryInfo> pecDeliveries = actions.stream()
                .map( action -> {
                    DigitalAddress address = action.getDigitalAddressSource().getAddressFrom(addresses);
                    NotificationRecipient recipient = notification.getRecipients().get(action.getRecipientIndex());
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

    public byte[] generatePecDeliveryWorkflowLegalFact(List<SendDigitalFeedback> feedbackFromExtChannelList, Notification notification, NotificationRecipient recipient) throws IOException {

        List<PecDeliveryInfo> pecDeliveries = feedbackFromExtChannelList.stream()
                .map( feedbackFromExtChannel -> {

                    ExtChannelResponseStatus status = feedbackFromExtChannel.getResponseStatus();
                    Instant notificationDate = feedbackFromExtChannel.getNotificationDate();

                    return new PecDeliveryInfo(
                            recipient.getDenomination(),
                            recipient.getTaxId(),
                            feedbackFromExtChannel.getAddress().getAddress(),
                            notificationDate,
                            instantWriter.instantToDate(notificationDate),
                            ExtChannelResponseStatus.OK.equals( status )
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


}

