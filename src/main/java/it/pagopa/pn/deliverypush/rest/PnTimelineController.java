package it.pagopa.pn.deliverypush.rest;

import it.pagopa.pn.deliverypush.dto.TimelineElementDto;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.api.TimelineAndStatusApi;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@RestController
public class PnTimelineController implements TimelineAndStatusApi {

    private TimelineService timelineService;

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
    @GetMapping(PnDeliveryPushRestConstants.TIMELINE_BY_IUN)
    public ResponseEntity<Set<TimelineElement>> getTimelineElements(String iun) {
        return ResponseEntity.ok().body(timelineService.getTimeline( iun ));
    }*/
}
