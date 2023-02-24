package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.generated.openapi.clients.delivery.model.NotificationCostResponse;
import it.pagopa.pn.deliverypush.dto.ext.delivery.NotificationCostResponseInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.AnalogSendTimelineElement;
import it.pagopa.pn.deliverypush.dto.timeline.details.NotificationViewedDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.RefinementDetailsInt;
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
    public Mono<Integer> getPagoPaNotificationBaseCost(NotificationInt notificationInt, int recIndex) {
        return Mono.just(PAGOPA_NOTIFICATION_BASE_COST);
    }

    @Override
    public Mono<Integer> getNotificationProcessCost(NotificationInt notification, int recIndex) {
        Set<TimelineElementInternal> timelineElements = timelineService.getTimeline(notification.getIun(), false);
        
        Instant refinementDate = null;
        Integer analogCost = 0;
        
        for(TimelineElementInternal timelineElement : timelineElements){
            refinementDate = checkRefinementOrViewDate(refinementDate, timelineElement);
            
            if(timelineElement.getDetails() instanceof AnalogSendTimelineElement analogSend){
                analogCost +=analogSend.getAnalogCost(); 
            }
        }

    }

    private Instant checkRefinementOrViewDate(Instant refinementDate, TimelineElementInternal timelineElement) {
        if ((timelineElement.getDetails() instanceof NotificationViewedDetailsInt || timelineElement.getDetails() instanceof RefinementDetailsInt) 
                && 
                (refinementDate == null || timelineElement.getTimestamp().isBefore(refinementDate))
            ){
            refinementDate = timelineElement.getTimestamp();
        }
        return refinementDate;
    }


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
