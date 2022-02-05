package it.pagopa.pn.deliverypush.action2.utils;

import it.pagopa.pn.api.dto.notification.timeline.ContactPhase;
import it.pagopa.pn.api.dto.notification.timeline.DeliveryMode;
import it.pagopa.pn.api.dto.notification.timeline.PublicRegistryCallDetails;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.api.dto.publicregistry.PublicRegistryResponse;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class PublicRegistryUtils {
    private final TimelineService timelineService;
    private final TimelineUtils timelineUtils;

    public PublicRegistryUtils(TimelineService timelineService,
                               TimelineUtils timelineUtils) {
        this.timelineService = timelineService;
        this.timelineUtils = timelineUtils;
    }

    public String generateCorrelationId(String iun, String taxId, ContactPhase contactPhase, int sentAttemptMade, DeliveryMode deliveryMode) {
        return String.format(
                "%s_%s_%s_%s_%d",
                iun,
                taxId,
                deliveryMode,
                contactPhase,
                sentAttemptMade
        );
    }

    public void addPublicRegistryCallToTimeline(String iun, String taxId, ContactPhase contactPhase, int sentAttemptMade, String correlationId, DeliveryMode digital) {
        addTimelineElement(timelineUtils.buildPublicRegistryCallTimelineElement(iun, taxId, correlationId, digital, contactPhase, sentAttemptMade));
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

    public void addPublicRegistryResponseToTimeline(String iun, String taxId, PublicRegistryResponse response) {
        addTimelineElement(timelineUtils.buildPublicRegistryResponseCallTimelineElement(iun, taxId, response));
    }

    private void addTimelineElement(TimelineElement element) {
        timelineService.addTimelineElement(element);
    }

}
