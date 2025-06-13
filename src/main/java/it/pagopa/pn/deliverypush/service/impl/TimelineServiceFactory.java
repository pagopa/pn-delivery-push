package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@RequiredArgsConstructor
@Service
@Slf4j
public class TimelineServiceFactory {
    private final TimelineServiceHttpImpl timelineServiceHttpImpl;
    private final TimeLineServiceImpl timelineServiceImpl;
    private final PnDeliveryPushConfigs pnDeliveryPushConfigs;

    public TimelineService createTimelineService() {
        return createTimelineService(Instant.now());
    }

    public TimelineService createTimelineService(Instant referenceInstant) {
        Instant timelineClientEnabled = pnDeliveryPushConfigs.getTimelineClientEnabledStartDate();

        if (timelineClientEnabled == null) {
            log.info("Timeline client feature flag is null, using default TimelineServiceImpl (DYNAMO Impl)");
            return timelineServiceImpl;
        }

        if(referenceInstant.isAfter(timelineClientEnabled) || referenceInstant.equals(timelineClientEnabled)) {
            log.debug("Timeline client is enabled, using TimelineServiceHttpImpl (HTTP Impl)");
            return timelineServiceHttpImpl;
        } else {
            log.debug("Timeline client is not enabled, using default TimelineServiceImpl (DYNAMO Impl)");
            return timelineServiceImpl;
        }
    }
}
