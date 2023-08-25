package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.generated.openapi.msclient.delivery.model.RequestUpdateStatusDto;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status.NotificationStatusHistoryElementInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status.NotificationStatusInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.delivery.PnDeliveryClient;
import it.pagopa.pn.deliverypush.service.StatusService;
import it.pagopa.pn.deliverypush.utils.StatusUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Set;


@Slf4j
@Service
public class StatusServiceImpl implements StatusService {
    private final PnDeliveryClient pnDeliveryClient;
    private final StatusUtils statusUtils;

    public StatusServiceImpl(PnDeliveryClient pnDeliveryClient, StatusUtils statusUtils) {
        this.pnDeliveryClient = pnDeliveryClient;
        this.statusUtils = statusUtils;
    }

    @Override
    public NotificationStatusUpdate computeStatusChange(TimelineElementInternal dto, Set<TimelineElementInternal> currentTimeline, NotificationInt notification) {
        log.debug("computeStatusChange Notification is present paProtocolNumber {} for iun {}", notification.getPaProtocolNumber(), dto.getIun());

        // - Calcolare lo stato corrente
        NotificationStatusInt currentState = computeLastStatusHistoryElement(notification, currentTimeline).getStatus();
        log.debug("computeStatusChange CurrentState is {} for iun {}", currentState, dto.getIun());

        currentTimeline.add(dto);

        // - Calcolare il nuovo stato
        NotificationStatusHistoryElementInt nextState = computeLastStatusHistoryElement(notification, currentTimeline);

        log.debug("computeStatusChange Next state is {} for iun {}", nextState.getStatus(), dto.getIun());

        return new NotificationStatusUpdate(currentState, nextState.getStatus());
    }


    @Override
    public NotificationStatusUpdate checkAndUpdateStatus(TimelineElementInternal dto, Set<TimelineElementInternal> currentTimeline, NotificationInt notification) {
        log.debug("checkAndUpdateStatus is present paProtocolNumber {} for iun {}", notification.getPaProtocolNumber(), dto.getIun());

        NotificationStatusUpdate notificationStatusUpdate = computeStatusChange(dto, currentTimeline, notification);
        NotificationStatusInt currentState = notificationStatusUpdate.getOldStatus();
        NotificationStatusInt nextState = notificationStatusUpdate.getNewStatus();

        log.debug("checkAndUpdateStatus Next state is {} for iun {}", nextState, dto.getIun());

        // - se i due stati differiscono
        if (!currentState.equals(nextState)) {
            updateStatus(dto.getIun(), nextState, dto.getTimestamp());
        }

        return new NotificationStatusUpdate(currentState, nextState);
    }

    @Override
    public NotificationStatusUpdate getStatus(TimelineElementInternal dto, Set<TimelineElementInternal> currentTimeline, NotificationInt notification) {
        log.debug("checkStatus is present paProtocolNumber {} for iun {}", notification.getPaProtocolNumber(), dto.getIun());

        NotificationStatusUpdate notificationStatusUpdate = computeStatusChange(dto, currentTimeline, notification);
        NotificationStatusInt currentState = notificationStatusUpdate.getOldStatus();
        NotificationStatusInt nextState = notificationStatusUpdate.getNewStatus();

        log.debug("checkStatus Next state is {} for iun {}", nextState, dto.getIun());

        return new NotificationStatusUpdate(currentState, nextState);
    }

    @Override
    public void updateStatus(String iun, NotificationStatusInt nextState, Instant timeStamp) {
        RequestUpdateStatusDto dto = getRequestUpdateStatusDto(iun, nextState, timeStamp);

        pnDeliveryClient.updateStatus(dto);
        log.info("Status changed to {} for iun {}", dto.getNextStatus(), dto.getIun());
    }

    private RequestUpdateStatusDto getRequestUpdateStatusDto(String iun, NotificationStatusInt nextState, Instant timeStamp) {
        return new RequestUpdateStatusDto()
                .nextStatus(it.pagopa.pn.deliverypush.generated.openapi.msclient.delivery.model.NotificationStatus.valueOf(nextState.name()))
                .iun(iun)
                .timestamp(timeStamp);
    }

    private NotificationStatusHistoryElementInt computeLastStatusHistoryElement(NotificationInt notification, Set<TimelineElementInternal> currentTimeline) {
        int numberOfRecipient = notification.getRecipients().size();
        Instant notificationCreatedAt = notification.getSentAt();
        List<NotificationStatusHistoryElementInt> historyElementList = statusUtils.getStatusHistory(
                currentTimeline,
                numberOfRecipient,
                notificationCreatedAt);

        return historyElementList.get(historyElementList.size() - 1);
    }

}
