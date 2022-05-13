package it.pagopa.pn.deliverypush.legalfacts;

import it.pagopa.pn.commons.abstractions.FileStorage;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactCategory;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.SendDigitalFeedback;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;

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

    String saveLegalFact(String iun, String name, byte[] legalFact, Map<String, String> metadata) throws IOException {
        String key = legalFactMetadataUtils.fullKey(iun, name);
        try (InputStream bodyStream = new ByteArrayInputStream(legalFact)) {
            fileStorage.putFileVersion(key, bodyStream, legalFact.length, LEGALFACTS_MEDIATYPE_STRING, metadata);
        }
        return key;
    }
    
/*x
    @Deprecated
    public String saveNotificationReceivedLegalFact(Action action, NotificationInt notification) {
        return saveNotificationReceivedLegalFact( notification );
    }*/
    
    public String saveNotificationReceivedLegalFact(NotificationInt notification) {
        try {
            Map<String, String> metadata = legalFactMetadataUtils.buildMetadata(
                    LegalFactCategory.SENDER_ACK, null
                );
            byte[] pdfBytes = legalFactBuilder.generateNotificationReceivedLegalFact(notification);
            return this.saveLegalFact(notification.getIun(), "sender_ack", pdfBytes, metadata);
        }
        catch ( IOException exc ) {
            String msg = String.format(SAVE_LEGAL_FACT_EXCEPTION_MESSAGE, "REQUEST_ACCEPTED",  notification.getIun(), "N/A");
            throw new PnInternalException( msg, exc);
        }
    }
    
    /*
    @Deprecated
    public String savePecDeliveryWorkflowLegalFact(List<Action> actions, NotificationInt notification, NotificationPathChooseDetails addresses) {
        Set<Integer> recipientIdx = actions.stream()
                .map(Action::getRecipientIndex)
                .collect(Collectors.toSet());
        if (recipientIdx.size() > 1) {
            throw new PnInternalException("Impossible generate unique act for distinct recipients");
        }

        String taxId = notification.getRecipients().get(recipientIdx.iterator().next()).getTaxId();

        try {
            Map<String, String> metadata = legalFactMetadataUtils.buildMetadata(
                    LegalFactCategory.DIGITAL_DELIVERY, taxId
                );

            byte[] pdfBytes = legalFactBuilder.generatePecDeliveryWorkflowLegalFact(
                    actions,
                    notification,
                    addresses
                );
            return this.saveLegalFact(notification.getIun(), "digital_delivery_info_" + taxId, pdfBytes, metadata);
        }
        catch ( IOException exc ) {
            String msg = String.format(SAVE_LEGAL_FACT_EXCEPTION_MESSAGE, "DIGITAL_DELIVERY",  notification.getIun(), taxId);
            throw new PnInternalException(  msg, exc);
        }
    }*/

    public String savePecDeliveryWorkflowLegalFact(
            List<SendDigitalFeedback> listFeedbackFromExtChannel,
            NotificationInt notification,
            NotificationRecipientInt recipient
    ) {

        try {
            Map<String, String> metadata = legalFactMetadataUtils.buildMetadata(
                    LegalFactCategory.DIGITAL_DELIVERY, recipient.getTaxId()
                );

            byte[] pdfBytes = legalFactBuilder.generatePecDeliveryWorkflowLegalFact(
                                        listFeedbackFromExtChannel, notification, recipient);
            return this.saveLegalFact(notification.getIun(), "digital_delivery_info_" + recipient.getTaxId(), pdfBytes, metadata);
        }
        catch ( IOException exc ) {
            String msg = String.format(SAVE_LEGAL_FACT_EXCEPTION_MESSAGE, "DIGITAL_DELIVERY",  notification.getIun(), recipient.getTaxId());
            throw new PnInternalException( msg, exc);
        }
    }
    
    /*
    @Deprecated
    public String saveNotificationViewedLegalFact(Action action, NotificationInt notification) {
        Integer recipientIndex = action.getRecipientIndex();
        NotificationRecipientInt recipient = notification.getRecipients().get( recipientIndex );

        return saveNotificationViewedLegalFact( notification, recipient, action.getNotBefore() );
    }*/

    public String saveNotificationViewedLegalFact(
            NotificationInt notification,
            NotificationRecipientInt recipient,
            Instant timeStamp
    ) {
        try {
            String taxId = recipient.getTaxId();
            Map<String, String> metadata = legalFactMetadataUtils.buildMetadata(
                    LegalFactCategory.RECIPIENT_ACCESS, taxId
                );
            byte[] pdfBytes = legalFactBuilder.generateNotificationViewedLegalFact(
                                                   notification.getIun(), recipient, timeStamp);
            return this.saveLegalFact(notification.getIun(), "notification_viewed_" + taxId, pdfBytes, metadata);
        }
        catch ( IOException exc ) {
            String msg = String.format(SAVE_LEGAL_FACT_EXCEPTION_MESSAGE, "NOTIFICATION_VIEWED",  notification.getIun(), recipient.getTaxId());
            throw new PnInternalException( msg, exc);
        }
    }
    
}
