package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.deliverypush.action.utils.EndWorkflowStatus;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.safestorage.FileCreationWithContentRequest;
import it.pagopa.pn.deliverypush.dto.legalfacts.AARInfo;
import it.pagopa.pn.deliverypush.dto.legalfacts.PdfInfo;
import it.pagopa.pn.deliverypush.dto.mandate.DelegateInfoInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendDigitalFeedbackDetailsInt;
import it.pagopa.pn.deliverypush.legalfacts.LegalFactGenerator;
import it.pagopa.pn.deliverypush.service.SafeStorageService;
import it.pagopa.pn.deliverypush.service.SaveLegalFactsService;
import it.pagopa.pn.deliverypush.service.utils.FileUtils;
import it.pagopa.pn.deliverypush.utils.LegalFactUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_SAVELEGALFACTSFAILED;

@Slf4j
@Service
public class SaveLegalFactsServiceImpl implements SaveLegalFactsService {

    public static final String SAVE_LEGAL_FACT_EXCEPTION_MESSAGE = "Generating %s legal fact for IUN=%s and recipientId=%s";
    public static final String LEGALFACTS_MEDIATYPE_STRING = "application/pdf";
    public static final String PN_LEGAL_FACTS = "PN_LEGAL_FACTS";
    public static final String SAVED = "SAVED";
    public static final String PN_AAR = "PN_AAR";

    private final LegalFactGenerator legalFactBuilder;

    private final SafeStorageService safeStorageService;

    public SaveLegalFactsServiceImpl(LegalFactGenerator legalFactBuilder,
                                     SafeStorageService safeStorageService) {
        this.legalFactBuilder = legalFactBuilder;
        this.safeStorageService = safeStorageService;
    }

    public Mono<String> saveLegalFact(byte[] legalFact) {
        FileCreationWithContentRequest fileCreationRequest = new FileCreationWithContentRequest();
        fileCreationRequest.setContentType(LEGALFACTS_MEDIATYPE_STRING);
        fileCreationRequest.setDocumentType(PN_LEGAL_FACTS);
        fileCreationRequest.setStatus(SAVED);
        fileCreationRequest.setContent(legalFact);
        
        return safeStorageService.createAndUploadContent(fileCreationRequest)
                .map( fileCreationResponse -> FileUtils.getKeyWithStoragePrefix(fileCreationResponse.getKey()));
    }

    public PdfInfo sendCreationRequestForAAR(NotificationInt notification,
                                             NotificationRecipientInt recipient,
                                             String quickAccessToken) {
        try {
            log.debug("Start sendCreationRequestForAAR - iun={}", notification.getIun());

            AARInfo aarInfo = legalFactBuilder.generateNotificationAAR(notification, recipient, quickAccessToken);
            int numberOfPages = LegalFactUtils.getNumberOfPageFromPdfBytes(aarInfo.getBytesArrayGeneratedAar());

            FileCreationWithContentRequest fileCreationRequest = new FileCreationWithContentRequest();
            fileCreationRequest.setContentType(LEGALFACTS_MEDIATYPE_STRING);
            fileCreationRequest.setDocumentType(PN_AAR);
            fileCreationRequest.setStatus(SAVED);
            fileCreationRequest.setContent(aarInfo.getBytesArrayGeneratedAar());

            return MDCUtils.addMDCToContextAndExecute(
                 safeStorageService.createAndUploadContent(fileCreationRequest).map( fileCreationResponse ->{
                            PdfInfo pdfInfo = PdfInfo.builder()
                                    .key(FileUtils.getKeyWithStoragePrefix(fileCreationResponse.getKey()))
                                    .numberOfPages(numberOfPages)
                                    .aarTemplateType(aarInfo.getTemplateType())
                                    .build();
    
                            log.debug("End sendCreationRequestForAAR - iun={} key={}", notification.getIun(), fileCreationResponse);
    
                            return pdfInfo;
                        }
                )
            ).block();
            
        } catch (Exception exc) {
            String msg = String.format(SAVE_LEGAL_FACT_EXCEPTION_MESSAGE, "AAR", notification.getIun(), "N/A");
            log.error("Exception in sendCreationRequestForAAR ex=", exc);
            throw new PnInternalException(msg, ERROR_CODE_DELIVERYPUSH_SAVELEGALFACTSFAILED, exc);
        }
    }

    public String sendCreationRequestForNotificationReceivedLegalFact(NotificationInt notification) {
        try {
            log.debug("Start sendCreationRequestForNotificationReceivedLegalFact - iun={}", notification.getIun());

            return MDCUtils.addMDCToContextAndExecute(
                    this.saveLegalFact(legalFactBuilder.generateNotificationReceivedLegalFact(notification))
                            .map( responseUrl -> {
                                log.debug("sendCreationRequestForNotificationReceivedLegalFact completed with fileKey={} - iun={}", responseUrl, notification.getIun());
                                return responseUrl;
                            })
            ).block();
            
        } catch (Exception exc) {
            String msg = String.format(SAVE_LEGAL_FACT_EXCEPTION_MESSAGE, "REQUEST_ACCEPTED", notification.getIun(), "N/A");
            log.error("Exception in saveNotificationReceivedLegalFact ex=", exc);
            throw new PnInternalException(msg, ERROR_CODE_DELIVERYPUSH_SAVELEGALFACTSFAILED, exc);
        }

    }

    public String sendCreationRequestForNotificationCancelledLegalFact(NotificationInt notification, Instant notificationCancellationRequestDate) {
        try {
            log.debug("Start sendCreationRequestForNotificationCancelledLegalFact - iun={}", notification.getIun());

            return MDCUtils.addMDCToContextAndExecute(
                    this.saveLegalFact(legalFactBuilder.generateNotificationCancelledLegalFact(notification, notificationCancellationRequestDate))
                            .map( responseUrl -> {
                                log.debug("sendCreationRequestForNotificationCancelledLegalFact completed with fileKey={} - iun={}", responseUrl, notification.getIun());
                                return responseUrl;
                            })
            ).block();

        } catch (Exception exc) {
            String msg = String.format(SAVE_LEGAL_FACT_EXCEPTION_MESSAGE, "NOTIFICATION_CANCELLED", notification.getIun(), "N/A");
            log.error("Exception in sendCreationRequestForNotificationCancelledLegalFact ex=", exc);
            throw new PnInternalException(msg, ERROR_CODE_DELIVERYPUSH_SAVELEGALFACTSFAILED, exc);
        }

    }

    public String sendCreationRequestForPecDeliveryWorkflowLegalFact(
            List<SendDigitalFeedbackDetailsInt> listFeedbackFromExtChannel,
            NotificationInt notification,
            NotificationRecipientInt recipient,
            EndWorkflowStatus status,
            Instant completionWorkflowDate
    ) {
        try {
            log.debug("Start sendCreationRequestForPecDeliveryWorkflowLegalFact - iun={}", notification.getIun());

            return MDCUtils.addMDCToContextAndExecute(
                    this.saveLegalFact(legalFactBuilder.generatePecDeliveryWorkflowLegalFact(
                                    listFeedbackFromExtChannel, notification, recipient, status, completionWorkflowDate))
                            .map( responseUrl -> {
                                log.debug("End sendCreationRequestForPecDeliveryWorkflowLegalFact - iun={} key={}", notification.getIun(), responseUrl);
                                return responseUrl;
                            })
            ).block();
        } catch (Exception exc) {
            String msg = String.format(SAVE_LEGAL_FACT_EXCEPTION_MESSAGE, "DIGITAL_DELIVERY", notification.getIun(), recipient.getTaxId());
            throw new PnInternalException(msg, ERROR_CODE_DELIVERYPUSH_SAVELEGALFACTSFAILED, exc);
        }
    }


    public String sendCreationRequestForAnalogDeliveryFailureWorkflowLegalFact(
            NotificationInt notification,
            NotificationRecipientInt recipient,
            EndWorkflowStatus status,
            Instant failureWorkflowDate
    ) {
        try {
            log.debug("Start sendCreationRequestForAnalogDeliveryFailureWorkflowLegalFact - iun={}", notification.getIun());


            return MDCUtils.addMDCToContextAndExecute(
                    this.saveLegalFact(legalFactBuilder.generateAnalogDeliveryFailureWorkflowLegalFact(
                                    notification, recipient, status, failureWorkflowDate))
                            .map( responseUrl -> {
                                log.debug("End sendCreationRequestForAnalogDeliveryFailureWorkflowLegalFact - iun={} key={}", notification.getIun(), responseUrl);
                                return responseUrl;
                            })
            ).block();
        } catch (Exception exc) {
            String msg = String.format(SAVE_LEGAL_FACT_EXCEPTION_MESSAGE, "ANALOG_FAILURE_DELIVERY", notification.getIun(), recipient.getTaxId());
            throw new PnInternalException(msg, ERROR_CODE_DELIVERYPUSH_SAVELEGALFACTSFAILED, exc);
        }
    }
    
    public Mono<String> sendCreationRequestForNotificationViewedLegalFact(
            NotificationInt notification,
            NotificationRecipientInt recipient,
            DelegateInfoInt delegateInfo,
            Instant timeStamp
    ) {
        log.info("sendCreationRequestForNotificationViewedLegalFact - iun={}", notification.getIun());

        return Mono.fromCallable(() -> legalFactBuilder.generateNotificationViewedLegalFact(notification.getIun(), recipient, delegateInfo, timeStamp, notification))
                .flatMap( res -> {
                        log.info("sendCreationRequestForNotificationViewedLegalFact completed - iun={} are not nulls={}", notification.getIun(), res != null);
                        return this.saveLegalFact(res)
                        .map( responseUrl -> {
                            log.debug("End sendCreationRequestForNotificationViewedLegalFact - iun={} key={}", notification.getIun(), responseUrl);
                            return responseUrl;
                        });
                }).doOnError( err -> log.error("Error in sendCreationRequestForNotificationViewedLegalFact - iun={} error=", notification.getIun(), err));
    }

    public String sendCreationRequestForAnalogDeliveryWorkflowTimeoutLegalFact(
            NotificationInt notification,
            NotificationRecipientInt recipient,
            PhysicalAddressInt physicalAddress,
            String sentAttemptMade,
            Instant timeoutDate
    ) {
        try {
            log.debug("Start sendCreationRequestForAnalogDeliveryWorkflowTimeoutLegalFact - iun={}", notification.getIun());


            return MDCUtils.addMDCToContextAndExecute(
                    this.saveLegalFact(legalFactBuilder.generateAnalogDeliveryWorkflowTimeoutLegalFact(notification, recipient, physicalAddress, sentAttemptMade, timeoutDate
                            ))
                            .map( responseUrl -> {
                                log.debug("End sendCreationRequestForAnalogDeliveryWorkflowTimeoutLegalFact - iun={} key={}", notification.getIun(), responseUrl);
                                return responseUrl;
                            })
            ).block();
        } catch (Exception exc) {
            String msg = String.format(SAVE_LEGAL_FACT_EXCEPTION_MESSAGE, "ANALOG_DELIVERY_TIMEOUT", notification.getIun(), recipient.getTaxId());
            throw new PnInternalException(msg, ERROR_CODE_DELIVERYPUSH_SAVELEGALFACTSFAILED, exc);
        }
    }

}
