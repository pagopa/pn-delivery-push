package it.pagopa.pn.deliverypush.action2.utils;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.dto.ext.publicregistry.PublicRegistryResponse;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.ContactPhase;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DeliveryMode;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.PublicRegistryCallDetails;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

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

    public String generateCorrelationId(String iun, Integer recIndex, ContactPhase contactPhase, int sentAttemptMade, DeliveryMode deliveryMode) {
        return String.format(
                "%s_%d_%s_%s_%d",
                iun,
                recIndex,
                deliveryMode,
                contactPhase,
                sentAttemptMade
        );
    }

    public void addPublicRegistryCallToTimeline(String iun, Integer recIndex, ContactPhase contactPhase, int sentAttemptMade, String correlationId, DeliveryMode digital) {
        addTimelineElement(timelineUtils.buildPublicRegistryCallTimelineElement(iun, recIndex, correlationId, digital, contactPhase, sentAttemptMade));
    }

    public PublicRegistryCallDetails getPublicRegistryCallDetail(String iun, String correlationId) {
        //Viene ottenuto l'oggetto di timeline creato in fase d'invio notifica al public registry
        Optional<PublicRegistryCallDetails> optTimeLinePublicRegistrySend = timelineService.getTimelineElement(iun, correlationId, PublicRegistryCallDetails.class);

        if (optTimeLinePublicRegistrySend.isPresent()) {
            return optTimeLinePublicRegistrySend.get();
        } else {
            log.error("There isn't timelineElement - iun {} correlationId {}", iun, correlationId);
            throw new PnInternalException("There isn't timelineElement - iun " + iun + " correlationId " + correlationId);
        }
    }

    public void addPublicRegistryResponseToTimeline(String iun,Integer recIndex, PublicRegistryResponse response) {
        addTimelineElement(timelineUtils.buildPublicRegistryResponseCallTimelineElement(iun, recIndex, response));
    }

    private void addTimelineElement(TimelineElementInternal element) {
        timelineService.addTimelineElement(element);
    }

}
