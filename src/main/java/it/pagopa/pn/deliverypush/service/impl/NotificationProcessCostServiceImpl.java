package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.cost.NotificationProcessCost;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.*;
import it.pagopa.pn.deliverypush.exceptions.PnNotificationNotAcceptedException;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationFeePolicy;
import it.pagopa.pn.deliverypush.service.NotificationProcessCostService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.utils.CostUtils;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Set;

@Service
@Slf4j
public class NotificationProcessCostServiceImpl implements NotificationProcessCostService {
    private final int sendFee;

    private final int defaultVat;

    private final int defaultFee;
    private final TimelineService timelineService;
    
    public NotificationProcessCostServiceImpl(TimelineService timelineService,
                                              PnDeliveryPushConfigs cfg
    ) {
        this.timelineService = timelineService;
        this.sendFee = cfg.getPagoPaNotificationBaseCost();
        this.defaultFee = cfg.getPagoPaNotificationFee();
        this.defaultVat = cfg.getPagoPaNotificationVat();
    }

    @Override
    public int getSendFee() {
        return sendFee;
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

        //Se la notificationFeePolicy è FLAT_RATE o flag applyCost è false, partialCost e totalCost sono sempre 0
        int notificationProcessPartialCost = 0;
        Integer notificationProcessTotalCost = 0;
        
        //Se la notificationFeePolicy è DELIVERY_MODE e il noticeCode/F24 per il quale si sta richiedendo il costo notificazione ha il flag applyCost a true ...
        if(NotificationFeePolicy.DELIVERY_MODE.equals(notificationFeePolicy) && Boolean.TRUE.equals(applyCost)) {
            //... viene valorizzato sempre il costo parziale di notificazione (senza iva e pafee) ...
            notificationProcessPartialCost = sendFee + analogCost;
            if (vat == null || paFee == null) {
                //... se iva e pafee NON sono valorizzati, ,vanno usati i valori di default.
                // si noti che per le notifiche create dopo lo sviluppo, questi sono comunque presenti nella notifica.
                vat = vat ==null? defaultVat:vat;
                paFee = paFee ==null? defaultFee:paFee;
            }
            int analogCostWithVatPlusFee = CostUtils.getCostWithVat(vat, analogCost) + paFee;
            notificationProcessTotalCost = sendFee + analogCostWithVatPlusFee;
        }
        
        log.info("End getNotificationProcessCost: notificationFeePolicy={} analogCost={} notificationBaseCost={} notificationProcessPartialCost={} notificationProcessTotalCost={} paFeeCost={} notificationViewDate={}, refinementDate={} - iun={} id={}",
                notificationFeePolicy, analogCost, sendFee, notificationProcessPartialCost, notificationProcessTotalCost, paFee, notificationViewDate, refinementDate, iun, recIndex);

        return NotificationProcessCost.builder()
                .partialCost(notificationProcessPartialCost)
                .totalCost(notificationProcessTotalCost)
                .analogCost(analogCost)
                .sendFee(sendFee)
                .vat(vat)
                .paFee(paFee)
                .notificationViewDate(notificationViewDate)
                .refinementDate(refinementDate)
                .build();
    }

    @NotNull
    private Result getAnalogCostNotificationViewDateRefinementDate(String iun, int recIndex) {
        Instant notificationViewDate = null;
        Instant refinementDate = null;
        Integer analogCost = 0;

        Set<TimelineElementInternal> timelineElements = timelineService.getTimeline(iun, false);
        log.debug("get timeline for notificationProcessCost completed - iun={} id={}", iun, recIndex);

        boolean isNotificationAccepted = false;
        for(TimelineElementInternal timelineElement : timelineElements) {

            if(timelineElement.getDetails() instanceof NotificationRequestAcceptedDetailsInt){
                isNotificationAccepted = true;
            }
            
            if( timelineElement.getDetails() instanceof RecipientRelatedTimelineElementDetails timelineElementRec 
                    && recIndex == timelineElementRec.getRecIndex()){
                
                if ( timelineElement.getDetails() instanceof NotificationViewedCreationRequestDetailsInt  notificationViewedCreationRequestDetailsInt){
                    notificationViewDate = notificationViewedCreationRequestDetailsInt.getEventTimestamp();
                } else {
                    refinementDate = getRefinementDate(recIndex, refinementDate, timelineElement);
                }
                
                analogCost = getAnalogCost(recIndex, analogCost, timelineElement);
            }
        }

        if(!isNotificationAccepted){
            log.warn("Notification with iun is not ACCEPTED - iun={}", iun);
            throw new PnNotificationNotAcceptedException();
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
        if(timelineElement.getDetails() instanceof RefinementDetailsInt refinementDetailsInt){
            refinementDate = refinementDetailsInt.getEventTimestamp();
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
