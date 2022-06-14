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

import static it.pagopa.pn.deliverypush.externalclient.pnclient.safestorage.PnSafeStorageClient.SAFE_STORAGE_URL_PREFIX;

@Component
public class LegalFactDao {

    private static final String SAVE_LEGAL_FACT_EXCEPTION_MESSAGE = "Generating %s legal fact for IUN=%s and recipientId=%s";
    public static final String LEGALFACTS_MEDIATYPE_STRING = "application/pdf";
    public static final String PN_LEGAL_FACTS = "PN_LEGAL_FACTS";
    public static final String SAVED = "SAVED";
    public static final String PN_AAR = "PN_AAR";

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
        fileCreationRequest.setDocumentType(PN_LEGAL_FACTS);
        fileCreationRequest.setStatus(SAVED);
        fileCreationRequest.setContent(legalFact);
        FileCreationResponse fileCreationResponse = safeStorageClient.createAndUploadContent(fileCreationRequest);


        return SAFE_STORAGE_URL_PREFIX + fileCreationResponse.getKey();
    }

    public String saveAAR(NotificationInt notification,
                          NotificationRecipientInt recipient) {
        try {
            FileCreationWithContentRequest fileCreationRequest = new FileCreationWithContentRequest();
            fileCreationRequest.setContentType(LEGALFACTS_MEDIATYPE_STRING);
            fileCreationRequest.setDocumentType(PN_AAR);
            fileCreationRequest.setStatus(SAVED);
            fileCreationRequest.setContent(legalFactBuilder.generateNotificationAAR(notification, recipient));
            FileCreationResponse fileCreationResponse = safeStorageClient.createAndUploadContent(fileCreationRequest);

            return SAFE_STORAGE_URL_PREFIX + fileCreationResponse.getKey();
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
