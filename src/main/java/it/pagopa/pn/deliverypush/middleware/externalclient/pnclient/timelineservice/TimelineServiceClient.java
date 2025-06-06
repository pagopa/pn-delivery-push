package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.timelineservice;

import it.pagopa.pn.deliverypush.generated.openapi.msclient.timelineservice.model.*;

import java.time.Instant;
import java.util.List;

public interface TimelineServiceClient {
    String CLIENT_NAME = "pn-timeline-service"; //TODO pn-commons??
    String ADD_TIMELINE_ELEMENT = "ADD TIMELINE ELEMENT";
    String RETRIEVE_AND_INCREMENT_COUNTER_FOR_TIMELINE_EVENT = "RETRIEVE AND INCREMENT COUNTER FOR TIMELINE EVENT";
    String GET_TIMELINE_ELEMENT = "GET TIMELINE ELEMENT";
    String GET_TIMELINE_ELEMENT_DETAILS = "GET TIMELINE ELEMENT DETAILS";
    String GET_TIMELINE_ELEMENT_DETAIL_FOR_SPECIFIC_RECIPIENT = "GET TIMELINE ELEMENT DETAIL FOR SPECIFIC RECIPIENT";
    String GET_TIMELINE_ELEMENT_FOR_SPECIFIC_RECIPIENT = "GET TIMELINE ELEMENT FOR SPECIFIC RECIPIENT";
    String GET_TIMELINE = "GET TIMELINE";
    String GET_TIMELINE_AND_STATUS_HISTORY = "GET TIMELINE AND STATUS HISTORY";
    String GET_SCHEDULING_ANALOG_DATE = "GET SCHEDULING ANALOG DATE";

    Boolean addTimelineElement(InlineObject inlineObject);

    Long retrieveAndIncrementCounterForTimelineEvent(String timelineId);

    TimelineElement getTimelineElement(String iun, String timelineId, Boolean strongly);

    TimelineElementDetails getTimelineElementDetails(String iun, String timelineId);

    TimelineElementDetails getTimelineElementDetailForSpecificRecipient(String iun, Integer recIndex, Boolean confidentialInfoRequired, TimelineCategory category);

    TimelineElement getTimelineElementForSpecificRecipient(String iun, Integer recIndex, TimelineCategory category);

    List<TimelineElement> getTimeline(String iun, Boolean confidentialInfoRequired, Boolean strongly, String timelineId);

    NotificationHistoryResponse getTimelineAndStatusHistory(String iun, Integer numberOfRecipients, Instant createdAt);

    ProbableSchedulingAnalogDate getSchedulingAnalogDate(String iun, Integer recIndex);
}
