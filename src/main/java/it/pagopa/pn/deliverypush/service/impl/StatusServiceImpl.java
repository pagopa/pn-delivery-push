package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.generated.openapi.clients.delivery.model.RequestUpdateStatusDto;
import it.pagopa.pn.deliverypush.dto.ext.delivery.RequestUpdateStatusDtoInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.externalclient.pnclient.delivery.PnDeliveryClient;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationStatus;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationStatusHistoryElement;
import it.pagopa.pn.deliverypush.service.StatusService;
import it.pagopa.pn.deliverypush.service.mapper.RequestUpdateStatusDtoMapper;
import it.pagopa.pn.deliverypush.utils.StatusUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
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
    public NotificationStatusUpdate checkAndUpdateStatus(TimelineElementInternal dto, Set<TimelineElementInternal> currentTimeline, NotificationInt notification) {
        log.debug("Notification is present PaNotificationId {} for iun {}", notification.getPaNotificationId(), dto.getIun());
        
        // - Calcolare lo stato corrente
        NotificationStatus currentState = computeLastStatusHistoryElement(notification, currentTimeline).getStatus();
        log.debug("CurrentState is {} for iun {}", currentState, dto.getIun());

        currentTimeline.add(dto);

        // - Calcolare il nuovo stato
        NotificationStatusHistoryElement nextState = computeLastStatusHistoryElement(notification, currentTimeline);

        log.debug("Next state is {} for iun {}", nextState.getStatus(), dto.getIun());

        // - se i due stati differiscono
        if (!currentState.equals(nextState.getStatus()) && !nextState.getStatus().equals(NotificationStatus.REFUSED)){
            
            RequestUpdateStatusDtoInt requestDto = getRequestUpdateStatusDto(dto.getIun(), nextState.getStatus());
            updateStatus(requestDto);
        }

        return new NotificationStatusUpdate(currentState, nextState.getStatus());
    }

    private void updateStatus(RequestUpdateStatusDtoInt dto) {
        RequestUpdateStatusDto updateStatusDto = RequestUpdateStatusDtoMapper.internalToExternal(dto);
        ResponseEntity<Void> resp = pnDeliveryClient.updateStatus(updateStatusDto);

        if (resp.getStatusCode().is2xxSuccessful()) {
            log.info("Status changed to {} for iun {}", dto.getNextState(), dto.getIun());
        } else {
            log.error("Status not updated correctly - iun {}", dto.getIun());
            throw new PnInternalException("Status not updated correctly - iun " + dto.getIun());
        }
    }
    
    private RequestUpdateStatusDtoInt getRequestUpdateStatusDto(String iun, NotificationStatus nextState) {
        return RequestUpdateStatusDtoInt.builder()
                .iun(iun)
                .nextState(nextState)
                .build();
    }

    private NotificationStatusHistoryElement computeLastStatusHistoryElement(NotificationInt notification, Set<TimelineElementInternal> currentTimeline) {
        int numberOfRecipient = notification.getRecipients().size();
        Instant notificationCreatedAt = notification.getSentAt();

        List<NotificationStatusHistoryElement> historyElementList = statusUtils.getStatusHistory(
                currentTimeline,
                numberOfRecipient,
                notificationCreatedAt);

        return historyElementList.get(historyElementList.size() - 1);
    }

    
}
