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
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactCategory;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactDownloadMetadataResponse;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactListElement;
import it.pagopa.pn.deliverypush.service.GetLegalFactService;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.SafeStorageService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.service.mapper.LegalFactIdMapper;
import it.pagopa.pn.deliverypush.service.utils.FileNameUtils;
import it.pagopa.pn.deliverypush.utils.AuditLogUtils;
import it.pagopa.pn.deliverypush.utils.AuthUtils;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
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
    public LegalFactDownloadMetadataResponse getLegalFactMetadata(String iun,
                                                                  LegalFactCategory legalFactType,
                                                                  String legalfactId,
                                                                  String senderReceiverId,
                                                                  String mandateId) {

        log.debug( "GetLegalFactMetadata iun={} legalFactId={}", iun, legalfactId );

       
        NotificationInt notification = notificationService.getNotificationByIun(iun);
        authUtils.checkUserPaAndMandateAuthorization(notification, senderReceiverId, mandateId);

        PnAuditLogEventType eventType = AuditLogUtils.getAuditLogEventType(notification, senderReceiverId, mandateId);

        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(eventType, "getLegalFactMetadata iun={} legalFactId={} senderReceiverId={}", iun, legalfactId, senderReceiverId)
                .iun(iun)
                .build();
        logEvent.log();
            
        try {
            // la key è la legalfactid
            FileDownloadResponseInt fileDownloadResponse = safeStorageService.getFile(legalfactId, false).block();
            LegalFactDownloadMetadataResponse response = generateResponse(iun, legalFactType, legalfactId, fileDownloadResponse);
            String fileName = response.getFilename();
            String url = response.getUrl();
            String retryAfter = String.valueOf( response.getRetryAfter() );
            String message = LogUtils.createAuditLogMessageForDownloadDocument( fileName, url, retryAfter );
            logEvent.generateSuccess("getLegalFactMetadata iun={} legalFactId={} senderReceiverId={} {}",
                    iun, legalfactId, senderReceiverId, message).log();
            return response;
        } catch (Exception exc) {
            logEvent.generateFailure("Exception in getLegalFactMetadata exc={}", exc).log();
            throw exc;
        }
        
    }

    @NotNull
    private LegalFactDownloadMetadataResponse generateResponse(String iun, LegalFactCategory legalFactType, String legalfactId, FileDownloadResponseInt fileDownloadResponse) {
        LegalFactDownloadMetadataResponse response = new LegalFactDownloadMetadataResponse();
        response.setFilename( FileNameUtils.buildFileName(iun, legalFactType.getValue(), legalfactId, fileDownloadResponse.getContentType()));
        response.setContentLength(fileDownloadResponse.getContentLength());
        response.setRetryAfter(fileDownloadResponse.getDownload() != null ? fileDownloadResponse.getDownload().getRetryAfter() : null);
        response.setUrl(fileDownloadResponse.getDownload().getUrl());
        return response;
    }

    @Override
    @NotNull
    public List<LegalFactListElement> getLegalFacts(String iun, String senderReceiverId, String mandateId) {
        log.debug( "Retrieve timeline elements for iun={}", iun );
        Set<TimelineElementInternal> timelineElements = timelineService.getTimeline(iun, true);
        
        NotificationInt notification = notificationService.getNotificationByIun(iun);

        authUtils.checkUserPaAndMandateAuthorization(notification, senderReceiverId, mandateId);
        PnAuditLogEventType eventType = AuditLogUtils.getAuditLogEventType(notification, senderReceiverId, mandateId);

        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(eventType, "GetLegalFacts iun={} senderReceiverId={} mandateId={}", iun, senderReceiverId, mandateId)
                .iun(iun)
                .build();
        logEvent.log();

        try {
            List<LegalFactListElement> legalFacts = timelineElements
                    .stream()
                    .filter( timeEl -> timeEl.getLegalFactsIds() != null )
                    .sorted( Comparator.comparing(TimelineElementInternal::getTimestamp))
                    .flatMap( timeEl -> timeEl.getLegalFactsIds().stream().map(
                            lfId -> LegalFactListElement.builder()
                                    .taxId( readRecipientId( timeEl, notification ) )
                                    .iun( iun )
                                    .legalFactsId( LegalFactIdMapper.internalToExternal(lfId) )
                                    .build()
                    ))
                    .toList();
            
            log.debug("legalFacts List={}" ,legalFacts );

            logEvent.generateSuccess().log();
            
            return legalFacts;
        } catch (Exception exc) {
            logEvent.generateFailure("Exception in getLegalFact exc={}", exc).log();
            throw exc;
        }
    }

    private String readRecipientId( TimelineElementInternal timelineElement, NotificationInt notification ) {
        String recipientId = null;

        if (timelineElement != null) {
            TimelineElementDetailsInt details = timelineElement.getDetails();
            if ( details instanceof RecipientRelatedTimelineElementDetails recipientRelatedTimelineElementDetails) {

                int recIndex = recipientRelatedTimelineElementDetails.getRecIndex();
                NotificationRecipientInt recipient = notificationUtils.getRecipientFromIndex(notification, recIndex);
                recipientId = recipient.getTaxId();
            }
        }

        return recipientId;
    }



}
