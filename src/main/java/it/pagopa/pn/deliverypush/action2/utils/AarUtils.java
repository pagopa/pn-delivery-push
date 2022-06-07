package it.pagopa.pn.deliverypush.action2.utils;


import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.AarGenerationDetails;
import it.pagopa.pn.deliverypush.legalfacts.LegalFactDao;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Component
@Slf4j
public class AarUtils {
    private final LegalFactDao legalFactDao;
    private final TimelineUtils timelineUtils;
    private final TimelineService timelineService;
    private final NotificationUtils notificationUtils;

    public AarUtils(TimelineService timelineService, TimelineUtils timelineUtils, LegalFactDao legalFactDao, NotificationUtils notificationUtils) {
        this.legalFactDao = legalFactDao;
        this.timelineUtils = timelineUtils;
        this.timelineService = timelineService;
        this.notificationUtils = notificationUtils;
    }

    public void generateAARAndSaveInSafeStorageAndAddTimelineevent(NotificationInt notification, Integer recIndex)
    {
        try {
            // check se già esiste
            String elementId = TimelineEventId.AAR_GENERATION.buildEventId(
                    EventId.builder()
                            .iun(notification.getIun())
                            .recIndex(recIndex)
                            .build());

            Optional<TimelineElementInternal> timeline = timelineService.getTimelineElement(notification.getIun(), elementId);
            if (!timeline.isPresent())
            {
                String safestoragekey = legalFactDao.saveAAR(notification, notificationUtils.getRecipientFromIndex(notification,recIndex));

                timelineService.addTimelineElement(timelineUtils.buildAarGenerationTimelineElement(notification, recIndex, safestoragekey));
            }
            else
                log.debug("no need to recreate AAR iun={} timelineId={}", notification.getIun(), elementId);
        } catch (Exception e) {
            throw new PnInternalException("cannot generate AAR pdf", e);
        }
    }

    public String getAarPdfFromTimeline(NotificationInt notification, Integer recIndex) {
        // ricostruisco il timelineid della  genrazione dell'aar
        String aarGenerationEventId = TimelineEventId.AAR_GENERATION.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .build()
        );

        Optional<AarGenerationDetails> detail = timelineService
                .getTimelineElementDetails(notification.getIun(), aarGenerationEventId, AarGenerationDetails.class);

        if (detail.isEmpty() || !StringUtils.hasText(detail.get().getGeneratedAarUrl()))
            throw new PnInternalException("cannot retreieve AAR pdf safestoragekey");

        return detail.get().getGeneratedAarUrl();
    }
}
