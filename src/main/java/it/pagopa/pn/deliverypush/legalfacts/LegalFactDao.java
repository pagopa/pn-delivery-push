package it.pagopa.pn.deliverypush.legalfacts;

import it.pagopa.pn.api.dto.legalfacts.LegalFactType;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.timeline.NotificationPathChooseDetails;
import it.pagopa.pn.api.dto.notification.timeline.SendDigitalFeedback;
import it.pagopa.pn.commons.abstractions.FileStorage;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons_delivery.utils.LegalfactsMetadataUtils;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
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
public class LegalFactDao {

    private final static String SAVE_LEGAL_FACT_EXCEPTION_MESSAGE = "Generating %s legal fact for IUN=%s and recipientId=%s";
    public static final String LEGALFACTS_MEDIATYPE_STRING = "application/pdf";
    private final FileStorage fileStorage;
    private final LegalFactGenerator legalFactBuilder;
    private final LegalfactsMetadataUtils legalFactMetadataUtils;

    public LegalFactDao(FileStorage fileStorage,
                        LegalFactGenerator legalFactBuilder,
                        LegalfactsMetadataUtils legalFactMetadataUtils
    ) {
        this.fileStorage = fileStorage;
        this.legalFactBuilder = legalFactBuilder;
        this.legalFactMetadataUtils = legalFactMetadataUtils;
    }

    void saveLegalFact(String iun, String name, byte[] legalFact, Map<String, String> metadata) throws IOException {
        String key = legalFactMetadataUtils.fullKey(iun, name);
        try (InputStream bodyStream = new ByteArrayInputStream(legalFact)) {
            fileStorage.putFileVersion(key, bodyStream, legalFact.length, LEGALFACTS_MEDIATYPE_STRING, metadata);
        }
    }

    @Deprecated
    public void saveNotificationReceivedLegalFact(Action action, Notification notification) {
        saveNotificationReceivedLegalFact( notification );
    }
    
    public void saveNotificationReceivedLegalFact(Notification notification) {
        try {
            Map<String, String> metadata = legalFactMetadataUtils.buildMetadata(
                    LegalFactType.SENDER_ACK, null
                );
            byte[] pdfBytes = legalFactBuilder.generateNotificationReceivedLegalFact(notification);
            this.saveLegalFact(notification.getIun(), "sender_ack", pdfBytes, metadata);
        }
        catch ( IOException exc ) {
            String msg = String.format(SAVE_LEGAL_FACT_EXCEPTION_MESSAGE, "REQUEST_ACCEPTED",  notification.getIun(), "N/A");
            throw new PnInternalException( msg, exc);
        }
    }

    @Deprecated
    public void savePecDeliveryWorkflowLegalFact(List<Action> actions, Notification notification, NotificationPathChooseDetails addresses) {
        Set<Integer> recipientIdx = actions.stream()
                .map(Action::getRecipientIndex)
                .collect(Collectors.toSet());
        if (recipientIdx.size() > 1) {
            throw new PnInternalException("Impossible generate unique act for distinct recipients");
        }

        String taxId = notification.getRecipients().get(recipientIdx.iterator().next()).getTaxId();

        try {
            Map<String, String> metadata = legalFactMetadataUtils.buildMetadata(
                    LegalFactType.DIGITAL_DELIVERY, taxId
                );

            byte[] pdfBytes = legalFactBuilder.generatePecDeliveryWorkflowLegalFact(
                    actions,
                    notification,
                    addresses
                );
            this.saveLegalFact(notification.getIun(), "digital_delivery_info_" + taxId, pdfBytes, metadata);
        }
        catch ( IOException exc ) {
            String msg = String.format(SAVE_LEGAL_FACT_EXCEPTION_MESSAGE, "DIGITAL_DELIVERY",  notification.getIun(), taxId);
            throw new PnInternalException(  msg, exc);
        }
    }

    public void savePecDeliveryWorkflowLegalFact(
            List<SendDigitalFeedback> listFeedbackFromExtChannel,
            Notification notification,
            NotificationRecipient recipient
    ) {

        try {
            Map<String, String> metadata = legalFactMetadataUtils.buildMetadata(
                    LegalFactType.DIGITAL_DELIVERY, recipient.getTaxId()
                );

            byte[] pdfBytes = legalFactBuilder.generatePecDeliveryWorkflowLegalFact(
                                        listFeedbackFromExtChannel, notification, recipient);
            this.saveLegalFact(notification.getIun(), "digital_delivery_info_" + recipient.getTaxId(), pdfBytes, metadata);
        }
        catch ( IOException exc ) {
            String msg = String.format(SAVE_LEGAL_FACT_EXCEPTION_MESSAGE, "DIGITAL_DELIVERY",  notification.getIun(), recipient.getTaxId());
            throw new PnInternalException( msg, exc);
        }
    }

    @Deprecated
    public void saveNotificationViewedLegalFact(Action action, Notification notification) {
        Integer recipientIndex = action.getRecipientIndex();
        NotificationRecipient recipient = notification.getRecipients().get( recipientIndex );

        saveNotificationViewedLegalFact( notification, recipient, action.getNotBefore() );
    }

    public void saveNotificationViewedLegalFact(
            Notification notification,
            NotificationRecipient recipient,
            Instant timeStamp
    ) {
        try {
            String taxId = recipient.getTaxId();
            Map<String, String> metadata = legalFactMetadataUtils.buildMetadata(
                    LegalFactType.RECIPIENT_ACCESS, taxId
                );
            byte[] pdfBytes = legalFactBuilder.generateNotificationViewedLegalFact(
                                                   notification.getIun(), recipient, timeStamp);
            this.saveLegalFact(notification.getIun(), "notification_viewed_" + taxId, pdfBytes, metadata);
        }
        catch ( IOException exc ) {
            String msg = String.format(SAVE_LEGAL_FACT_EXCEPTION_MESSAGE, "NOTIFICATION_VIEWED",  notification.getIun(), recipient.getTaxId());
            throw new PnInternalException( msg, exc);
        }
    }
    
}
