package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactsIdIntWithRecIndex;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationHistoryResponse;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface TimelineService {

    boolean addTimelineElement(TimelineElementInternal element, NotificationInt notification);

    Optional<TimelineElementInternal> getTimelineElement(String iun, String timelineId);

    Set<TimelineElementInternal> getTimeline(String iun, boolean confidentialInfoRequired);

    NotificationHistoryResponse getTimelineAndStatusHistory(String iun, int numberOfRecipients, Instant createdAt);

    boolean isNotificationRefused(String iun);

    Optional<String> getRecipientAARUrl(String iun, int recIndex);
    List<LegalFactsIdIntWithRecIndex> getLegalFacts(String iun, Integer recIndex);
}
