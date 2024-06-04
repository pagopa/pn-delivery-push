package it.pagopa.pn.deliverypush.service.impl;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_TOTAL_COST_NOT_PRESENT;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.cost.NotificationProcessCost;
import it.pagopa.pn.deliverypush.dto.cost.PaymentsInfoForRecipientInt;
import it.pagopa.pn.deliverypush.dto.cost.UpdateCostPhaseInt;
import it.pagopa.pn.deliverypush.dto.cost.UpdateNotificationCostResponseInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.AnalogSendTimelineElement;
import it.pagopa.pn.deliverypush.dto.timeline.details.NotificationViewedCreationRequestDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.RecipientRelatedTimelineElementDetails;
import it.pagopa.pn.deliverypush.dto.timeline.details.RefinementDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.ScheduleRefinementDetailsInt;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalregistry_reactive.model.UpdateNotificationCostRequest;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationFeePolicy;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.externalregistry.PnExternalRegistriesClientReactive;
import it.pagopa.pn.deliverypush.service.NotificationProcessCostService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.service.mapper.NotificationCostResponseMapper;
import it.pagopa.pn.deliverypush.utils.CostUtils;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class NotificationProcessCostServiceImpl implements NotificationProcessCostService {
    public static final double MIN_VERSION_PAFEE_VAT_MANDATORY = 2.3;
    private final int sendFee;

    private final int defaultVat;

    private final int defaultFee;
    private final TimelineService timelineService;
    private final PnExternalRegistriesClientReactive pnExternalRegistriesClientReactive;
    
    public NotificationProcessCostServiceImpl(TimelineService timelineService,
                                              PnExternalRegistriesClientReactive pnExternalRegistriesClientReactive, 
                                              PnDeliveryPushConfigs cfg
    ) {
        this.timelineService = timelineService;
        this.pnExternalRegistriesClientReactive = pnExternalRegistriesClientReactive;
        this.sendFee = cfg.getPagoPaNotificationBaseCost();
        this.defaultFee = cfg.getPagoPaNotificationFee();
        this.defaultVat = cfg.getPagoPaNotificationVat();
    }

    @Override
    public Mono<Integer> getSendFeeAsync() {
        return Mono.just(sendFee);
    }

    @Override
    public int getSendFee() {
        return sendFee;
    }
    
    @Override
    public int getNotificationBaseCost(int paFee) {
        return paFee + sendFee;
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
    public Mono<Integer> notificationProcessCostF24(String iun,
                                                    int recIndex,
                                                    NotificationFeePolicy notificationFeePolicy, 
                                                    Integer paFee,
                                                    Integer vat,
                                                    String version
    ) {
        return Mono.fromCallable(() -> getNotificationProcessCost(iun, recIndex, notificationFeePolicy, true, paFee, vat))
                .map(notificationProcessCost -> {
                    log.debug("Get notificationProcessCost response={}", notificationProcessCost);
                    if (notificationProcessCost.getTotalCost() != null){
                        log.info("For F24 can set notification total cost={} - iun={} id={}",notificationProcessCost.getTotalCost(), iun, recIndex );
                        return notificationProcessCost.getTotalCost();
                    } else {
                        log.info("For F24 cannot set notification total cost- iun={} id={}", iun, recIndex);
                        checkIsPossibileCase(iun, recIndex, notificationFeePolicy, version);
                        log.info("For F24 can set notification partial cost={} - iun={} id={}", notificationProcessCost.getPartialCost(), iun, recIndex );
                        return notificationProcessCost.getPartialCost();
                    }
                });
    }

    private static void checkIsPossibileCase(String iun, int recIndex, NotificationFeePolicy notificationFeePolicy, String version) {
        //Dalla versione 2.3 il totalCost deve essere sempre Valorizzato, perchè c'è l'obbligatorietà ed eventualmente default
        Double numberVersion =  version != null ? Double.valueOf(version) : null;
        if(NotificationFeePolicy.DELIVERY_MODE.equals(notificationFeePolicy) && numberVersion != null && numberVersion >= MIN_VERSION_PAFEE_VAT_MANDATORY){
            String msg = String.format("Notification process totalCost is not present and notification version is=%s, can't generate F24 - iun=%s id=%s", version, iun, recIndex);
            log.error(msg);
            throw new PnInternalException(msg, ERROR_CODE_DELIVERYPUSH_TOTAL_COST_NOT_PRESENT);
        }
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

        for(TimelineElementInternal timelineElement : timelineElements){
            
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
