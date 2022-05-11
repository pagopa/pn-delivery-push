package it.pagopa.pn.deliverypush.abstractions.actionspool.impl;

import it.pagopa.pn.api.dto.events.StandardEventHeader;
import it.pagopa.pn.commons.abstractions.MomProducer;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionHandler;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.middleware.timelinedao.TimelineDao;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.temp.mom.consumer.AbstractEventHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class ActionsEventHandler extends AbstractEventHandler<ActionEvent> {

    private final NotificationService notificationService;
    private final TimelineDao timelineDao;
    private final Map<ActionType, ActionHandler> actionHandlers;
    private final MomProducer<ActionEvent> actionsDoneQueue;

    public ActionsEventHandler(NotificationService notificationService, TimelineDao timelineDao, List<ActionHandler> actionHandlers, @Qualifier("action-done") MomProducer<ActionEvent> actionsDoneQueue) {
        super( ActionEvent.class );
        this.notificationService = notificationService;
        this.timelineDao = timelineDao;
        this.actionHandlers = toMap( actionHandlers);
        this.actionsDoneQueue = actionsDoneQueue;
    }

    private Map<ActionType, ActionHandler> toMap(List<ActionHandler> actionHandlers) {
        Map<ActionType, ActionHandler> result = new EnumMap<>( ActionType.class );
        for( ActionHandler actionHandler: actionHandlers ) {
            result.put( actionHandler.getActionType(), actionHandler );
        }
        return  result;
    }

    @Override
    public void handleEvent(ActionEvent evt ) {
        StandardEventHeader header = evt.getHeader();
        Action action = evt.getPayload();

        log.info( "Received ACTION iun={} eventId={} actionType={}", header.getIun(), header.getEventId(), action.getType() );
        NotificationInt notification = notificationService.getNotificationByIun( header.getIun() );
        
        if( ! checkAlreadyDone( header )) {
            log.info("NOTIFICATION: {}", notification );
            doHandle( action, notification );
            notifyActionDone( evt );
        }
        else {
            log.warn("Duplicated action event: {}", evt );
        }
    }

    private void notifyActionDone(ActionEvent evt) {
        actionsDoneQueue.push( evt );
    }

    private void doHandle(Action action, NotificationInt notification) {

        ActionHandler actionHandler = actionHandlers.get(action.getType());
        if (actionHandler == null) {
            log.error("Action type not supported: " + action.getType());
            throw new PnInternalException("Action type not supported: " + action.getType());
        }

        actionHandler.handleAction(action, notification);
    }

    public boolean checkAlreadyDone( StandardEventHeader header ) {
        Optional<TimelineElementInternal> timeline = timelineDao.getTimelineElement( header.getIun(), header.getEventId() );
        return timeline.isPresent();
    }

}
