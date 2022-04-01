package it.pagopa.pn.deliverypush.legalfacts;

import it.pagopa.pn.api.dto.legalfacts.LegalFactType;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.timeline.NotificationPathChooseDetails;
import it.pagopa.pn.api.dto.notification.timeline.SendDigitalFeedback;
import it.pagopa.pn.commons.abstractions.FileStorage;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Slf4j
public class LegalFactUtils {

    public static final String LEGALFACTS_MEDIATYPE_STRING = "application/pdf";
    private final FileStorage fileStorage;
    private final LegalFactPdfGenerator pdfUtils;
    private final LegalfactsMetadataUtils legalfactMetadataUtils;


    public LegalFactUtils(FileStorage fileStorage,
                          LegalFactPdfGenerator pdfUtils,
                          LegalfactsMetadataUtils legalfactMetadataUtils
    ) {
        this.fileStorage = fileStorage;
        this.pdfUtils = pdfUtils;
        this.legalfactMetadataUtils = legalfactMetadataUtils;
    }

    public String saveLegalFact(String iun, String name, byte[] legalFact, Map<String, String> metadata) {
        String fullFileKey = legalfactMetadataUtils.fullKey(iun, name);
        String versionId;
        try {
            try (InputStream bodyStream = new ByteArrayInputStream(legalFact)) {
                versionId = fileStorage.putFileVersion(fullFileKey, bodyStream, legalFact.length, LEGALFACTS_MEDIATYPE_STRING, metadata);
            }
        } catch (IOException exc) {
            String errMsg = "Error while saving file on storage: " + fullFileKey + ".";
            throw new PnInternalException(errMsg, exc);
        }
        String legalFactName = fullFileKey.replaceFirst("^.*/(.*)\\.pdf$", "$1");
        return legalFactName + "~" + versionId;
    }
    
    public String saveNotificationReceivedLegalFact(Action action, Notification notification) {
        Map<String, String> metadata = legalfactMetadataUtils.buildMetadata( LegalFactType.SENDER_ACK, null );
        byte[] pdfBytes = pdfUtils.generateNotificationReceivedLegalFact( action, notification);
        return this.saveLegalFact(action.getIun(), "sender_ack", pdfBytes, metadata);
    }
    
    public String saveNotificationReceivedLegalFact(Notification notification) {
        Map<String, String> metadata = legalfactMetadataUtils.buildMetadata(LegalFactType.SENDER_ACK, null);
        byte[] pdfBytes = pdfUtils.generateNotificationReceivedLegalFact(notification);
        return this.saveLegalFact(notification.getIun(), "sender_ack", pdfBytes, metadata);
    }

    public String savePecDeliveryWorkflowLegalFact(List<Action> actions, Notification notification, NotificationPathChooseDetails addresses) {
        Set<Integer> recipientIdx = actions.stream()
                .map(Action::getRecipientIndex)
                .collect(Collectors.toSet());
        if (recipientIdx.size() > 1) {
            throw new PnInternalException("Impossible generate distinct act for distinct recipients");
        }

        String taxId = notification.getRecipients().get(recipientIdx.iterator().next()).getTaxId();
        Map<String, String> metadata = legalfactMetadataUtils.buildMetadata(LegalFactType.DIGITAL_DELIVERY, taxId);

        byte[] pdfBytes = pdfUtils.generatePecDeliveryWorkflowLegalFact(actions, notification, addresses);
        return this.saveLegalFact(notification.getIun(), "digital_delivery_info_" + taxId, pdfBytes, metadata);
    }

    public String savePecDeliveryWorkflowLegalFact(List<SendDigitalFeedback> listFeedbackFromExtChannel, Notification notification, NotificationRecipient recipient) {
        Map<String, String> metadata = legalfactMetadataUtils.buildMetadata(LegalFactType.DIGITAL_DELIVERY, recipient.getTaxId());

        byte[] pdfBytes = pdfUtils.generatePecDeliveryWorkflowLegalFact(listFeedbackFromExtChannel, notification, recipient);
        return this.saveLegalFact(notification.getIun(), "digital_delivery_info_" + recipient.getTaxId(), pdfBytes, metadata);
    }


    public String saveNotificationViewedLegalFact(Notification notification, NotificationRecipient recipient, Instant timeStamp) {
        String taxId = recipient.getTaxId();
        Map<String, String> metadata = legalfactMetadataUtils.buildMetadata(LegalFactType.RECIPIENT_ACCESS, taxId);
        byte[] pdfBytes = pdfUtils.generateNotificationViewedLegalFact(notification.getIun(), recipient, timeStamp);
        return this.saveLegalFact(notification.getIun(), "notification_viewed_" + taxId, pdfBytes, metadata);
    }
    
}
