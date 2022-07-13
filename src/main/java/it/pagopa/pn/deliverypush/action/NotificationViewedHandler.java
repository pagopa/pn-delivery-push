package it.pagopa.pn.deliverypush.action;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.deliverypush.action.utils.InstantNowSupplier;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status.NotificationStatusInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.PaperNotificationFailedService;
import it.pagopa.pn.deliverypush.service.SaveLegalFactsService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.utils.StatusUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NotificationViewedHandler {

    private final SaveLegalFactsService legalFactStore;
    private final PaperNotificationFailedService paperNotificationFailedService;
    private final TimelineService timelineService;
    private final NotificationService notificationService;
    private final TimelineUtils timelineUtils;
    private final InstantNowSupplier instantNowSupplier;
    private final NotificationUtils notificationUtils;
    private final StatusUtils statusUtils;
    
    public NotificationViewedHandler(TimelineService timelineService,
                                     SaveLegalFactsService legalFactStore,
                                     PaperNotificationFailedService paperNotificationFailedService,
                                     NotificationService notificationService,
                                     TimelineUtils timelineUtils,
                                     InstantNowSupplier instantNowSupplier,
                                     NotificationUtils notificationUtils, 
                                     StatusUtils statusUtils
    ) {
        this.legalFactStore = legalFactStore;
        this.paperNotificationFailedService = paperNotificationFailedService;
        this.timelineService = timelineService;
        this.notificationService = notificationService;
        this.timelineUtils = timelineUtils;
        this.instantNowSupplier = instantNowSupplier;
        this.notificationUtils = notificationUtils;
        this.statusUtils = statusUtils;
    }
    
    public void handleViewNotification(String iun, Integer recIndex) {
        log.debug("Start HandleViewNotification - iun={}", iun);
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_NT_CHECK, "Start HandleViewNotification - iun={}", iun )
                .mdcEntry("iun", iun)
                .build();
        logEvent.log();
        
        boolean isNotificationAlreadyViewed = timelineUtils.checkNotificationIsAlreadyViewed(iun, recIndex);
        
        //I processi collegati alla visualizzazione di una notifica vengono effettuati solo la prima volta che la stessa viene visualizzata
        if( !isNotificationAlreadyViewed ){
            log.info("Start view notification process");
            
            NotificationInt notification = notificationService.getNotificationByIun(iun);
            NotificationStatusInt currentStatus = statusUtils.getCurrentStatusFromNotification(notification, timelineService);
            
            //Una notifica annullata non può essere perfezionata per visione
            if( !NotificationStatusInt.CANCELLED.equals(currentStatus) ){
                try {
                    handleViewNotification(iun, recIndex, notification);
                    logEvent.generateSuccess().log();
                } catch (Exception exc) {
                    logEvent.generateFailure(
                            "Notification is in status={}, can't start view Notification process - iun={} id={}", currentStatus, iun, recIndex
                    ).log();
                    throw exc;
                }
                
            }else {
                logEvent.generateFailure(
                        "Notification is in status={}, can't start view Notification process - iun={} id={}", currentStatus, iun, recIndex
                ).log();
                log.debug("Notification is in status {}, can't start view Notification process - iun={} id={}", currentStatus, iun, recIndex);
            }
        } else {
            logEvent.generateFailure("Notification is already viewed - iun={} id={}", iun, recIndex).log();
            log.debug("Notification is already viewed - iun={} id={}", iun, recIndex);
        }
        log.debug("End HandleViewNotification - iun={} id={}", iun, recIndex);
    }

    private void handleViewNotification(String iun, Integer recIndex, NotificationInt notification) {
        log.debug("handleViewNotification get recipient ok - iun={} id={}", iun, recIndex);

        NotificationRecipientInt recipient = notificationUtils.getRecipientFromIndex(notification, recIndex);
        String legalFactId = legalFactStore.saveNotificationViewedLegalFact(notification, recipient, instantNowSupplier.get());

        addTimelineElement(
                timelineUtils.buildNotificationViewedTimelineElement(notification, recIndex, legalFactId),
                notification
        ) ;

        paperNotificationFailedService.deleteNotificationFailed(recipient.getTaxId(), iun); //Viene eliminata l'eventuale istanza di notifica fallita dal momento che la stessa è stata letta
    }

    private void addTimelineElement(TimelineElementInternal element, NotificationInt notification) {
        timelineService.addTimelineElement(element, notification);
    }
}
