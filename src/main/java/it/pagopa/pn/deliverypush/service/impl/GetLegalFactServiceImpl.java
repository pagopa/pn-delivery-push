package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.commons.utils.LogUtils;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.safestorage.FileDownloadResponseInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.RecipientRelatedTimelineElementDetails;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementDetailsInt;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.deliverypush.service.GetLegalFactService;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.SafeStorageService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.service.mapper.LegalFactIdMapper;
import it.pagopa.pn.deliverypush.service.utils.FileUtils;
import it.pagopa.pn.deliverypush.utils.AuditLogUtils;
import it.pagopa.pn.deliverypush.utils.AuthUtils;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@Slf4j
public class GetLegalFactServiceImpl implements GetLegalFactService {

    private final TimelineService timelineService;
    private final SafeStorageService safeStorageService;
    private final NotificationService notificationService;
    private final NotificationUtils notificationUtils;
    private final AuthUtils authUtils;

    public GetLegalFactServiceImpl(TimelineService timelineService,
                                   SafeStorageService safeStorageService,
                                   NotificationService notificationService,
                                   NotificationUtils notificationUtils,
                                   AuthUtils authUtils) {
        this.timelineService = timelineService;
        this.safeStorageService = safeStorageService;
        this.notificationService = notificationService;
        this.notificationUtils = notificationUtils;
        this.authUtils = authUtils;
    }

    @Override
    public Mono<LegalFactDownloadMetadataResponse> getLegalFactMetadata(String iun,
                                                                        LegalFactCategory legalFactType,
                                                                        String legalfactId,
                                                                        String senderReceiverId,
                                                                        String mandateId,
                                                                        CxTypeAuthFleet cxType,
                                                                        List<String> cxGroups) {
        return downloadLegalFact(iun, legalFactType, legalfactId, senderReceiverId, mandateId, cxType, cxGroups)
                .map(fileDownloadResponse -> generateResponse(iun, legalFactType, legalfactId, fileDownloadResponse));
    }
    @Override
    public Mono<LegalFactDownloadMetadataWithContentTypeResponse> getLegalFactMetadataWithContentType(String iun,
                                                                                                      LegalFactCategory legalFactType,
                                                                                                      String legalfactId,
                                                                                                      String senderReceiverId,
                                                                                                      String mandateId,
                                                                                                      CxTypeAuthFleet cxType,
                                                                                                      List<String> cxGroups) {
        return downloadLegalFact(iun, legalFactType, legalfactId, senderReceiverId, mandateId, cxType, cxGroups)
                .map(fileDownloadResponse -> generateResponseWithContentType(iun, legalFactType, legalfactId, fileDownloadResponse));

    }

    private Mono<FileDownloadResponseInt> downloadLegalFact(String iun,
                                                            LegalFactCategory legalFactType,
                                                            String legalfactId,
                                                            String senderReceiverId,
                                                            String mandateId,
                                                            CxTypeAuthFleet cxType,
                                                            List<String> cxGroups) {
        return Mono.fromCallable(() -> notificationService.getNotificationByIun(iun))
                .flatMap(notification -> 
                        Mono.fromRunnable(() -> authUtils.checkUserPaAndMandateAuthorization(notification, senderReceiverId, mandateId, cxType, cxGroups))
                        .then( Mono.just(notification) )
                )
                .map(notification -> {
                    PnAuditLogEvent logEvent = getAuditLog(iun, legalfactId, senderReceiverId, mandateId, notification);
                    logEvent.log();
                    return logEvent;
                })
                .flatMap(logEvent ->
                        // la key è la legalfactId
                        safeStorageService.getFile(legalfactId, false)
                                .onErrorResume(exc -> {
                                            logEvent.generateFailure("Exception in getLegalFactMetadata", exc).log();
                                            return Mono.error(exc);
                                        }
                                )
                                .doOnNext(fileDownloadResponse -> {
                                    String filename = FileUtils.buildFileName(iun, legalFactType == null ? null : legalFactType.getValue(), legalfactId, fileDownloadResponse.getContentType());
                                    String url = Objects.nonNull(fileDownloadResponse.getDownload()) ? fileDownloadResponse.getDownload().getUrl() : null;
                                    BigDecimal retryAfter = Objects.nonNull(fileDownloadResponse.getDownload()) ? fileDownloadResponse.getDownload().getRetryAfter() : null;
                                    generateSuccessAuditLog(iun, legalfactId, senderReceiverId, logEvent, filename, url, retryAfter);
                                })
                );
    }

    private PnAuditLogEvent getAuditLog(String iun, String legalfactId, String senderReceiverId, String mandateId, NotificationInt notification) {
        PnAuditLogEventType eventType = AuditLogUtils.getAuditLogEventType(notification, senderReceiverId, mandateId);
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        return auditLogBuilder
                .before(eventType, "getLegalFactMetadata iun={} legalFactId={} senderReceiverId={}", iun, legalfactId, senderReceiverId)
                .iun(iun)
                .build();
    }

    @NotNull
    private LegalFactDownloadMetadataResponse generateResponse(String iun, LegalFactCategory legalFactType, String legalfactId, FileDownloadResponseInt fileDownloadResponse) {
        LegalFactDownloadMetadataResponse response = new LegalFactDownloadMetadataResponse();
        response.setFilename(FileUtils.buildFileName(iun, legalFactType == null ? null : legalFactType.getValue(), legalfactId, fileDownloadResponse.getContentType()));
        response.setContentLength(fileDownloadResponse.getContentLength());
        response.setRetryAfter(fileDownloadResponse.getDownload() != null ? fileDownloadResponse.getDownload().getRetryAfter() : null);
        response.setUrl(fileDownloadResponse.getDownload().getUrl());
        return response;
    }
    @NotNull
    private LegalFactDownloadMetadataWithContentTypeResponse generateResponseWithContentType(String iun, LegalFactCategory legalFactType, String legalfactId, FileDownloadResponseInt fileDownloadResponse) {
        LegalFactDownloadMetadataWithContentTypeResponse response = new LegalFactDownloadMetadataWithContentTypeResponse();
        response.setFilename(FileUtils.buildFileName(iun, legalFactType == null ? null : legalFactType.getValue(), legalfactId, fileDownloadResponse.getContentType()));
        response.setContentLength(fileDownloadResponse.getContentLength());
        response.setContentType(fileDownloadResponse.getContentType());
        response.setRetryAfter(fileDownloadResponse.getDownload() != null ? fileDownloadResponse.getDownload().getRetryAfter() : null);
        response.setUrl(fileDownloadResponse.getDownload().getUrl());
        return response;
    }

    private void generateSuccessAuditLog(String iun, String legalfactId, String senderReceiverId, PnAuditLogEvent logEvent, String fileName, String url, BigDecimal retryAfter) {
        String retryAfterStr = String.valueOf(retryAfter);
        String message = LogUtils.createAuditLogMessageForDownloadDocument(fileName, url, retryAfterStr);
        logEvent.generateSuccess("getLegalFactMetadata iun={} legalFactId={} senderReceiverId={} {}",
                iun, legalfactId, senderReceiverId, message).log();
    }

    @Override
    @NotNull
    public List<LegalFactListElement> getLegalFacts(String iun, String senderReceiverId, String mandateId, CxTypeAuthFleet cxType, List<String> cxGroups) {
        log.debug("Retrieve timeline elements for iun={}", iun);
        Set<TimelineElementInternal> timelineElements = timelineService.getTimeline(iun, true);

        NotificationInt notification = notificationService.getNotificationByIun(iun);

        String recipientId = authUtils.checkUserPaAndMandateAuthorizationAndRetrieveRealRecipientId(notification, senderReceiverId, mandateId, cxType, cxGroups);
        PnAuditLogEventType eventType = AuditLogUtils.getAuditLogEventType(notification, senderReceiverId, mandateId);

        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(eventType, "GetLegalFacts iun={} senderReceiverId={} mandateId={} resolvedRecipientId={}", iun, senderReceiverId, mandateId, recipientId)
                .iun(iun)
                .build();
        logEvent.log();

        try {
            List<LegalFactListElement> legalFacts = timelineElements
                    .stream()
                    .filter(timeEl -> timeEl.getLegalFactsIds() != null)
                    .filter(timeEl -> checkIfLegalFactCanBeViewed(notification, recipientId, cxType, timeEl))
                    .sorted(Comparator.comparing(TimelineElementInternal::getTimestamp))
                    .flatMap(timeEl -> timeEl.getLegalFactsIds().stream().map(
                            lfId -> LegalFactListElement.builder()
                                    .taxId(readRecipientId(timeEl, notification))
                                    .iun(iun)
                                    .legalFactsId(LegalFactIdMapper.internalToExternal(lfId))
                                    .build()
                    ))
                    .toList();

            log.debug("legalFacts List={}", legalFacts);

            logEvent.generateSuccess().log();

            return legalFacts;
        } catch (Exception exc) {
            logEvent.generateFailure("Exception in getLegalFact", exc).log();
            throw exc;
        }
    }

    private boolean checkIfLegalFactCanBeViewed(NotificationInt notification, String senderReceiverId, CxTypeAuthFleet cxType, TimelineElementInternal timeEl) {
        if (cxType == CxTypeAuthFleet.PA)
            return true;
        else if (timeEl.getDetails() instanceof RecipientRelatedTimelineElementDetails recipientRelatedTimelineElementDetails) {
            return senderReceiverId.equals(notification.getRecipients().get(recipientRelatedTimelineElementDetails.getRecIndex()).getInternalId());
        } else
            return true;
    }

    private String readRecipientId(TimelineElementInternal timelineElement, NotificationInt notification) {
        String recipientId = null;

        if (timelineElement != null) {
            TimelineElementDetailsInt details = timelineElement.getDetails();
            if (details instanceof RecipientRelatedTimelineElementDetails recipientRelatedTimelineElementDetails) {

                int recIndex = recipientRelatedTimelineElementDetails.getRecIndex();
                NotificationRecipientInt recipient = notificationUtils.getRecipientFromIndex(notification, recIndex);
                recipientId = recipient.getTaxId();
            }
        }

        return recipientId;
    }
}
