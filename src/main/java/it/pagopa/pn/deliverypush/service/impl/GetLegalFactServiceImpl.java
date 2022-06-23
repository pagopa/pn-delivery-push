package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.FileDownloadResponse;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.RecipientRelatedTimelineElementDetails;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementDetailsInt;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.safestorage.PnSafeStorageClient;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.deliverypush.service.GetLegalFactService;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.service.mapper.LegalFactIdMapper;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

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

    public GetLegalFactServiceImpl(TimelineService timelineService,
                                   PnSafeStorageClient safeStorageClient,
                                   NotificationService notificationService,
                                   NotificationUtils notificationUtils) {
        this.timelineService = timelineService;
        this.safeStorageClient = safeStorageClient;
        this.notificationService = notificationService;
        this.notificationUtils = notificationUtils;
    }

    @Override
    @NotNull
    public List<LegalFactListElement> getLegalFacts(String iun) {
        log.debug( "Retrieve timeline elements for iun={}", iun );
        Set<TimelineElementInternal> timelineElements = timelineService.getTimeline(iun);
        NotificationInt notification = notificationService.getNotificationByIun(iun);
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

    public LegalFactDownloadMetadataResponse getLegalFactMetadata(String iun, LegalFactCategory legalFactType, String legalfactId) {
        log.debug( "getLegalFactMetadata for iun={} and legalfactId={}", iun, legalfactId );
        // la key è la legalfactid
        FileDownloadResponse fileDownloadResponse = safeStorageClient.getFile(legalfactId, false);
        LegalFactDownloadMetadataResponse response = new LegalFactDownloadMetadataResponse();

        response.setFilename(buildLegalFactFilename(iun, legalFactType, legalfactId));
        response.setContentLength(fileDownloadResponse.getContentLength());
        response.setRetryAfter(fileDownloadResponse.getDownload().getRetryAfter());
        response.setUrl(fileDownloadResponse.getDownload().getUrl());

        return response;
    }

    public ResponseEntity<Resource> getLegalfact(String iun, LegalFactCategory legalFactType, String legalfactId ) {
        log.debug( "getLegalfact for iun={} and legalfactId={}", iun, legalfactId );
        // la key è la legalfactid
        FileDownloadResponse fileDownloadResponse = safeStorageClient.getFile(legalfactId, false);

        try {
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + buildLegalFactFilename(iun, legalFactType, legalfactId))
                    .contentType(MediaType.parseMediaType(fileDownloadResponse.getContentType()))
                    .body( new UrlResource(fileDownloadResponse.getDownload().getUrl()) );
        } catch (Exception e) {
            log.error("cannot stream content", e);
            throw new PnInternalException("Cannot get legal fact content", e);
        }
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

}
