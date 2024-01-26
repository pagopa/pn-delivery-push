package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.cost.NotificationProcessCost;
import it.pagopa.pn.deliverypush.dto.cost.PaymentsInfoForRecipientInt;
import it.pagopa.pn.deliverypush.dto.cost.UpdateCostPhaseInt;
import it.pagopa.pn.deliverypush.dto.cost.UpdateNotificationCostResponseInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.*;
import it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalregistry_reactive.model.UpdateNotificationCostRequest;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationFeePolicy;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.externalregistry.PnExternalRegistriesClientReactive;
import it.pagopa.pn.deliverypush.service.NotificationProcessCostService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.service.mapper.NotificationCostResponseMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
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
    public Mono<Integer> notificationProcessCostF24(String iun, int recIndex, NotificationFeePolicy notificationFeePolicy, Boolean applyCost, Integer paFee, Integer vat) {
        return Mono.fromCallable(() -> getNotificationProcessCost(iun, recIndex, notificationFeePolicy, applyCost, paFee, vat))
                .map(notificationProcessCost -> {
                    if (notificationProcessCost.getTotalCost() != null){
                        return notificationProcessCost.getTotalCost();
                    } else {
                        //F24 fa parte delle v2.1 delle api, che ha default per i campi vat e paFee, il totalCost deve essere sempre Valorizzato!
                        //TODO Valutare se lanciare exception o settare partialCost
                        String msg = String.format("Notification process totalCost is not present, can't generate F24 - iun=%s id=%s", iun, recIndex);
                        log.error(msg);
                        throw new PnInternalException(msg, PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_TOTAL_COST_NOT_AVAILABLE);                    }
                });
    }
    
    @Override
    public Mono<NotificationProcessCost> notificationProcessCost(String iun, int recIndex, NotificationFeePolicy notificationFeePolicy, Boolean applyCost, Integer paFee, Integer vat) {
        return Mono.fromCallable(() -> getNotificationProcessCost(iun, recIndex, notificationFeePolicy, applyCost, paFee, vat));
    }

    private NotificationProcessCost getNotificationProcessCost(
            String iun,
            int recIndex, 
            NotificationFeePolicy notificationFeePolicy, 
            Boolean applyCost, 
            Integer paFee,
            Integer vat) {
        log.info("Start getNotificationProcessCost notificationFeePolicy={} - iun={} id={} applyCost={} paFee={}", notificationFeePolicy, iun, recIndex, applyCost, paFee);
        final Result result = getAnalogCostNotificationViewDateRefinementDate(iun, recIndex);
        Instant notificationViewDate = result.notificationViewDate();
        Instant refinementDate = result.refinementDate();
        Integer analogCost = result.analogCost();
        Integer analogCostWithVat = getAnalogCostWithVat(vat, analogCost);

        //Se la notificationFeePolicy è FLAT_RATE o flag applyCost è false, partialCost e totalCost sono sempre 0
        int notificationProcessPartialCost = 0;
        Integer notificationProcessTotalCost = 0;
        
        //Se la notificationFeePolicy è DELIVERY_MODE e il noticeCode per il quale si sta richiedendo il costo notificazione ha il flag applyCost a true ...
        if(NotificationFeePolicy.DELIVERY_MODE.equals(notificationFeePolicy) && Boolean.TRUE.equals(applyCost)) {
            //... viene valorizzato sempre il costo parziale di notificazione (senza iva e pafee) ...
            notificationProcessPartialCost = PAGOPA_NOTIFICATION_BASE_COST + analogCost;
            if (vat != null && paFee != null) {
                //... se inoltre, iva e pafee sono valorizzati, viene calcolato anche il costo totale di notificazione (con iva e pafee)
                notificationProcessTotalCost = PAGOPA_NOTIFICATION_BASE_COST + analogCostWithVat + paFee;
            } else {
                //... se invece iva e pafee non sono valorizzati viene ritornato null. Vale solo per le sole notifiche precedenti alla v2,1 in cui
                // non risultavano presenti tali campi, dalla v2,1 in poi i campi ci sono ed è previsto sempre un default
                notificationProcessTotalCost = null;
            }
        }
        
        log.info("End getNotificationProcessCost: notificationFeePolicy={} analogCost={} notificationBaseCost={} notificationProcessPartialCost={} notificationProcessTotalCost={} paFeeCost={} notificationViewDate={}, refinementDate={} - iun={} id={}",
                notificationFeePolicy, analogCost, PAGOPA_NOTIFICATION_BASE_COST, notificationProcessPartialCost, notificationProcessTotalCost, paFee, notificationViewDate, refinementDate, iun, recIndex);

        return NotificationProcessCost.builder()
                .partialCost(notificationProcessPartialCost)
                .totalCost(notificationProcessTotalCost)
                .analogCost(analogCost)
                .pagoPaBaseCost(PAGOPA_NOTIFICATION_BASE_COST)
                .vat(vat)
                .paFee(paFee)
                .notificationViewDate(notificationViewDate)
                .refinementDate(refinementDate)
                .build();
    }

    private static Integer getAnalogCostWithVat(Integer vat, Integer analogCost) {
        return vat != null ? analogCost + (analogCost * vat / 100) : null;
    }

    @NotNull
    private Result getAnalogCostNotificationViewDateRefinementDate(String iun, int recIndex) {
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
        return new Result(notificationViewDate, refinementDate, analogCost);
    }

    private record Result(Instant notificationViewDate, Instant refinementDate, Integer analogCost) {
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
