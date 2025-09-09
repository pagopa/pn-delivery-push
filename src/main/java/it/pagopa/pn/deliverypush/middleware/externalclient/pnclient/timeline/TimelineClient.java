package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.timeline;

import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.timelineservice.model.*;

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

    boolean addTimelineElement(NewTimelineElement newTimelineElement);

    TimelineElement getTimelineElement(String iun, String timelineId, Boolean strongly);

    TimelineElementDetails getTimelineElementDetails(String iun, String timelineId);

    TimelineElementDetails getTimelineElementDetailForSpecificRecipient(String iun, Integer recIndex, Boolean confidentialInfoRequired, TimelineCategory category);

    List<TimelineElement> getTimeline(String iun, Boolean confidentialInfoRequired, Boolean strongly, String timelineId);

    NotificationHistoryResponse getTimelineAndStatusHistory(String iun, Integer numberOfRecipients, Instant createdAt);
}
