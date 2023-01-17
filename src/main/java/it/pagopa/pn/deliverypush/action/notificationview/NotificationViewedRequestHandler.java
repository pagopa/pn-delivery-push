package it.pagopa.pn.deliverypush.action.notificationview;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status.NotificationStatusInt;
import it.pagopa.pn.deliverypush.dto.mandate.DelegateInfoInt;
import it.pagopa.pn.deliverypush.dto.radd.RaddInfo;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.utils.StatusUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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
        handleViewNotification(iun, recIndex, null, delegateInfo, eventTimestamp);
    }

    //La richiesta proviene da RADD, visualizzazione da parte del destinatario 
    public void handleViewNotificationRadd(String iun, Integer recIndex, RaddInfo raddInfo, Instant eventTimestamp) {
        handleViewNotification(iun, recIndex, raddInfo, null, eventTimestamp);
    }
    
    private void handleViewNotification(String iun, Integer recIndex, RaddInfo raddInfo, DelegateInfoInt delegateInfo, Instant eventTimestamp) {
        PnAuditLogEvent logEvent = generateAuditLog(iun, recIndex, raddInfo, delegateInfo);
        logEvent.log();
        
        boolean isNotificationAlreadyViewed = timelineUtils.checkNotificationIsAlreadyViewed(iun, recIndex);
        
        //I processi collegati alla visualizzazione di una notifica vengono effettuati solo la prima volta che la stessa viene visualizzata
        if( !isNotificationAlreadyViewed ){
            log.debug("Notification is not already viewed - iun={} id={}", iun, recIndex);

            NotificationInt notification = notificationService.getNotificationByIun(iun);
            NotificationStatusInt currentStatus = statusUtils.getCurrentStatusFromNotification(notification, timelineService);
            
            //Una notifica annullata non può essere perfezionata per visione
            if( !NotificationStatusInt.CANCELLED.equals(currentStatus) ){
                log.debug("Notification is not in state CANCELLED - iun={} id={}", iun, recIndex);

                try {
                    NotificationRecipientInt recipient = notificationUtils.getRecipientFromIndex(notification, recIndex);
                    viewNotification.startVewNotificationProcess(notification, recipient, recIndex, raddInfo, delegateInfo, eventTimestamp);
                    
                    logEvent.generateSuccess().log();
                } catch (Exception exc) {
                    logEvent.generateFailure("Exception in View notification ex={}", exc).log();
                    throw exc;
                }
                
            }else {
                log.debug("Notification is in status {}, can't start view Notification process - iun={} id={}", currentStatus, iun, recIndex);
            }
        } else {
            log.debug("Notification is already viewed - iun={} id={}", iun, recIndex);
        }
    }

    private PnAuditLogEvent generateAuditLog(String iun, Integer recIndex, RaddInfo raddInfo, DelegateInfoInt delegateInfo ) {
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        boolean viewedFromDelegate = delegateInfo != null;
        
        PnAuditLogEventType type = viewedFromDelegate ? PnAuditLogEventType.AUD_NT_VIEW_DEL : PnAuditLogEventType.AUD_NT_VIEW_RCP;
        return auditLogBuilder
                .before(type, "Start HandleViewNotification - iun={} id={} " +
                        "raddType={} raddTransactionId={} internalDelegateId={} mandateId={}", 
                        iun, 
                        recIndex,
                        raddInfo != null ? raddInfo.getType() : null,
                        raddInfo != null ? raddInfo.getTransactionId() : null,
                        viewedFromDelegate ? delegateInfo.getInternalId() : null,
                        viewedFromDelegate ? delegateInfo.getMandateId() : null
                )
                .iun(iun)
                .build();
    }



}
