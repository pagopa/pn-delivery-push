package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.generated.openapi.clients.delivery.model.NotificationCostResponse;
import it.pagopa.pn.deliverypush.dto.cost.NotificationProcessCost;
import it.pagopa.pn.deliverypush.dto.ext.delivery.NotificationCostResponseInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.*;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.delivery.PnDeliveryClient;
import it.pagopa.pn.deliverypush.service.NotificationCostService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.service.mapper.NotificationCostResponseMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Set;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_TAXIDNOTICECODEFAILED;

@Service
@AllArgsConstructor
@Slf4j
public class NotificationCostServiceImpl implements NotificationCostService {
    public static final int PAGOPA_NOTIFICATION_BASE_COST = 100;
    private final PnDeliveryClient pnDeliveryClient;
    private final TimelineService timelineService;
    

    @Override
    public Mono<Integer> getPagoPaNotificationBaseCost() {
        return Mono.just(PAGOPA_NOTIFICATION_BASE_COST);
    }

    @Override
    public Mono<NotificationProcessCost> notificationProcessCost(String iun, int recIndex) {
        return Mono.fromCallable(() -> getNotificationProcessCost(iun, recIndex));
    }

    private NotificationProcessCost getNotificationProcessCost(String iun, int recIndex) {
        log.info("Start getNotificationProcessCost - iun={} id={}", iun, recIndex);
        
        Instant notificationViewDate = null;
        Instant refinementDate = null;
        Integer analogCost = 0;
        
        Set<TimelineElementInternal> timelineElements = timelineService.getTimeline(iun, false);
        log.debug("get timeline for notificationProcessCost completed - iun={} id={}", iun, recIndex);

        for(TimelineElementInternal timelineElement : timelineElements){
            
            if( timelineElement.getDetails() instanceof RecipientRelatedTimelineElementDetails timelineElementRec 
                    && recIndex == timelineElementRec.getRecIndex()){
                
                if ( timelineElement.getDetails() instanceof NotificationViewedDetailsInt ){
                    notificationViewDate = timelineElement.getTimestamp();
                } else {
                    refinementDate = getRefinementDate(recIndex, refinementDate, timelineElement);
                }

                analogCost = getAnalogCost(recIndex, analogCost, timelineElement);
            }
        }

        int notificationProcessCost = PAGOPA_NOTIFICATION_BASE_COST + analogCost;

        log.info("End getNotificationProcessCost: analogCost={} notificationBaseCost={} notificationProcessCost={} notificationViewDate={}, refinementDate={} - iun={} id={}", 
                analogCost, PAGOPA_NOTIFICATION_BASE_COST, notificationProcessCost, notificationViewDate, refinementDate, iun, recIndex);

        return NotificationProcessCost.builder()
                .cost(notificationProcessCost)
                .notificationViewDate(notificationViewDate)
                .refinementDate(refinementDate)
                .build();
    }

    private Integer getAnalogCost(int recIndex, Integer analogCost, TimelineElementInternal timelineElement) {
        if(timelineElement.getDetails() instanceof AnalogSendTimelineElement analogSend){
            log.debug("Add analogCost={} from timelineCategory={} - iun={} id={}", analogSend.getAnalogCost(), timelineElement.getCategory(), timelineElement.getIun(), recIndex);
            analogCost += analogSend.getAnalogCost();
        }
        return analogCost;
    }

    private Instant getRefinementDate(int recIndex, Instant refinementDate, TimelineElementInternal timelineElement) {
        if(timelineElement.getDetails() instanceof RefinementDetailsInt){
            refinementDate = timelineElement.getTimestamp();
            log.debug("Set refinementDate={} from timelineCategory={} - iun={} id={}", refinementDate, timelineElement.getCategory(), timelineElement.getIun(), recIndex);
        } else {
            if(timelineElement.getDetails() instanceof ScheduleRefinementDetailsInt scheduleRefinementDetails
                && refinementDate == null){
                refinementDate = scheduleRefinementDetails.getSchedulingDate();
                log.debug("Set refinementDate={} from timelineCategory={} - iun={} id={}", refinementDate, timelineElement.getCategory(), timelineElement.getIun(), recIndex);
            }
        }
        return refinementDate;
    }
    
    //TODO Cancellare a valle del merge
    @Override
    public NotificationCostResponseInt getIunFromPaTaxIdAndNoticeCode(String paTaxId, String noticeCode) {
        NotificationCostResponse notificationCostResponse = pnDeliveryClient.getNotificationCostPrivate(paTaxId, noticeCode);

        log.debug("Get getIunFromPaTaxIdAndNoticeCode OK - paTaxId={} noticeCode={}", paTaxId, noticeCode);

        if (notificationCostResponse != null) {
            return NotificationCostResponseMapper.externalToInternal(notificationCostResponse);
        } else {
            log.error("getIunFromPaTaxIdAndNoticeCode is not valid - paTaxId={} noticeCode={}", paTaxId, noticeCode);
            throw new PnInternalException("getIunFromPaTaxIdAndNoticeCode - paTaxId= " + paTaxId + " noticeCode=" + noticeCode, ERROR_CODE_DELIVERYPUSH_TAXIDNOTICECODEFAILED);
        }
    }
}
