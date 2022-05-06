package it.pagopa.pn.deliverypush.middleware.timelinedao;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.status.NotificationStatus;
import it.pagopa.pn.api.dto.notification.status.NotificationStatusHistoryElement;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElement;
import it.pagopa.pn.api.dto.status.RequestUpdateStatusDto;
import it.pagopa.pn.commons.abstractions.impl.MiddlewareTypes;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.pnclient.delivery.PnDeliveryClient;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.util.StatusUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(name = TimelineDao.IMPLEMENTATION_TYPE_PROPERTY_NAME, havingValue = MiddlewareTypes.DYNAMO)
@Slf4j
public class TimelineDaoDynamo implements TimelineDao {
    private final TimelineEntityDao entityDao;
    private final DtoToEntityTimelineMapper dto2entity;
    private final EntityToDtoTimelineMapper entity2dto;
    private final PnDeliveryClient pnDeliveryClient;
    private final NotificationService notificationService;
    private final StatusUtils statusUtils;

    public TimelineDaoDynamo(TimelineEntityDao entityDao, DtoToEntityTimelineMapper dto2entity,
                             EntityToDtoTimelineMapper entity2dto, PnDeliveryClient client, 
                             NotificationService notificationService, StatusUtils statusUtils) {
        this.entityDao = entityDao;
        this.dto2entity = dto2entity;
        this.entity2dto = entity2dto;
        this.pnDeliveryClient = client;
        this.notificationService = notificationService;
        this.statusUtils = statusUtils;
    }

    @Override
    public void addTimelineElement(TimelineElement dto) {
        //TODO Quando si passa alla versione v2 ristrutturando utilizzando il service che richiama l'addTimelineElement e non il dao come ora.
        // Verificare inoltre se possibile ristrutturare il codice per ricevere la Notification in ingresso, invece di effettuare la chiamata a delivery
        
        Notification notification = notificationService.getNotificationByIun(dto.getIun());
        
        if (notification != null) {
            log.debug("Notification is present {} for iun {}", notification.getPaNotificationId(), dto.getIun());

            Set<TimelineElement> currentTimeline = this.getTimeline(dto.getIun());

            // - Calcolare lo stato corrente
            NotificationStatus currentState = computeLastStatusHistoryElement(notification, currentTimeline).getStatus();
            log.debug("CurrentState is {} for iun {}", currentState, dto.getIun());

            currentTimeline.add(dto);

            // - Calcolare il nuovo stato
            NotificationStatusHistoryElement nextState = computeLastStatusHistoryElement(notification, currentTimeline);

            log.debug("Next state is {} for iun {}", nextState.getStatus(), dto.getIun());

            // - se i due stati differiscono
            if (!currentState.equals(nextState.getStatus()) && !nextState.getStatus().equals(NotificationStatus.REFUSED)){
                updateStatus(dto, currentState, nextState);
            }
            
            TimelineElementEntity entity = dto2entity.dtoToEntity(dto);
            entityDao.put(entity);
        } else {
            throw new PnInternalException("Try to update Timeline and Status for non existing iun " + dto.getIun());
        }
    }

    private void updateStatus(TimelineElement dto, NotificationStatus currentState, NotificationStatusHistoryElement nextState) {
        RequestUpdateStatusDto requestDto = getRequestUpdateStatusDto(dto, nextState.getStatus());

        ResponseEntity<Void> resp = pnDeliveryClient.updateState(requestDto);

        if (resp.getStatusCode().is2xxSuccessful()) {
            log.info("Status changed From {} to {} for iun {}", currentState, nextState.getStatus(), dto.getIun());
        } else {
            log.error("Status not updated correctly - iun {} timelineId {}", dto.getIun(), dto.getElementId());
            throw new PnInternalException("Status not updated correctly - iun " + dto.getIun() + " timelineId " + dto.getElementId());
        }
    }

    private NotificationStatusHistoryElement computeLastStatusHistoryElement(Notification notification, Set<TimelineElement> currentTimeline) {
        int numberOfRecipient = notification.getRecipients().size();
        Instant notificationCreatedAt = notification.getSentAt();

        List<NotificationStatusHistoryElement> historyElementList = statusUtils.getStatusHistory(
                currentTimeline,
                numberOfRecipient,
                notificationCreatedAt);

        return historyElementList.get(historyElementList.size() - 1);
    }

    @Override
    public Optional<TimelineElement> getTimelineElement(String iun, String timelineId) {
        Key keyToSearch = Key.builder()
                .partitionValue(iun)
                .sortValue(timelineId)
                .build();
        return entityDao.get(keyToSearch)
                .map(entity2dto::entityToDto);
    }

    @Override
    public Set<TimelineElement> getTimeline(String iun) {
        return entityDao.findByIun(iun)
                .stream()
                .map(entity2dto::entityToDto)
                .collect(Collectors.toSet());
    }

    @Override
    public void deleteTimeline(String iun) {
        entityDao.deleteByIun(iun);
    }

    private RequestUpdateStatusDto getRequestUpdateStatusDto(TimelineElement dto, NotificationStatus nextState) {
        return RequestUpdateStatusDto.builder()
                .iun(dto.getIun())
                .nextState(nextState)
                .build();
    }

}
