package it.pagopa.pn.deliverypush.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationHistoryResponse;
import it.pagopa.pn.deliverypush.service.impl.TimeLineServiceImpl;
import it.pagopa.pn.deliverypush.service.impl.TimelineServiceHttpImpl;
import lombok.CustomLog;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@RestController
@CustomLog
public class PrivateTestController {
    private final TimelineServiceHttpImpl serviceHttp;
    private final TimeLineServiceImpl serviceDao;
    private final ObjectMapper objectMapper;

    public PrivateTestController(TimelineServiceHttpImpl serviceHttp, TimeLineServiceImpl serviceDao, ObjectMapper objectMapper) {
        this.serviceHttp = serviceHttp;
        this.serviceDao = serviceDao;
        this.objectMapper = objectMapper.copy();
        this.objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }

    @GetMapping(
            value = "/delivery-push-private/test/old-impl/timeline/{iun}",
            produces = { "application/json", "application/problem+json" }
    )
    public Mono<ResponseEntity<String>> getTimelineWithOldImpl (
            @PathVariable("iun") String iun,
            @RequestParam(name="confidentialInfoRequired", required = false, defaultValue = "false") boolean confidentialInfoRequired
    ) {
        log.info("getTimelineWithOldImpl called with iun: {} and confidentialInfoRequired: {}", iun, confidentialInfoRequired);
        Set<TimelineElementInternal> timeline = serviceDao.getTimeline(iun, confidentialInfoRequired);
        List<TimelineElementInternal> orderedTimeline = timeline.stream()
                .sorted(Comparator.comparing(TimelineElementInternal::getElementId))
                .toList();
        return Mono.just(ResponseEntity.ok().body(writeJson(orderedTimeline)));
    }

    @GetMapping(
            value = "/delivery-push-private/test/new-impl/timeline/{iun}",
            produces = { "application/json", "application/problem+json" }
    )
    public Mono<ResponseEntity<String>> getTimelineWithNewImpl (
            @PathVariable("iun") String iun,
            @RequestParam(name="confidentialInfoRequired", required = false, defaultValue = "false") boolean confidentialInfoRequired
    ) {
        log.info("getTimelineWithNewImpl called with iun: {} and confidentialInfoRequired: {}", iun, confidentialInfoRequired);
        Set<TimelineElementInternal> timeline = serviceHttp.getTimeline(iun, confidentialInfoRequired);
        List<TimelineElementInternal> orderedTimeline = timeline.stream()
                .sorted(Comparator.comparing(TimelineElementInternal::getElementId))
                .toList();
        return Mono.just(ResponseEntity.ok().body(writeJson(orderedTimeline)));
    }

    @GetMapping(
            value = "/delivery-push-private/test/old-impl/history/{iun}",
            produces = { "application/json", "application/problem+json" }
    )
    public Mono<ResponseEntity<String>> getHistoryWithOldImpl (
            @PathVariable("iun") String iun,
            @RequestParam(name="numberOfRecipients") int numberOfRecipients,
            @RequestParam(name="createdAt") Instant createdAt
    ) {
        log.info("getTimelineWithOldImpl called with iun: {}, numberOfRecipients: {}, createdAt: {}", iun, numberOfRecipients, createdAt);
        NotificationHistoryResponse history = serviceDao.getTimelineAndStatusHistory(iun, numberOfRecipients, createdAt);
        return Mono.just(ResponseEntity.ok().body(writeJson(history)));
    }

    @GetMapping(
            value = "/delivery-push-private/test/new-impl/history/{iun}",
            produces = { "application/json", "application/problem+json" }
    )
    public Mono<ResponseEntity<String>> getHistoryWithNewImpl (
            @PathVariable("iun") String iun,
            @RequestParam(name="numberOfRecipients") int numberOfRecipients,
            @RequestParam(name="createdAt") Instant createdAt
    ) {
        log.info("getHistoryWithNewImpl called with iun: {}, numberOfRecipients: {}, createdAt: {}", iun, numberOfRecipients, createdAt);
        NotificationHistoryResponse history = serviceHttp.getTimelineAndStatusHistory(iun, numberOfRecipients, createdAt);
        return Mono.just(ResponseEntity.ok().body(writeJson(history)));
    }

    private <T> String writeJson(T object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new PnInternalException("Error serializing object to JSON", "JSON_SERIALIZATION_ERROR");
        }
    }
}
