package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.FileDownloadResponse;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.RecipientRelatedTimelineElementDetails;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementDetailsInt;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactCategory;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactDownloadMetadataResponse;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactListElement;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.safestorage.PnSafeStorageClient;
import it.pagopa.pn.deliverypush.service.GetLegalFactService;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.service.mapper.LegalFactIdMapper;
import it.pagopa.pn.deliverypush.utils.AuthUtils;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.safestorage.PnSafeStorageClient.SAFE_STORAGE_URL_PREFIX;

@Service
@Slf4j
public class GetLegalFactServiceImpl implements GetLegalFactService {

    private final TimelineService timelineService;
    private final PnSafeStorageClient safeStorageClient;
    private final NotificationService notificationService;
    private final NotificationUtils notificationUtils;
    private final AuthUtils authUtils;
    
    public GetLegalFactServiceImpl(TimelineService timelineService,
                                PnSafeStorageClient safeStorageClient,
                                NotificationService notificationService,
                                NotificationUtils notificationUtils,
                                AuthUtils authUtils) {
        this.timelineService = timelineService;
        this.safeStorageClient = safeStorageClient;
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

        
        LegalFactDownloadMetadataResponse response = new LegalFactDownloadMetadataResponse();
       
            NotificationInt notification = notificationService.getNotificationByIun(iun);
            authUtils.checkUserAndMandateAuthorization(notification, senderReceiverId, mandateId);

            PnAuditLogEventType eventType = getAuditLogEventType(notification, senderReceiverId, mandateId);

            PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
            PnAuditLogEvent logEvent = auditLogBuilder
                    .before(eventType, "getLegalFactMetadata iun={} legafactId={} senderReceiverId={}", iun, legalfactId, senderReceiverId)
                    .iun(iun)
                    .build();
            logEvent.log();
            
        try {
            // la key è la legalfactid
            FileDownloadResponse fileDownloadResponse = safeStorageClient.getFile(legalfactId, false);

            response.setFilename(buildLegalFactFilename(iun, legalFactType, legalfactId));
            response.setContentLength(fileDownloadResponse.getContentLength());
            response.setRetryAfter(fileDownloadResponse.getDownload() != null ? fileDownloadResponse.getDownload().getRetryAfter() : null);
            response.setUrl(fileDownloadResponse.getDownload().getUrl());
            logEvent.generateSuccess().log();
        } catch (Exception exc) {
            logEvent.generateFailure("Exception in getLegalFactMetadata", exc.getMessage());
            throw exc;
        }
        
        return response;
    }
    
    @Override
    @NotNull
    public List<LegalFactListElement> getLegalFacts(String iun, String senderReceiverId, String mandateId) {
        log.debug( "Retrieve timeline elements for iun={}", iun );
        Set<TimelineElementInternal> timelineElements = timelineService.getTimeline(iun);
        
        NotificationInt notification = notificationService.getNotificationByIun(iun);

        authUtils.checkUserAndMandateAuthorization(notification, senderReceiverId, mandateId);
        PnAuditLogEventType eventType = getAuditLogEventType(notification, senderReceiverId, mandateId);

        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(eventType, "getLegalFacts iun={} senderReceiverId={}", iun, senderReceiverId)
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
                    .collect(Collectors.toList());
            
            log.debug( "legalFacts List={}" ,legalFacts );
            
            return legalFacts;

        } catch (Exception exc) {
            logEvent.generateFailure("Exception in getLegalFact", exc.getMessage());
            throw exc;
        }
    }

    private String readRecipientId( TimelineElementInternal timelineElement, NotificationInt notification ) {
        String recipientId = null;
        //TODO Verificare se è necessario restituire il taxId o se può bastare il recIndex

        if (timelineElement != null) {
            TimelineElementDetailsInt details = timelineElement.getDetails();
            if ( details instanceof RecipientRelatedTimelineElementDetails) {

                int recIndex = ((RecipientRelatedTimelineElementDetails) details).getRecIndex();
                NotificationRecipientInt recipient = notificationUtils.getRecipientFromIndex(notification, recIndex);
                recipientId = recipient.getTaxId();
            }
        }

        return recipientId;
    }

    /**
     * il nome, viene generato da iun, type e factid e per ora si suppone essere un pdf
     * @param iun iun
     * @param legalFactType fact type
     * @param legalfactId fact id
     * @return filename
     */
    private String buildLegalFactFilename(String iun, LegalFactCategory legalFactType, String legalfactId)
    {
        return iun.replaceAll("[^a-zA-Z0-9]", "")
                + "_" + legalFactType.getValue()
                + "_" + legalfactId.replace(SAFE_STORAGE_URL_PREFIX, "").replaceAll("[^a-zA-Z0-9]", "")
                + ".pdf";
    }


    private PnAuditLogEventType getAuditLogEventType(NotificationInt notification, String senderRecipientId, String mandateId){
        if( StringUtils.hasText( mandateId ) ){
            //La request è stata ricevuta da un delagato, generazione audit log per destinatario
            return PnAuditLogEventType.AUD_NT_LEGALOPEN_RCP;
        } else {
            String paId = notification.getSender().getPaId();
            boolean isRequestFromSender = paId.equals(senderRecipientId);

            //Viene verificato se la richiesta proviene dalla Pa indicata nella notifica
            if( isRequestFromSender ){
                //La request è stata ricevuta dalla PA, generazione audit log per sender
                return PnAuditLogEventType.AUD_NT_LEGALOPEN_SND;
            }else {
                boolean isRequestFromRecipient = notification.getRecipients().stream().anyMatch(
                        recipient -> recipient.getInternalId().equals(senderRecipientId)
                );

                if( isRequestFromRecipient ){
                    //La request è stata ricevuta dal recipient, generazione audit log per recipient
                    return PnAuditLogEventType.AUD_NT_LEGALOPEN_RCP;
                }else {
                    throw new PnInternalException("Request is not from any authorized user. The audit log type cannot be determined");
                }
            }
        }
    }

}
