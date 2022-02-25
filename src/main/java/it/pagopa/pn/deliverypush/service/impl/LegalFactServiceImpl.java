package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.api.dto.legalfacts.LegalFactType;
import it.pagopa.pn.api.dto.legalfacts.LegalFactsListEntry;
import it.pagopa.pn.api.dto.legalfacts.LegalFactsListEntryId;
import it.pagopa.pn.api.dto.notification.NotificationAttachment;
import it.pagopa.pn.api.dto.notification.timeline.*;
import it.pagopa.pn.commons.abstractions.FileData;
import it.pagopa.pn.commons.abstractions.FileStorage;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons_delivery.middleware.TimelineDao;
import it.pagopa.pn.commons_delivery.utils.LegalfactsMetadataUtils;
import it.pagopa.pn.deliverypush.service.LegalFactService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class LegalFactServiceImpl implements LegalFactService {

    private TimelineDao timelineDao;

    public LegalFactServiceImpl(TimelineDao timelineDao) {  this.timelineDao = timelineDao; }
    @Override
    @NotNull
    public List<LegalFactsListEntry> getLegalFacts(String iun) {
        log.debug( "Retrieve timeline elements for iun={}", iun );
        Set<TimelineElement> timelineElements = timelineDao.getTimeline(iun);
        List<LegalFactsListEntry> legalFacts = timelineElements
                .stream()
                .filter( timeEl -> timeEl.getLegalFactsIds() != null )
                .sorted(  )
                .flatMap( timeEl -> timeEl.getLegalFactsIds().stream().map(
                        lfId -> LegalFactsListEntry.builder()
                                .taxId( readRecipientId( timeEl ) )
                                .iun( iun )
                                .legalFactsId( lfId )
                                .build()
                        ))
                .collect(Collectors.toList());
        log.debug( "legalFacts List={}" ,legalFacts );
        return legalFacts;
    }

    private String readRecipientId( TimelineElement  timelineElement ) {
        String recipientId = null;
        if (timelineElement != null) {
            TimelineElementDetails details = timelineElement.getDetails();
            if ( details != null && details instanceof RecipientRelatedTimelineElementDetails ) {
                recipientId = ((RecipientRelatedTimelineElementDetails) details).getTaxId();
            }
        }
        return recipientId;
    }

    // FIXME: Marco comincia da qua
    private final FileStorage fileStorage = null;
    private final LegalfactsMetadataUtils legalfactMetadataUtils = null;

    public static final String MISSING_EXT_CHA_ATTACHMENT_MESSAGE = "Unable to retrieve paper feedback for iun=%s with id=%s from external channel API";

    public ResponseEntity<Resource> loadAttachment(NotificationAttachment.Ref attachmentRef) {
        String attachmentKey = attachmentRef.getKey();
        String savedVersionId = attachmentRef.getVersionToken();

        FileData fileData = fileStorage.getFileVersion( attachmentKey, savedVersionId );

        ResponseEntity<Resource> response = ResponseEntity.ok()
                .headers( headers() )
                .contentLength( fileData.getContentLength() )
                .contentType( extractMediaType( fileData.getContentType() ) )
                .body( new InputStreamResource(fileData.getContent() ) );

        log.debug("AttachmentKey: response={}", response);
        return response;
    }

    private MediaType extractMediaType(String contentType ) {
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;

        try {
            if ( StringUtils.isNotBlank( contentType ) ) {
                mediaType = MediaType.parseMediaType( contentType );
            }
        } catch (InvalidMediaTypeException exc)  {
            // using default
        }
        return mediaType;
    }

    public ResponseEntity<Resource> loadLegalfact(String iun, LegalFactType type, String legalfactId ) {
        if ( LegalFactType.ANALOG_DELIVERY.equals( type ) ) {
            return getPaperFeedbackLegalFact(iun, legalfactId);
        } else {
            log.debug( "Retrieve notification attachment Ref for iun={} and legalfactId={}", iun, legalfactId );
            NotificationAttachment.Ref ref = legalfactMetadataUtils.fromIunAndLegalFactId( iun, legalfactId );
            return loadAttachment( ref );
        }
    }

    @NotNull
    private ResponseEntity<Resource> getPaperFeedbackLegalFact(String iun, String legalfactId) {
        final String attachmentId = legalfactId.replace("~","/");
        log.debug( "Retrieve attachment url from External Channel with attachmentId={}", attachmentId );
        String[] response = null; // this.externalChannelClient.getResponseAttachmentUrl( new String[] {attachmentId} );

        if ( response != null && response.length > 0 ) {
            try {
                final UrlResource urlResource = new UrlResource(URI.create(response[0]));
                return ResponseEntity.ok()
                        .headers( headers() )
                        .contentType(MediaType.APPLICATION_PDF)
                        .body( urlResource );

            } catch (MalformedURLException e) {
                log.error( "Unable to retrieve a valid attachment url for iun={} and attachmentId={}",
                        iun,
                        attachmentId );
                throw new PnInternalException( "Unable to retrieve resource " + response[0], e );
            }

        } else {
            final String message = String.format(MISSING_EXT_CHA_ATTACHMENT_MESSAGE, iun, legalfactId);
            log.error(message);
            throw new PnInternalException(message);
        }
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.add( "Cache-Control", "no-cache, no-store, must-revalidate" );
        headers.add( "Pragma", "no-cache" );
        headers.add( "Expires", "0" );
        return headers;
    }
}
