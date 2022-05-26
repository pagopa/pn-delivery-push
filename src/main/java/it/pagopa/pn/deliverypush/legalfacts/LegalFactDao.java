package it.pagopa.pn.deliverypush.legalfacts;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.FileCreationResponse;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.externalclient.pnclient.safestorage.FileCreationWithContentRequest;
import it.pagopa.pn.deliverypush.externalclient.pnclient.safestorage.PnSafeStorageClient;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.SendDigitalFeedback;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

@Component
public class LegalFactDao {

    public static final String PN_NOTIFICATION_ATTACHMENTS = "PN_NOTIFICATION_ATTACHMENTS";
    public static final String PRELOADED = "PRELOADED";

    private static final String SAVE_LEGAL_FACT_EXCEPTION_MESSAGE = "Generating %s legal fact for IUN=%s and recipientId=%s";
    public static final String LEGALFACTS_MEDIATYPE_STRING = "application/pdf";
    private final LegalFactGenerator legalFactBuilder;

    private final PnSafeStorageClient safeStorageClient;

    public LegalFactDao(LegalFactGenerator legalFactBuilder,
                        PnSafeStorageClient safeStorageClient) {
        this.legalFactBuilder = legalFactBuilder;
        this.safeStorageClient = safeStorageClient;
    }

    String saveLegalFact(byte[] legalFact) {
        FileCreationWithContentRequest fileCreationRequest = new FileCreationWithContentRequest();
        fileCreationRequest.setContentType(LEGALFACTS_MEDIATYPE_STRING);
        fileCreationRequest.setDocumentType(PN_NOTIFICATION_ATTACHMENTS);
        fileCreationRequest.setStatus(PRELOADED);
        fileCreationRequest.setContent(legalFact);
        FileCreationResponse fileCreationResponse = safeStorageClient.createAndUploadContent(fileCreationRequest);

        return fileCreationResponse.getKey();
    }

/*x
    @Deprecated
    public String saveNotificationReceivedLegalFact(Action action, NotificationInt notification) {
        return saveNotificationReceivedLegalFact( notification );
    }*/

    public String saveAARLegalFact(NotificationInt notification) {
        try {
            return this.saveLegalFact(legalFactBuilder.generateNotificationAAR(notification));
        }
        catch ( IOException exc ) {
            String msg = String.format(SAVE_LEGAL_FACT_EXCEPTION_MESSAGE, "AAR",  notification.getIun(), "N/A");
            throw new PnInternalException( msg, exc);
        }
    }

    public String saveNotificationReceivedLegalFact(NotificationInt notification) {
        try {
            return this.saveLegalFact(legalFactBuilder.generateNotificationReceivedLegalFact(notification));
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
            return this.saveLegalFact(legalFactBuilder.generatePecDeliveryWorkflowLegalFact(
                    listFeedbackFromExtChannel, notification, recipient));
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
            return this.saveLegalFact(legalFactBuilder.generateNotificationViewedLegalFact(
                    notification.getIun(), recipient, timeStamp));
        }
        catch ( IOException exc ) {
            String msg = String.format(SAVE_LEGAL_FACT_EXCEPTION_MESSAGE, "NOTIFICATION_VIEWED",  notification.getIun(), recipient.getTaxId());
            throw new PnInternalException( msg, exc);
        }
    }
    
}
