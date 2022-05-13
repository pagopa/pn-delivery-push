package it.pagopa.pn.deliverypush.legalfacts;
//
//
//import it.pagopa.pn.commons.abstractions.FileStorage;
//import it.pagopa.pn.commons.exceptions.PnInternalException;
//import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
//import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
//import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
//import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactCategory;
//import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationPathChooseDetails;
//import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.SendDigitalFeedback;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Component;
//
//import java.io.ByteArrayInputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.time.Instant;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//import java.util.stream.Collectors;
//
//@Component
//@Slf4j
//public class LegalFactUtils {
//
//    public static final String LEGALFACTS_MEDIATYPE_STRING = "application/pdf";
//    private final FileStorage fileStorage;
//    private final LegalFactGenerator pdfUtils;
//    private final LegalfactsMetadataUtils legalfactMetadataUtils;
//
//
//    public LegalFactUtils(FileStorage fileStorage,
//                          LegalFactGenerator pdfUtils,
//                          LegalfactsMetadataUtils legalfactMetadataUtils
//    ) {
//        this.fileStorage = fileStorage;
//        this.pdfUtils = pdfUtils;
//        this.legalfactMetadataUtils = legalfactMetadataUtils;
//    }
//
//    public String saveLegalFact(String iun, String name, byte[] legalFact, Map<String, String> metadata) {
//        String fullFileKey = legalfactMetadataUtils.fullKey(iun, name);
//        String versionId;
//        try {
//            try (InputStream bodyStream = new ByteArrayInputStream(legalFact)) {
//                versionId = fileStorage.putFileVersion(fullFileKey, bodyStream, legalFact.length, LEGALFACTS_MEDIATYPE_STRING, metadata);
//            }
//        } catch (IOException exc) {
//            String errMsg = "Error while saving file on storage: " + fullFileKey + ".";
//            throw new PnInternalException(errMsg, exc);
//        }
//        String legalFactName = fullFileKey.replaceFirst("^.*/(.*)\\.pdf$", "$1");
//        return legalFactName + "~" + versionId;
//    }
//    
//    public String saveNotificationReceivedLegalFact(Action action, NotificationInt notification) {
//        Map<String, String> metadata = legalfactMetadataUtils.buildMetadata( LegalFactCategory.SENDER_ACK, null );
//        try {
//            byte[] pdfBytes = pdfUtils.generateNotificationReceivedLegalFact( notification);
//            return this.saveLegalFact(action.getIun(), "sender_ack", pdfBytes, metadata);
//        }
//        catch (IOException exc) {
//            throw new PnInternalException("", exc);
//        }
//    }
//    
//    public String saveNotificationReceivedLegalFact(NotificationInt notification) {
//        Map<String, String> metadata = legalfactMetadataUtils.buildMetadata(LegalFactCategory.SENDER_ACK, null);
//        try {
//            byte[] pdfBytes = pdfUtils.generateNotificationReceivedLegalFact(notification);
//            return this.saveLegalFact(notification.getIun(), "sender_ack", pdfBytes, metadata);
//        }
//        catch ( IOException exc ) {
//            throw new PnInternalException( "", exc );
//        }
//    }
//    
//
//    public String savePecDeliveryWorkflowLegalFact(List<Action> actions, NotificationInt notification, NotificationPathChooseDetails addresses) {
//        Set<Integer> recipientIdx = actions.stream()
//                .map(Action::getRecipientIndex)
//                .collect(Collectors.toSet());
//        if (recipientIdx.size() > 1) {
//            throw new PnInternalException("Impossible generate distinct act for distinct recipients");
//        }
//
//        String taxId = notification.getRecipients().get(recipientIdx.iterator().next()).getTaxId();
//        Map<String, String> metadata = legalfactMetadataUtils.buildMetadata(LegalFactCategory.DIGITAL_DELIVERY, taxId);
//
//        try {
//            byte[] pdfBytes = pdfUtils.generatePecDeliveryWorkflowLegalFact(actions, notification, addresses);
//            return this.saveLegalFact(notification.getIun(), "digital_delivery_info_" + taxId, pdfBytes, metadata);
//        }
//        catch(IOException exc) {
//            throw new PnInternalException( "", exc );
//        }
//    }
//
//    public String savePecDeliveryWorkflowLegalFact(List<SendDigitalFeedback> listFeedbackFromExtChannel, NotificationInt notification, NotificationRecipientInt recipient) {
//        Map<String, String> metadata = legalfactMetadataUtils.buildMetadata(LegalFactCategory.DIGITAL_DELIVERY, recipient.getTaxId());
//
//        try {
//            byte[] pdfBytes = pdfUtils.generatePecDeliveryWorkflowLegalFact(listFeedbackFromExtChannel, notification, recipient);
//            return this.saveLegalFact(notification.getIun(), "digital_delivery_info_" + recipient.getTaxId(), pdfBytes, metadata);
//        } catch ( IOException exc) {
//            throw new PnInternalException( "", exc );
//        }
//    }
//
//
//    public String saveNotificationViewedLegalFact(NotificationInt notification, NotificationRecipientInt recipient, Instant timeStamp) {
//        String taxId = recipient.getTaxId();
//        Map<String, String> metadata = legalfactMetadataUtils.buildMetadata(LegalFactCategory.RECIPIENT_ACCESS, taxId);
//        try {
//            byte[] pdfBytes = pdfUtils.generateNotificationViewedLegalFact(notification.getIun(), recipient, timeStamp);
//            return this.saveLegalFact(notification.getIun(), "notification_viewed_" + taxId, pdfBytes, metadata);
//        } catch (IOException exc) {
//            throw new PnInternalException( "", exc );
//        }
//    }
//    
//}
