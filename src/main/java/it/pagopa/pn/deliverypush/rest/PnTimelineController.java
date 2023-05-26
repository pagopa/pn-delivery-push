package it.pagopa.pn.deliverypush.rest;

import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.api.TimelineAndStatusApi;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationHistoryResponse;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.ProbableSchedulingAnalogDateResponse;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.utils.MdcKey;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Slf4j
@RestController
public class PnTimelineController implements TimelineAndStatusApi {

    private final TimelineService timelineService;

    public PnTimelineController(TimelineService timelineService) { this.timelineService = timelineService; }

    @Override
    public Mono<ResponseEntity<NotificationHistoryResponse>> getNotificationHistory(String iun, 
                                                                                    Integer numberOfRecipients, 
                                                                                    Instant createdAt,
                                                                                    ServerWebExchange exchange) {
        log.debug("Received request getTimelineAndStatusHistory - iun={} numberOfRecipients={} createdAt={}", 
                iun, numberOfRecipients, createdAt);
        MDC.put(MDCUtils.MDC_PN_CTX_TOPIC, MdcKey.TIMELINE_KEY);
        
        NotificationHistoryResponse notificationHistoryResponse = timelineService.getTimelineAndStatusHistory(
                iun, 
                numberOfRecipients,
                createdAt
        );

        return Mono.just(ResponseEntity.ok(notificationHistoryResponse));
    }

    @Override
    public Mono<ResponseEntity<ProbableSchedulingAnalogDateResponse>> getSchedulingAnalogDate(String iun,
                                                                                              String recipientId,
                                                                                              final ServerWebExchange exchange) {

        return timelineService.getSchedulingAnalogDate(iun, recipientId)
                .map(ResponseEntity::ok);

    }


}
