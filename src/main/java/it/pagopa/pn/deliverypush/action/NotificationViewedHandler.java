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
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.service.*;
import it.pagopa.pn.deliverypush.utils.StatusUtils;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
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
    private final NotificationCostService notificationCostService;
    
    public NotificationViewedHandler(TimelineService timelineService,
                                     SaveLegalFactsService legalFactStore,
                                     PaperNotificationFailedService paperNotificationFailedService,
                                     NotificationService notificationService,
                                     TimelineUtils timelineUtils,
                                     InstantNowSupplier instantNowSupplier,
                                     NotificationUtils notificationUtils,
                                     StatusUtils statusUtils,
                                     NotificationCostService notificationCostService) {
        this.legalFactStore = legalFactStore;
        this.paperNotificationFailedService = paperNotificationFailedService;
        this.timelineService = timelineService;
        this.notificationService = notificationService;
        this.timelineUtils = timelineUtils;
        this.instantNowSupplier = instantNowSupplier;
        this.notificationUtils = notificationUtils;
        this.statusUtils = statusUtils;
        this.notificationCostService = notificationCostService;
    }
    
    public void handleViewNotification(String iun, Integer recIndex) {
        
        log.info("Start HandleViewNotification - iun={}", iun);
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_NT_VIEW_RPC, "Start HandleViewNotification - iun={} id={}", iun, recIndex )
                .iun(iun)
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

    private void handleViewNotification(String iun, Integer recIndex, NotificationInt notification) {
        log.debug("handleViewNotification get recipient ok - iun={} id={}", iun, recIndex);

        NotificationRecipientInt recipient = notificationUtils.getRecipientFromIndex(notification, recIndex);
        String legalFactId = legalFactStore.saveNotificationViewedLegalFact(notification, recipient, instantNowSupplier.get());

        Integer notificationCost = getNotificationCost(iun, recIndex, notification);
        log.debug("Notification cost is {} - iun {} id {}",notificationCost, iun, recIndex);

        addTimelineElement(
                timelineUtils.buildNotificationViewedTimelineElement(notification, recIndex, legalFactId, notificationCost),
                notification
        ) ;

        paperNotificationFailedService.deleteNotificationFailed(recipient.getInternalId(), iun); //Viene eliminata l'eventuale istanza di notifica fallita dal momento che la stessa è stata letta
    }

    @Nullable
    private Integer getNotificationCost(String iun, Integer recIndex, NotificationInt notification) {
        Integer notificationCost = null;

        String elementId = TimelineEventId.REFINEMENT.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .build()
        );
        
        /*
        * Il costo della notifica viene valorizzato in fase di visualizzazione solo se la notifica non è già perfezionata per decorrenza termini
        * in quel caso il costo della notifica sarà sull'elemento di timeline corrispondente
        */
        
        if( timelineService.getTimelineElement(iun, elementId).isEmpty() ){
            notificationCost = notificationCostService.getNotificationCost(notification, recIndex);
        }
        return notificationCost;
    }

    private void addTimelineElement(TimelineElementInternal element, NotificationInt notification) {
        timelineService.addTimelineElement(element, notification);
    }
}
