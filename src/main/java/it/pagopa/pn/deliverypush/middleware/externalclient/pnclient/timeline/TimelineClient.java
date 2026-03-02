package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.timeline;

import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementDetailsInt;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.timelineservice.model.LegalFactsResponse;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.timelineservice.model.NotificationHistoryResponse;

import java.time.Instant;
import java.util.List;

public interface TimelineClient {
    String CLIENT_NAME = PnLogger.EXTERNAL_SERVICES.PN_TIMELINE_SERVICE;
    String ADD_TIMELINE_ELEMENT = "ADD TIMELINE ELEMENT";
    String GET_TIMELINE_ELEMENT = "GET TIMELINE ELEMENT";
    String GET_TIMELINE_ELEMENT_DETAILS = "GET TIMELINE ELEMENT DETAILS";
    String GET_TIMELINE_ELEMENT_DETAIL_FOR_SPECIFIC_RECIPIENT = "GET TIMELINE ELEMENT DETAIL FOR SPECIFIC RECIPIENT";
    String GET_TIMELINE_ELEMENT_FOR_SPECIFIC_RECIPIENT = "GET TIMELINE ELEMENT FOR SPECIFIC RECIPIENT";
    String GET_TIMELINE = "GET TIMELINE";
    String GET_TIMELINE_AND_STATUS_HISTORY = "GET TIMELINE AND STATUS HISTORY";
    String GET_LEGAL_FACTS = "GET LEGAL FACTS";

    boolean addTimelineElement(TimelineElementInternal element, NotificationInt notification);

    TimelineElementInternal getTimelineElement(String iun, String timelineId, Boolean strongly);

    TimelineElementDetailsInt getTimelineElementDetails(String iun, String timelineId);

    TimelineElementDetailsInt getTimelineElementDetailForSpecificRecipient(String iun, Integer recIndex, Boolean confidentialInfoRequired, TimelineElementCategoryInt category);

    TimelineElementInternal getTimelineElementForSpecificRecipient(String iun, Integer recIndex, TimelineElementCategoryInt category);

    List<TimelineElementInternal> getTimeline(String iun, Boolean confidentialInfoRequired, Boolean strongly, String timelineId);

    NotificationHistoryResponse getTimelineAndStatusHistory(String iun, Integer numberOfRecipients, Instant createdAt);

    LegalFactsResponse getLegalFacts(String iun, Integer recIndex);
}
