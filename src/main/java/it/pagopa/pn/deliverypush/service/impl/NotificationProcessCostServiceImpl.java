package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.cost.NotificationProcessCost;
import it.pagopa.pn.deliverypush.dto.cost.PaymentsInfoForRecipientInt;
import it.pagopa.pn.deliverypush.dto.cost.UpdateCostPhaseInt;
import it.pagopa.pn.deliverypush.dto.cost.UpdateNotificationCostResponseInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.*;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalregistry_reactive.model.UpdateNotificationCostRequest;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationFeePolicy;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.externalregistry.PnExternalRegistriesClientReactive;
import it.pagopa.pn.deliverypush.service.NotificationProcessCostService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.service.mapper.NotificationCostResponseMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@Service
@AllArgsConstructor
@Slf4j
public class NotificationProcessCostServiceImpl implements NotificationProcessCostService {
    public static final int PAGOPA_NOTIFICATION_BASE_COST = 100;
    private final TimelineService timelineService;
    private final PnExternalRegistriesClientReactive pnExternalRegistriesClientReactive;
    private final PnDeliveryPushConfigs cfg;

    @Override
    public Mono<Integer> getPagoPaNotificationBaseCost() {
        return Mono.just(cfg.getPagoPaNotificationBaseCost());
    }

    @Override
    public int getNotificationBaseCost(int paFee) {
        return paFee + cfg.getPagoPaNotificationBaseCost();
    }
    
    public Mono<UpdateNotificationCostResponseInt> setNotificationStepCost(int notificationStepCost,
                                                                         String iun,
                                                                         List<PaymentsInfoForRecipientInt> paymentsInfoForRecipients,
                                                                         Instant eventTimestamp,
                                                                         Instant eventStorageTimestamp,
                                                                         UpdateCostPhaseInt updateCostPhase){
        log.debug("Start service setNotificationStepCost");

        UpdateNotificationCostRequest updateNotificationCostRequest = NotificationCostResponseMapper.internalToExternal(notificationStepCost, iun, paymentsInfoForRecipients, eventTimestamp, eventStorageTimestamp, updateCostPhase);
        return pnExternalRegistriesClientReactive.updateNotificationCost(updateNotificationCostRequest)
                .map(NotificationCostResponseMapper::externalToInternal)
                .doOnSuccess(res -> log.debug("setNotificationStepCost service completed"));
    }

    @Override
    public Mono<NotificationProcessCost> notificationProcessCost(String iun, int recIndex, NotificationFeePolicy notificationFeePolicy, Boolean applyCost, Integer paFee) {
        return Mono.fromCallable(() -> getNotificationProcessCost(iun, recIndex, notificationFeePolicy, applyCost, paFee));
    }

    private NotificationProcessCost getNotificationProcessCost(String iun, int recIndex, NotificationFeePolicy notificationFeePolicy, Boolean applyCost, Integer paFee) {
        log.info("Start getNotificationProcessCost notificationFeePolicy={} - iun={} id={} applyCost={} paFee={}", notificationFeePolicy, iun, recIndex, applyCost, paFee);
        
        Instant notificationViewDate = null;
        Instant refinementDate = null;
        Integer analogCost = 0;
        int paFeeCost = paFee != null ? paFee : 0;
        
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
        
        int notificationProcessCost = 0; //In caso di FLAT_RATE viene restituito sempre zero
        
        if(NotificationFeePolicy.DELIVERY_MODE.equals(notificationFeePolicy) && Boolean.TRUE.equals(applyCost)){
            notificationProcessCost = PAGOPA_NOTIFICATION_BASE_COST + analogCost + paFeeCost;
        }

        log.info("End getNotificationProcessCost: notificationFeePolicy={} analogCost={} notificationBaseCost={} notificationProcessCost={} paFeeCost={} notificationViewDate={}, refinementDate={} - iun={} id={}",
                notificationFeePolicy, analogCost, PAGOPA_NOTIFICATION_BASE_COST, notificationProcessCost, paFeeCost, notificationViewDate, refinementDate, iun, recIndex);

        return NotificationProcessCost.builder()
                .cost(notificationProcessCost)
                .notificationViewDate(notificationViewDate)
                .refinementDate(refinementDate)
                .build();
    }

    private Integer getAnalogCost(int recIndex, Integer analogCost, TimelineElementInternal timelineElement) {
        if(timelineElement.getDetails() instanceof AnalogSendTimelineElement analogSend){
            log.debug("Add analogCost={} from timelineCategory={} - iun={} id={}", analogSend.getAnalogCost(), timelineElement.getCategory(), timelineElement.getIun(), recIndex);
            analogCost += analogSend.getAnalogCost() != null ? analogSend.getAnalogCost() : 0;
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
}
