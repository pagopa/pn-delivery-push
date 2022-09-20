package it.pagopa.pn.deliverypush.service.utils;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.publicregistry.PublicRegistryResponse;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.dto.timeline.details.ContactPhaseInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.DeliveryModeInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.PublicRegistryCallDetailsInt;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_TIMELINE_ELEMENT_FAILED;

@Component
@Slf4j
public class PublicRegistryUtils {
    private final TimelineService timelineService;
    private final TimelineUtils timelineUtils;

    public PublicRegistryUtils(TimelineService timelineService,
                               TimelineUtils timelineUtils) {
        this.timelineService = timelineService;
        this.timelineUtils = timelineUtils;
    }

    public String generateCorrelationId(String iun, Integer recIndex, ContactPhaseInt contactPhase, int sentAttemptMade, DeliveryModeInt deliveryMode) {
        return TimelineEventId.PUBLIC_REGISTRY_CALL.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .deliveryMode(deliveryMode)
                        .contactPhase(contactPhase)
                        .sentAttemptMade(sentAttemptMade)
                        .build());
    }

    public void addPublicRegistryCallToTimeline(NotificationInt notification, Integer recIndex, ContactPhaseInt contactPhase, 
                                                int sentAttemptMade, String correlationId, DeliveryModeInt digital) {
        addTimelineElement( 
                timelineUtils.buildPublicRegistryCallTimelineElement(notification, recIndex, correlationId, digital, contactPhase, sentAttemptMade), 
                notification
        );
    }

    public PublicRegistryCallDetailsInt getPublicRegistryCallDetail(String iun, String correlationId) {
        //Viene ottenuto l'oggetto di timeline creato in fase d'invio notifica al public registry
        Optional<PublicRegistryCallDetailsInt> optTimeLinePublicRegistrySend = timelineService.getTimelineElementDetails(iun, correlationId, PublicRegistryCallDetailsInt.class);

        if (optTimeLinePublicRegistrySend.isPresent()) {
            return optTimeLinePublicRegistrySend.get();
        } else {
            log.error("There isn't timelineElement - iun {} correlationId {}", iun, correlationId);
            throw new PnInternalException("There isn't timelineElement - iun " + iun + " correlationId " + correlationId, ERROR_CODE_TIMELINE_ELEMENT_FAILED);
        }
    }

    public void addPublicRegistryResponseToTimeline(NotificationInt notification, Integer recIndex, PublicRegistryResponse response) {
        addTimelineElement( 
                timelineUtils.buildPublicRegistryResponseCallTimelineElement(notification, recIndex, response),
                notification
        );
    }

    private void addTimelineElement(TimelineElementInternal element, NotificationInt notification) {
        timelineService.addTimelineElement(element, notification);
    }

}
