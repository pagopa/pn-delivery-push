package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.api.dto.extchannel.ExtChannelResponse;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressSource2;
import it.pagopa.pn.api.dto.notification.address.PhysicalAddress;
import it.pagopa.pn.api.dto.notification.timeline.ContactPhase;
import it.pagopa.pn.api.dto.notification.timeline.DeliveryMode;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.api.dto.publicregistry.PublicRegistryResponse;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

public interface TimelineService {
    void addTimelineElement(TimelineElement element);

    Optional<TimelineElement> getTimelineElement(String iun, String timelineId);

    <T> Optional<T> getTimelineElement(String iun, String timelineId, Class<T> timelineDetailsClass);

    Set<TimelineElement> getTimeline(String iun);

    void addAcceptedRequestToTimeline(Notification notification, String taxId);

    void addAvailabilitySourceToTimeline(String taxId, String iun, DigitalAddressSource2 source, boolean isAvailable, int sentAttemptMade);

    void addDigitalFailureAttemptToTimeline(ExtChannelResponse response);

    void addSendCourtesyMessageToTimeline(String taxId, String iun, DigitalAddress address, Instant sendDate, String eventId);

    void addSendDigitalNotificationToTimeline(DigitalAddress digitalAddress, NotificationRecipient recipient, Notification notification, int sentAttemptMade, String eventId);

    void addSendSimpleRegisteredLetterToTimeline(String taxId, String iun, PhysicalAddress address, String eventId);

    void addSendAnalogNotificationToTimeline(PhysicalAddress address, NotificationRecipient recipient, Notification notification, boolean investigation,
                                             int sentAttemptMade, String eventId);

    void addSuccessDigitalWorkflowToTimeline(String taxId, String iun, DigitalAddress address);

    void addFailureDigitalWorkflowToTimeline(String taxId, String iun);

    void addSuccessAnalogWorkflowToTimeline(String taxId, String iun, PhysicalAddress address);

    void addFailureAnalogWorkflowToTimeline(String taxId, String iun);

    void addPublicRegistryResponseCallToTimeline(String iun, String taxId, PublicRegistryResponse response);

    void addPublicRegistryCallToTimeline(String iun, String taxId, String eventId, DeliveryMode deliveryMode, ContactPhase contactPhase, int sentAttemptMade);

    void addAnalogFailureAttemptToTimeline(ExtChannelResponse response, int sentAttemptMade);

}
