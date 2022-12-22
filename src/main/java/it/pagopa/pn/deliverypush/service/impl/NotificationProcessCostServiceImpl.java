package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendAnalogDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.SimpleRegisteredLetterDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypush.service.NotificationProcessCostService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class NotificationProcessCostServiceImpl implements NotificationProcessCostService {
    private final PnDeliveryPushConfigs pnDeliveryPushConfigs;
    private final TimelineService timelineService;
    
    public NotificationProcessCostServiceImpl(PnDeliveryPushConfigs pnDeliveryPushConfigs,
                                              TimelineService timelineService) {
        this.pnDeliveryPushConfigs = pnDeliveryPushConfigs;
        this.timelineService = timelineService;
    }

    @Override
    public Mono<Integer> getNotificationProcessCost(String iun, int recIndex) {
        return getPaperNotificationSentCost(iun, recIndex).map(
                res -> res + pnDeliveryPushConfigs.getNotificationBaseCostForPn()
        );
    }

    private Mono<Integer> getPaperNotificationSentCost(String iun, int recIndex){
        return Mono.just(
                //Da rivedere il get timeline e renderlo mono
                timelineService.getTimeline(iun, false)
                        .stream()
                        .mapToInt( timelineElement -> getTimelineElementCost(timelineElement, recIndex))
                        .sum()
        );
    }

    private Integer getTimelineElementCost(TimelineElementInternal timelineElement, int recIndex) {
        
        if ( timelineElement.getCategory().equals(TimelineElementCategoryInt.SEND_ANALOG_DOMICILE) ){
            SendAnalogDetailsInt sendAnalogDetails = (SendAnalogDetailsInt) timelineElement.getDetails();
            if(recIndex == sendAnalogDetails.getRecIndex()){
                return sendAnalogDetails.getAnalogCost();
            }
        } else if ( timelineElement.getCategory().equals(TimelineElementCategoryInt.SEND_SIMPLE_REGISTERED_LETTER) ){
            SimpleRegisteredLetterDetailsInt simpleRegisteredLetterDetails = (SimpleRegisteredLetterDetailsInt) timelineElement.getDetails();
            if(recIndex == simpleRegisteredLetterDetails.getRecIndex()){
                return simpleRegisteredLetterDetails.getAnalogCost();
            }
        }
        
        return 0;
    }
}
