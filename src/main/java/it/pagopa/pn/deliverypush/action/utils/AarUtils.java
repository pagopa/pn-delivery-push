package it.pagopa.pn.deliverypush.action.utils;


import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.PdfInfo;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.dto.timeline.details.AarGenerationDetailsInt;
import it.pagopa.pn.deliverypush.service.SaveLegalFactsService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Optional;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_GENERATEPDFFAILED;

@Component
@Slf4j
public class AarUtils {
    private final SaveLegalFactsService saveLegalFactsService;
    private final TimelineUtils timelineUtils;
    private final TimelineService timelineService;
    private final NotificationUtils notificationUtils;

    public AarUtils(TimelineService timelineService, TimelineUtils timelineUtils, SaveLegalFactsService saveLegalFactsService, NotificationUtils notificationUtils) {
        this.saveLegalFactsService = saveLegalFactsService;
        this.timelineUtils = timelineUtils;
        this.timelineService = timelineService;
        this.notificationUtils = notificationUtils;
    }

    public void generateAARAndSaveInSafeStorageAndAddTimelineevent(NotificationInt notification, Integer recIndex) {
        try {
            // check se già esiste
            String elementId = TimelineEventId.AAR_GENERATION.buildEventId(
                    EventId.builder()
                            .iun(notification.getIun())
                            .recIndex(recIndex)
                            .build());

            Optional<TimelineElementInternal> timeline = timelineService.getTimelineElement(notification.getIun(), elementId);
            if (!timeline.isPresent()) {
                PdfInfo pdfInfo = saveLegalFactsService.saveAAR(notification, notificationUtils.getRecipientFromIndex(notification, recIndex));

                timelineService.addTimelineElement(
                        timelineUtils.buildAarGenerationTimelineElement(notification, recIndex, pdfInfo.getKey(), pdfInfo.getNumberOfPages()),
                        notification
                );
            } else
                log.debug("no need to recreate AAR iun={} timelineId={}", notification.getIun(), elementId);
        } catch (Exception e) {
            throw new PnInternalException("cannot generate AAR pdf", ERROR_CODE_DELIVERYPUSH_GENERATEPDFFAILED);
        }
    }


    public AarGenerationDetailsInt getAarGenerationDetails(NotificationInt notification, Integer recIndex) {
        // ricostruisco il timelineid della  genrazione dell'aar
        String aarGenerationEventId = TimelineEventId.AAR_GENERATION.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .build()
        );

        Optional<AarGenerationDetailsInt> detailOpt =
                timelineService.getTimelineElementDetails(notification.getIun(), aarGenerationEventId, AarGenerationDetailsInt.class);

        if (detailOpt.isEmpty() || !StringUtils.hasText(detailOpt.get().getGeneratedAarUrl()) || detailOpt.get().getNumberOfPages() == null)
            throw new PnInternalException("cannot retreieve AAR pdf safestoragekey", ERROR_CODE_DELIVERYPUSH_GENERATEPDFFAILED);

        return detailOpt.get();
    }
}
