package it.pagopa.pn.deliverypush.action2.utils;


import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.AarGenerationDetails;
import it.pagopa.pn.deliverypush.legalfacts.LegalFactDao;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Slf4j
public class AarUtils {
    private final LegalFactDao legalFactDao;
    private final TimelineUtils timelineUtils;
    private final TimelineService timelineService;

    public AarUtils(TimelineService timelineService, TimelineUtils timelineUtils, LegalFactDao legalFactDao) {
        this.legalFactDao = legalFactDao;
        this.timelineUtils = timelineUtils;
        this.timelineService = timelineService;
    }

    public void generateAARAndSaveInSafeStorageAndAddTimelineevent(NotificationInt notification, Integer recIndex)
    {
        try {
            String safestoragekey = legalFactDao.saveAAR(notification);

            timelineService.addTimelineElement(timelineUtils.buildAarGenerationTimelineElement(notification, recIndex, safestoragekey));

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

        if (detail.isEmpty())
            throw new PnInternalException("cannot retreieve AAR pdf safestoragekey");

        return detail.get().getSafestorageKey();
    }
}
