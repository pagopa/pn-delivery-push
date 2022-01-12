package it.pagopa.pn.deliverypush.action2.utils;

import it.pagopa.pn.api.dto.notification.timeline.ContactPhase;
import it.pagopa.pn.api.dto.notification.timeline.DeliveryMode;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.deliverypush.external.PublicRegistry;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PublicRegistryUtils {
    private final PublicRegistry publicRegistry;
    private final TimelineService timelineService;
    private final TimelineUtils timelineUtils;

    public PublicRegistryUtils(PublicRegistry publicRegistry, TimelineService timelineService,
                               TimelineUtils timelineUtils) {
        this.publicRegistry = publicRegistry;
        this.timelineService = timelineService;
        this.timelineUtils = timelineUtils;
    }

    /**
     * Send get request to public registry
     *
     * @param iun          Notification unique identifier
     * @param taxId        User identifier
     * @param contactPhase Process phase where the request is sent. CHOOSE_DELIVERY -> request sent during delivery selection,  SEND_ATTEMPT ->  request Sent in Digital or analogic workflow
     */
    public void sendRequestForGetDigitalAddress(String iun, String taxId, ContactPhase contactPhase, int sentAttemptMade) {

        String correlationId = String.format(
                "%s_%s_%s_%s_%d",
                iun,
                taxId,
                DeliveryMode.DIGITAL,
                contactPhase,
                sentAttemptMade
        );
        log.info("SendRequestForGetDigitalAddress correlationId {} - iun {} id {}", correlationId, iun, taxId);

        addTimelineElement(timelineUtils.buildPublicRegistryCallTimelineElement(iun, taxId, correlationId, DeliveryMode.DIGITAL, contactPhase, sentAttemptMade));
        publicRegistry.sendRequestForGetDigitalAddress(taxId, correlationId);

        log.debug("End sendRequestForGetAddress correlationId {} - iun {} id {}", correlationId, iun, taxId);
    }

    public void sendRequestForGetPhysicalAddress(String iun, String taxId, int sentAttemptMade) {

        String correlationId = String.format(
                "%s_%s_%s_%s_%d",
                iun,
                taxId,
                DeliveryMode.ANALOG,
                ContactPhase.SEND_ATTEMPT,
                sentAttemptMade
        );
        log.info("SendRequestForGetPhysicalAddress correlationId {} - iun {} id {}", correlationId, iun, taxId);

        addTimelineElement(timelineUtils.buildPublicRegistryCallTimelineElement(iun, taxId, correlationId, DeliveryMode.ANALOG, ContactPhase.SEND_ATTEMPT, sentAttemptMade));
        publicRegistry.sendRequestForGetPhysicalAddress(taxId, correlationId);

        log.debug("End sendRequestForGetPhysicalAddress correlationId {} - iun {} id {}", correlationId, iun, taxId);
    }

    private void addTimelineElement(TimelineElement element) {
        timelineService.addTimelineElement(element);
    }
}
