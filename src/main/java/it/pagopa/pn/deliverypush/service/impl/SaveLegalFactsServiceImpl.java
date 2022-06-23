package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.FileCreationResponse;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.PdfInfo;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendDigitalFeedbackDetailsInt;
import it.pagopa.pn.deliverypush.legalfacts.LegalFactGenerator;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.safestorage.FileCreationWithContentRequest;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.safestorage.PnSafeStorageClient;
import it.pagopa.pn.deliverypush.service.SaveLegalFactsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import static it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.safestorage.PnSafeStorageClient.SAFE_STORAGE_URL_PREFIX;

@Slf4j
@Service
public class SaveLegalFactsServiceImpl implements SaveLegalFactsService {

    private static final String SAVE_LEGAL_FACT_EXCEPTION_MESSAGE = "Generating %s legal fact for IUN=%s and recipientId=%s";
    public static final String LEGALFACTS_MEDIATYPE_STRING = "application/pdf";
    public static final String PN_LEGAL_FACTS = "PN_LEGAL_FACTS";
    public static final String SAVED = "SAVED";
    public static final String PN_AAR = "PN_AAR";

    private final LegalFactGenerator legalFactBuilder;

    private final PnSafeStorageClient safeStorageClient;

    public SaveLegalFactsServiceImpl(LegalFactGenerator legalFactBuilder,
                                     PnSafeStorageClient safeStorageClient) {
        this.legalFactBuilder = legalFactBuilder;
        this.safeStorageClient = safeStorageClient;
    }

    public String saveLegalFact(byte[] legalFact) {
        FileCreationWithContentRequest fileCreationRequest = new FileCreationWithContentRequest();
        fileCreationRequest.setContentType(LEGALFACTS_MEDIATYPE_STRING);
        fileCreationRequest.setDocumentType(PN_LEGAL_FACTS);
        fileCreationRequest.setStatus(SAVED);
        fileCreationRequest.setContent(legalFact);
        FileCreationResponse fileCreationResponse = safeStorageClient.createAndUploadContent(fileCreationRequest);


        return SAFE_STORAGE_URL_PREFIX + fileCreationResponse.getKey();
    }

    public PdfInfo saveAAR(NotificationInt notification,
                           NotificationRecipientInt recipient) {
        try {
            log.debug("Start Save AAR - iun={}", notification.getIun());
            
            byte[] pdfByte = legalFactBuilder.generateNotificationAAR(notification, recipient);
            int numberOfPages = legalFactBuilder.getNumberOfPages(pdfByte);

            FileCreationWithContentRequest fileCreationRequest = new FileCreationWithContentRequest();
            fileCreationRequest.setContentType(LEGALFACTS_MEDIATYPE_STRING);
            fileCreationRequest.setDocumentType(PN_AAR);
            fileCreationRequest.setStatus(SAVED);
            fileCreationRequest.setContent(pdfByte);
            FileCreationResponse fileCreationResponse = safeStorageClient.createAndUploadContent(fileCreationRequest);

            log.debug("End Save AAR - iun={}", notification.getIun());

            return PdfInfo.builder()
                    .key( SAFE_STORAGE_URL_PREFIX + fileCreationResponse.getKey() )
                    .numberOfPages( numberOfPages )
                    .build();
        }
        catch ( IOException exc ) {
            String msg = String.format(SAVE_LEGAL_FACT_EXCEPTION_MESSAGE, "AAR",  notification.getIun(), "N/A");
            log.error("Exception in saveAAR ex=", exc);
            throw new PnInternalException( msg, exc);
        }
    }

    public String saveNotificationReceivedLegalFact(NotificationInt notification) {
        try {
            log.debug("Start saveNotificationReceivedLegalFact - iun={}", notification.getIun());

            String url = this.saveLegalFact(legalFactBuilder.generateNotificationReceivedLegalFact(notification));

            log.debug("End saveNotificationReceivedLegalFact - iun={}", notification.getIun());
            
            return url;
        }
        catch ( IOException exc ) {
            String msg = String.format(SAVE_LEGAL_FACT_EXCEPTION_MESSAGE, "REQUEST_ACCEPTED",  notification.getIun(), "N/A");
            log.error("Exception in saveNotificationReceivedLegalFact ex=", exc);
            throw new PnInternalException( msg, exc);
        }
    }

    public String savePecDeliveryWorkflowLegalFact(
            List<SendDigitalFeedbackDetailsInt> listFeedbackFromExtChannel,
            NotificationInt notification,
            NotificationRecipientInt recipient
    ) {
        try {
            log.debug("Start savePecDeliveryWorkflowLegalFact - iun={}", notification.getIun());

            String url = this.saveLegalFact(legalFactBuilder.generatePecDeliveryWorkflowLegalFact(
                    listFeedbackFromExtChannel, notification, recipient));

            log.debug("End savePecDeliveryWorkflowLegalFact - iun={}", notification.getIun());

            return url;
        }
        catch ( IOException exc ) {
            String msg = String.format(SAVE_LEGAL_FACT_EXCEPTION_MESSAGE, "DIGITAL_DELIVERY",  notification.getIun(), recipient.getTaxId());
            log.error("Exception in savePecDeliveryWorkflowLegalFact ex=", exc);
            throw new PnInternalException( msg, exc);
        }

    }

    public String saveNotificationViewedLegalFact(
            NotificationInt notification,
            NotificationRecipientInt recipient,
            Instant timeStamp
    ) {
        try {
            log.debug("Start saveNotificationViewedLegalFact - iun={}", notification.getIun());

            String url =  this.saveLegalFact(legalFactBuilder.generateNotificationViewedLegalFact(
                    notification.getIun(), recipient, timeStamp));

            log.debug("End saveNotificationViewedLegalFact - iun={}", notification.getIun());

            return url;
        }
        catch ( IOException exc ) {
            String msg = String.format(SAVE_LEGAL_FACT_EXCEPTION_MESSAGE, "NOTIFICATION_VIEWED",  notification.getIun(), recipient.getTaxId());
            log.error("Exception in saveNotificationViewedLegalFact ex=", exc);
            throw new PnInternalException( msg, exc);
        }
    }
}
