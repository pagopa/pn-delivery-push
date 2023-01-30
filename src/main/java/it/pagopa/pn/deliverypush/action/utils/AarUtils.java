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
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Optional;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_GENERATEPDFFAILED;

@Component
@AllArgsConstructor
@Slf4j
public class AarUtils {
    private final SaveLegalFactsService saveLegalFactsService;
    private final TimelineUtils timelineUtils;
    private final TimelineService timelineService;
    private final NotificationUtils notificationUtils;

    public void generateAARAndSaveInSafeStorageAndAddTimelineevent(NotificationInt notification, Integer recIndex, String quickAccessToken) {
        try {
            // check se già esiste
            String elementId = TimelineEventId.AAR_GENERATION.buildEventId(
                    EventId.builder()
                            .iun(notification.getIun())
                            .recIndex(recIndex)
                            .build());

            Optional<TimelineElementInternal> timeline = timelineService.getTimelineElement(notification.getIun(), elementId);
            if (timeline.isEmpty()) {
                PdfInfo pdfInfo = saveLegalFactsService.sendCreationRequestForAAR(notification, notificationUtils.getRecipientFromIndex(notification, recIndex), quickAccessToken);

                timelineService.addTimelineElement(
                        timelineUtils.buildAarGenerationTimelineElement(notification, recIndex, pdfInfo.getKey(), pdfInfo.getNumberOfPages()),
                        notification
                );
            } else
                log.debug("no need to recreate AAR iun={} timelineId={}", notification.getIun(), elementId);
        } catch (Exception e) {
            log.error("cannot generate AAR pdf iun={} ex={}", notification.getIun(), e);
            throw new PnInternalException("cannot generate AAR pdf", ERROR_CODE_DELIVERYPUSH_GENERATEPDFFAILED, e);
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

        if (detailOpt.isEmpty() || !StringUtils.hasText(detailOpt.get().getGeneratedAarUrl()) || detailOpt.get().getNumberOfPages() == null) {
            log.error("Cannot retreieve AAR pdf safestoragekey iun={} detail={}", notification.getIun(), detailOpt);
            throw new PnInternalException("cannot retreieve AAR pdf safestoragekey", ERROR_CODE_DELIVERYPUSH_GENERATEPDFFAILED);
        }
        return detailOpt.get();
    }
}
