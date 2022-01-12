package it.pagopa.pn.deliverypush.abstractions.actionspool.impl;

import it.pagopa.pn.api.dto.events.StandardEventHeader;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.commons.abstractions.MomProducer;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons_delivery.middleware.NotificationDao;
import it.pagopa.pn.commons_delivery.middleware.TimelineDao;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionHandler;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
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

    private final NotificationDao notificationDao;
    private final TimelineDao timelineDao;
    private final Map<ActionType, ActionHandler> actionHandlers;
    private final MomProducer<ActionEvent> actionsDoneQueue;

    public ActionsEventHandler(NotificationDao notificationDao, TimelineDao timelineDao, List<ActionHandler> actionHandlers, @Qualifier("action-done") MomProducer<ActionEvent> actionsDoneQueue) {
        super(ActionEvent.class);
        this.notificationDao = notificationDao;
        this.timelineDao = timelineDao;
        this.actionHandlers = toMap(actionHandlers);
        this.actionsDoneQueue = actionsDoneQueue;
    }

    private Map<ActionType, ActionHandler> toMap(List<ActionHandler> actionHandlers) {
        Map<ActionType, ActionHandler> result = new EnumMap<>(ActionType.class);
        for (ActionHandler actionHandler : actionHandlers) {
            result.put(actionHandler.getActionType(), actionHandler);
        }
        return result;
    }

    @Override
    public void handleEvent(ActionEvent evt) {
        StandardEventHeader header = evt.getHeader();
        Action action = evt.getPayload();

        log.info("Received ACTION iun={} eventId={} actionType={}", header.getIun(), header.getEventId(), action.getType());
        Optional<Notification> notification = notificationDao.getNotificationByIun(header.getIun());
        if (notification.isPresent()) {
            if (!checkAlreadyDone(header)) {
                log.info("NOTIFICATION: {}", notification.get());
                doHandle(action, notification.get());
                notifyActionDone(evt);
            } else {
                log.warn("Duplicated action event: {}", evt);
            }
        } else {
            log.warn("Notification metadata not found  - iun {}", header.getIun());
        }
    }

    private void notifyActionDone(ActionEvent evt) {
        actionsDoneQueue.push(evt);
    }

    private void doHandle(Action action, Notification notification) {

        ActionHandler actionHandler = actionHandlers.get(action.getType());
        if (actionHandler == null) {
            log.error("Action type not supported: " + action.getType());
            throw new PnInternalException("Action type not supported: " + action.getType());
        }

        actionHandler.handleAction(action, notification);
    }

    public boolean checkAlreadyDone(StandardEventHeader header) {
        Optional<TimelineElement> timeline = timelineDao.getTimelineElement(header.getIun(), header.getEventId());
        return timeline.isPresent();
    }

}
