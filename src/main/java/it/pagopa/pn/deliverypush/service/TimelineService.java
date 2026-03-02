package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactsIdIntWithRecIndex;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationHistoryResponse;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface TimelineService {

    boolean addTimelineElement(TimelineElementInternal element, NotificationInt notification);

    Optional<TimelineElementInternal> getTimelineElement(String iun, String timelineId);

    <T> Optional<T> getTimelineElementDetails(String iun, String timelineId, Class<T> timelineDetailsClass);

    <T> Optional<T> getTimelineElementDetailForSpecificRecipient(String iun, int recIndex, boolean confidentialInfoRequired, TimelineElementCategoryInt category, Class<T> timelineDetailsClass);

    Set<TimelineElementInternal> getTimeline(String iun, boolean confidentialInfoRequired);

    Set<TimelineElementInternal> getTimelineByIunTimelineId(String iun, String timelineId, boolean confidentialInfoRequired);

    NotificationHistoryResponse getTimelineAndStatusHistory(String iun, int numberOfRecipients, Instant createdAt);

    List<LegalFactsIdIntWithRecIndex> getLegalFacts(String iun, Integer recIndex);
}
