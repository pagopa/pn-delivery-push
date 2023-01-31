package it.pagopa.pn.deliverypush.action.notificationview;

import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status.NotificationStatusInt;
import it.pagopa.pn.deliverypush.dto.mandate.DelegateInfoInt;
import it.pagopa.pn.deliverypush.dto.radd.RaddInfo;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.utils.StatusUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Slf4j
@Component
public class NotificationViewedRequestHandler {

    private final TimelineService timelineService;
    private final NotificationService notificationService;
    private final TimelineUtils timelineUtils;
    private final StatusUtils statusUtils;
    private final NotificationUtils notificationUtils;
    private final ViewNotification viewNotification;

    public NotificationViewedRequestHandler(TimelineService timelineService,
                                            NotificationService notificationService,
                                            TimelineUtils timelineUtils,
                                            StatusUtils statusUtils,
                                            NotificationUtils notificationUtils,
                                            ViewNotification viewNotification) {
        this.timelineService = timelineService;
        this.notificationService = notificationService;
        this.timelineUtils = timelineUtils;
        this.statusUtils = statusUtils;
        this.notificationUtils = notificationUtils;
        this.viewNotification = viewNotification;
    }
    
    //La richiesta proviene da delivery (La visualizzazione potrebbe essere da parte del delegato o da parte del destinatario)
    public void handleViewNotificationDelivery(String iun, Integer recIndex, DelegateInfoInt delegateInfo, Instant eventTimestamp) {
        handleViewNotification(iun, recIndex, null, delegateInfo, eventTimestamp).block();
    }

    //La richiesta proviene da RADD, visualizzazione da parte del destinatario 
    public Mono<Void> handleViewNotificationRadd(String iun, Integer recIndex, RaddInfo raddInfo, Instant eventTimestamp) {
        return handleViewNotification(iun, recIndex, raddInfo, null, eventTimestamp);
    }
    
    private Mono<Void> handleViewNotification(String iun, Integer recIndex, RaddInfo raddInfo, DelegateInfoInt delegateInfo, Instant eventTimestamp) {
        
        return Mono.fromCallable(() -> timelineUtils.checkNotificationIsAlreadyViewed(iun, recIndex))
            .flatMap( isNotificationAlreadyViewed -> {
                
                //I processi collegati alla visualizzazione di una notifica vengono effettuati solo la prima volta che la stessa viene visualizzata
                if(Boolean.FALSE.equals(isNotificationAlreadyViewed) ){

                    log.debug("Notification is not already viewed - iun={} id={}", iun, recIndex);

                    return Mono.fromCallable(() -> notificationService.getNotificationByIun(iun))
                            .flatMap( notification -> 
                                    Mono.fromCallable(() -> statusUtils.getCurrentStatusFromNotification(notification, timelineService))
                                    .flatMap( currentStatus -> {
                                        //Una notifica annullata non può essere perfezionata per visione
                                        if( !NotificationStatusInt.CANCELLED.equals(currentStatus) ){
                                            log.debug("Notification is not in state CANCELLED - iun={} id={}", iun, recIndex);
                                            
                                            NotificationRecipientInt recipient = notificationUtils.getRecipientFromIndex(notification, recIndex);
                                            return viewNotification.startVewNotificationProcess(notification, recipient, recIndex, raddInfo, delegateInfo, eventTimestamp);
                                        } else {
                                            log.debug("Notification is in status {}, can't start view Notification process - iun={} id={}", currentStatus, iun, recIndex);
                                            return Mono.empty();
                                        }
                                    })
                            );
                } else {
                    log.debug("Notification is already viewed - iun={} id={}", iun, recIndex);
                    return Mono.empty();
                }
            });
    }
}
