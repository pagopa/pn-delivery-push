package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.api.dto.legalfacts.LegalFactType;
import it.pagopa.pn.api.dto.legalfacts.LegalFactsListEntry;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationAttachment;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.timeline.RecipientRelatedTimelineElementDetails;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElementDetails;
import it.pagopa.pn.commons.abstractions.FileStorage;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.action2.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.legalfacts.LegalfactsMetadataUtils;
import it.pagopa.pn.deliverypush.middleware.timelinedao.TimelineDao;
import it.pagopa.pn.deliverypush.pnclient.externalchannel.ExternalChannelClient;
import it.pagopa.pn.deliverypush.service.LegalFactService;
import it.pagopa.pn.deliverypush.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class LegalFactServiceImpl implements LegalFactService {

    public static final String MISSING_EXT_CHA_LEGAL_FACT_MESSAGE = "Unable to retrieve paper feedback for iun=%s with id=%s from external channel API";

    private final TimelineDao timelineDao;
    private final FileStorage fileStorage;
    private final LegalfactsMetadataUtils legalfactsUtils;
    private final ExternalChannelClient externalChannelClient;
    private final NotificationService notificationService;
    private final NotificationUtils notificationUtils;

    public LegalFactServiceImpl(TimelineDao timelineDao,
                                FileStorage fileStorage,
                                LegalfactsMetadataUtils legalFactsUtils,
                                ExternalChannelClient externalChannelClient,
                                NotificationService notificationService, 
                                NotificationUtils notificationUtils) {
        this.timelineDao = timelineDao;
        this.fileStorage = fileStorage;
        this.legalfactsUtils = legalFactsUtils;
        this.externalChannelClient = externalChannelClient;
        this.notificationService = notificationService;
        this.notificationUtils = notificationUtils;
    }

    @Override
    @NotNull
    public List<LegalFactsListEntry> getLegalFacts(String iun) {
        log.debug( "Retrieve timeline elements for iun={}", iun );
        Set<TimelineElement> timelineElements = timelineDao.getTimeline(iun);
        Notification notification = notificationService.getNotificationByIun(iun);
        List<LegalFactsListEntry> legalFacts = timelineElements
                .stream()
                .filter( timeEl -> timeEl.getLegalFactsIds() != null )
                .sorted( Comparator.comparing( TimelineElement::getTimestamp ))
                .flatMap( timeEl -> timeEl.getLegalFactsIds().stream().map(
                        lfId -> LegalFactsListEntry.builder()
                                .taxId( readRecipientId( timeEl, notification ) )
                                .iun( iun )
                                .legalFactsId( lfId )
                                .build()
                        ))
                .collect(Collectors.toList());
        log.debug( "legalFacts List={}" ,legalFacts );
        return legalFacts;
    }

    private String readRecipientId( TimelineElement  timelineElement, Notification notification ) {
        String recipientId = null;
        //TODO Verificare se è necessario restituire il taxId o se può bastare il recIndex
        
        if (timelineElement != null) {
            TimelineElementDetails details = timelineElement.getDetails();
            if ( details instanceof RecipientRelatedTimelineElementDetails) {
                
                int recIndex = ((RecipientRelatedTimelineElementDetails) details).getRecIndex();
                NotificationRecipient recipient = notificationUtils.getRecipientFromIndex(notification, recIndex);
                recipientId = recipient.getTaxId();
            }
        }
        return recipientId;
    }

    public ResponseEntity<Resource> getLegalfact(String iun, LegalFactType type, String legalfactId ) {
        if ( LegalFactType.ANALOG_DELIVERY.equals( type ) ) {
            return getPaperFeedbackLegalFact(iun, legalfactId);
        } else {
            log.debug( "Retrieve notification attachment Ref for iun={} and legalfactId={}", iun, legalfactId );
            NotificationAttachment.Ref ref = legalfactsUtils.fromIunAndLegalFactId( iun, legalfactId );
            return fileStorage.loadAttachment( ref );
        }
    }

    @NotNull
    private ResponseEntity<Resource> getPaperFeedbackLegalFact(String iun, String legalfactId) {
        final String attachmentId = legalfactId.replace("~","/");
        log.debug( "Retrieve attachment url from External Channel with attachmentId={}", attachmentId );
        String[] response = this.externalChannelClient.getResponseAttachmentUrl( new String[] {attachmentId} );

        if ( response != null && response.length > 0 ) {
            try {
                final UrlResource urlResource = new UrlResource(URI.create(response[0]));
                return ResponseEntity.ok()
                        .headers( fileStorage.headers() )
                        .contentType(MediaType.APPLICATION_PDF)
                        .body( urlResource );

            } catch (MalformedURLException e) {
                log.error( "Unable to retrieve a valid attachment url for iun={} and attachmentId={}",
                        iun,
                        attachmentId );
                throw new PnInternalException( "Unable to retrieve resource " + response[0], e );
            }

        } else {
            final String message = String.format(MISSING_EXT_CHA_LEGAL_FACT_MESSAGE, iun, legalfactId);
            log.error(message);
            throw new PnInternalException(message);
        }
    }

}
