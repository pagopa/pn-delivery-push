package it.pagopa.pn.deliverypush.rest;

import it.pagopa.pn.api.dto.notification.timeline.NotificationHistoryResponse;
import it.pagopa.pn.api.rest.PnDeliveryPushRestApi_methodGetTimeline;
import it.pagopa.pn.api.rest.PnDeliveryPushRestConstants;
import it.pagopa.pn.deliverypush.dto.TimelineElementDto;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.api.TimelineAndStatusApi;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
public class PnTimelineController implements TimelineAndStatusApi {

    private final TimelineService timelineService;

    public PnTimelineController(TimelineService timelineService) { this.timelineService = timelineService; }

    @Override
    public Mono<ResponseEntity<NotificationHistoryResponse>> getNotificationHistory(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String iun, ServerWebExchange exchange) {
        AnalogFailureWorkflow analogFailureWorkflow  = new AnalogFailureWorkflow();
        analogFailureWorkflow.recIndex(0);
        AnalogSuccessWorkflow analogSuccessWorkflow = new AnalogSuccessWorkflow();
        PhysicalAddress physicalAddress = new PhysicalAddress();
        physicalAddress.setAddress("indirizzo");
        physicalAddress.setAddressDetails("prova");
        physicalAddress.setAt("Milano");
        analogSuccessWorkflow.setAddress(physicalAddress);
        analogSuccessWorkflow.setRecIndex(0);
        /*
        TimelineElementDetails timelineElementDetails1 = new TimelineElementDetails();
        timelineElementDetails1.setTaxId(analogFailureWorkflow.getTaxId());
        timelineElementDetails1.setKind(analogFailureWorkflow.getKind());
        
        TimelineElementDetails timelineElementDetails2 = new TimelineElementDetails();
        timelineElementDetails2.setTaxId(analogSuccessWorkflow.getTaxId());
        timelineElementDetails2.setAddress(analogSuccessWorkflow.getAddress());
        timelineElementDetails2.setKind(analogSuccessWorkflow.getKind());
        */
        TimelineElement element1 = new TimelineElement();
        TimelineElementDto.setSpecificDetails(element1,analogFailureWorkflow);

        TimelineElement element2 = new TimelineElement();
        TimelineElementDto.setSpecificDetails(element2,analogSuccessWorkflow);

        List<TimelineElement> listTimelineElement = new ArrayList<>();
        listTimelineElement.add(element1);
        listTimelineElement.add(element2);
        
        NotificationHistoryResponse notificationHistoryResponse = new NotificationHistoryResponse();
        notificationHistoryResponse.setTimeline(listTimelineElement);
    
        return Mono.just(ResponseEntity.ok(notificationHistoryResponse));
    
    }
    
    /*
    @Override
    @GetMapping(PnDeliveryPushRestConstants.TIMELINE_AND_STATUS_HISTORY_BY_IUN)
    public ResponseEntity<NotificationHistoryResponse> getTimelineAndStatusHistory(String iun, int numberOfRecipients, Instant createdAt) {
        log.debug("Received request getTimelineAndStatusHistory - iun {} numberOfRecipients {} createdAt {}", iun, numberOfRecipients, createdAt);
        return ResponseEntity.ok().body(timelineService.getTimelineAndStatusHistory( iun, numberOfRecipients, createdAt));
    }
    
}
