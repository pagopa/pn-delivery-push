package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.f24.PnF24Client;
import it.pagopa.pn.deliverypush.service.F24Service;
import it.pagopa.pn.deliverypush.service.NotificationProcessCostService;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@AllArgsConstructor
@Slf4j
@Service
public class F24ServiceImpl implements F24Service {

    private final PnF24Client pnF24Client;
    private final NotificationProcessCostService notificationProcessCostService;
    private final NotificationService notificationService;
    private final TimelineUtils timelineUtils;
    private final TimelineService timelineService;


    @Override
    public void preparePDF(String iun) {
        NotificationInt notification = notificationService.getNotificationByIun(iun);
        Integer cost = retrieveCost(notification,0);

        TimelineElementInternal generateF24RequestTimelineElement = timelineUtils.buildGenerateF24RequestTimelineElement(notification);

        log.debug("Invoke preparePdf elementId{}, iun: {}, cost {}",generateF24RequestTimelineElement.getElementId(),iun,cost);
        pnF24Client.preparePDF(generateF24RequestTimelineElement.getElementId(),iun, cost);

        boolean skipped = timelineService.addTimelineElement(generateF24RequestTimelineElement, notification);
        if(skipped){
            log.error("GenerateF24Request Timeline element not inserted! iun: {}, timelineElement: {}", iun, generateF24RequestTimelineElement);
        }else{
            log.debug("GenerateF24Request Timeline element inserted! iun: {}",iun);
        }

    }

    @Override
    public void handleF24PrepareResponse(String iun, Map<Integer, List<String>> generatedUrls) {
        NotificationInt notification = notificationService.getNotificationByIun(iun);

        generatedUrls.keySet().forEach(recIndex -> {
            List<String> f24Attachments = generatedUrls.get(recIndex);
            TimelineElementInternal generatedF24TimelineElem = timelineUtils.buildGeneratedF24TimelineElement(notification, recIndex, f24Attachments);
            boolean skipped = timelineService.addTimelineElement(generatedF24TimelineElem, notification);
            if(skipped){
                log.error("GeneratedF24 Timeline element not inserted! iun: {}, recIndex: {}, timelineElement: {}", iun, recIndex, generatedF24TimelineElem);
            }else{
                log.debug("GeneratedF24 Timeline element inserted! iun: {}, recIndex: {}", iun, recIndex);
            }
        });
    }


    private Integer retrieveCost(NotificationInt notificationInt, int recipientIdx) {
        return notificationProcessCostService.notificationProcessCostF24(
                        notificationInt.getIun(),
                        recipientIdx,
                        notificationInt.getNotificationFeePolicy(),
                        notificationInt.getPaFee(),
                        notificationInt.getVat(),
                        notificationInt.getVersion()
                )
                .block();
    }

}
