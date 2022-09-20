package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.generated.openapi.clients.delivery.model.RequestUpdateStatusDto;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status.NotificationStatusHistoryElementInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status.NotificationStatusInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.delivery.PnDeliveryClient;
import it.pagopa.pn.deliverypush.service.StatusService;
import it.pagopa.pn.deliverypush.utils.StatusUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_STATUS_UPDATE_FAILED;

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
    public NotificationStatusUpdate checkAndUpdateStatus(TimelineElementInternal dto, Set<TimelineElementInternal> currentTimeline, NotificationInt notification) {
        log.debug("Notification is present paProtocolNumber {} for iun {}", notification.getPaProtocolNumber(), dto.getIun());

        // - Calcolare lo stato corrente
        NotificationStatusInt currentState = computeLastStatusHistoryElement(notification, currentTimeline).getStatus();
        log.debug("CurrentState is {} for iun {}", currentState, dto.getIun());

        currentTimeline.add(dto);

        // - Calcolare il nuovo stato
        NotificationStatusHistoryElementInt nextState = computeLastStatusHistoryElement(notification, currentTimeline);

        log.debug("Next state is {} for iun {}", nextState.getStatus(), dto.getIun());

        // - se i due stati differiscono
        if (!currentState.equals(nextState.getStatus()) && !nextState.getStatus().equals(NotificationStatusInt.REFUSED)) {

            updateStatus(dto.getIun(), nextState.getStatus(), dto.getTimestamp());
        }

        return new NotificationStatusUpdate(currentState, nextState.getStatus());
    }

    private void updateStatus(String iun, NotificationStatusInt nextState, Instant timeStamp) {
        RequestUpdateStatusDto dto = getRequestUpdateStatusDto(iun, nextState, timeStamp);

        ResponseEntity<Void> resp = pnDeliveryClient.updateStatus(dto);

        if (resp.getStatusCode().is2xxSuccessful()) {
            log.info("Status changed to {} for iun {}", dto.getNextStatus(), dto.getIun());
        } else {
            log.error("Status not updated correctly - iun {}", dto.getIun());
            throw new PnInternalException("Status not updated correctly - iun " + dto.getIun(), ERROR_CODE_STATUS_UPDATE_FAILED);
        }
    }

    private RequestUpdateStatusDto getRequestUpdateStatusDto(String iun, NotificationStatusInt nextState, Instant timeStamp) {
        return new RequestUpdateStatusDto()
                .nextStatus(it.pagopa.pn.delivery.generated.openapi.clients.delivery.model.NotificationStatus.valueOf(nextState.name()))
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
