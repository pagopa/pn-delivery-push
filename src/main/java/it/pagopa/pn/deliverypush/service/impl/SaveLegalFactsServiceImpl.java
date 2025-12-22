package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.safestorage.FileCreationWithContentRequest;
import it.pagopa.pn.deliverypush.dto.mandate.DelegateInfoInt;
import it.pagopa.pn.deliverypush.legalfacts.LegalFactGenerator;
import it.pagopa.pn.deliverypush.service.SafeStorageService;
import it.pagopa.pn.deliverypush.service.SaveLegalFactsService;
import it.pagopa.pn.deliverypush.service.utils.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Slf4j
@Service
public class SaveLegalFactsServiceImpl implements SaveLegalFactsService {

    public static final String LEGALFACTS_MEDIATYPE_STRING = "application/pdf";
    public static final String PN_LEGAL_FACTS = "PN_LEGAL_FACTS";
    public static final String SAVED = "SAVED";

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

}
