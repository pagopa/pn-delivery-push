package it.pagopa.pn.deliverypush.service.utils;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status.NotificationStatusInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity.webhook.WebhookTimelineElementEntity;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.mapper.webhook.DtoToEntityWebhookTimelineMapper;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.mapper.webhook.EntityToDtoWebhookTimelineMapper;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.mapper.webhook.WebhookTimelineElementJsonConverter;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.entity.EventEntity;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.entity.StreamEntity;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.StatusService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.service.mapper.SmartMapper;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.Base64Utils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class WebhookUtils {
    private final DtoToEntityWebhookTimelineMapper mapperTimeline;
    private final EntityToDtoWebhookTimelineMapper entityToDtoTimelineMapper;
    private final WebhookTimelineElementJsonConverter timelineElementJsonConverter;
    private final TimelineService timelineService;
    private final StatusService statusService;
    private final NotificationService notificationService;
    private final Duration ttl;
    private final PnDeliveryPushConfigs pnDeliveryPushConfigs;

    public WebhookUtils(TimelineService timelineService, StatusService statusService, NotificationService notificationService,
                        PnDeliveryPushConfigs pnDeliveryPushConfigs, DtoToEntityWebhookTimelineMapper mapperTimeline, EntityToDtoWebhookTimelineMapper entityToDtoTimelineMapper,
                        WebhookTimelineElementJsonConverter timelineElementJsonConverter) {
        this.timelineService = timelineService;
        this.statusService = statusService;
        this.notificationService = notificationService;
        this.entityToDtoTimelineMapper = entityToDtoTimelineMapper;
        this.pnDeliveryPushConfigs = pnDeliveryPushConfigs;
        this.ttl = pnDeliveryPushConfigs.getWebhook().getTtl();
        this.mapperTimeline = mapperTimeline;
        this.timelineElementJsonConverter = timelineElementJsonConverter;
    }


    public RetrieveTimelineResult retrieveTimeline(String iun, String timelineId) {
        NotificationInt notificationInt = notificationService.getNotificationByIun(iun);
        // Non sono richieste le confidential infos. Nel caso in cui dovesse cambiare in futuro, rivedere il log dell'elemento timeline
        Set<TimelineElementInternal> timelineElementInternalSet = timelineService.getTimeline(iun, false);
        Optional<TimelineElementInternal> event = timelineElementInternalSet.stream().filter(x -> x.getElementId().equals(timelineId)).findFirst();

        if (event.isEmpty())
            throw new PnInternalException("Timeline event not found in timeline history", PnDeliveryPushExceptionCodes.ERROR_CODE_WEBHOOK_SAVEEVENT);

        // considero gli elementi di timeline più vecchi di quello passato
        Set<TimelineElementInternal> filteredPreviousTimelineElementInternalSet = timelineElementInternalSet.stream().filter(x -> x.getTimestamp().isBefore(event.get().getTimestamp())).collect(Collectors.toSet());
        // calcolo vecchio e nuovo stato in base allo storico "di quel momento"
        StatusService.NotificationStatusUpdate notificationStatusUpdate = statusService.computeStatusChange(event.get(), filteredPreviousTimelineElementInternalSet, notificationInt);
        return RetrieveTimelineResult.builder()
                .event(SmartMapper.mapTimelineInternal(event.get(), timelineElementInternalSet))  //bisogna cmq rimappare l'evento per sistemare le date
                .notificationStatusUpdate(notificationStatusUpdate)
                .notificationInt(notificationInt)
                .build();
    }


    public EventEntity buildEventEntity(Long atomicCounterUpdated, StreamEntity streamEntity,
                                        String newStatus, TimelineElementInternal timelineElementInternal) throws PnInternalException{

        Instant timestamp = timelineElementInternal.getTimestamp();

        // creo l'evento e lo salvo
        EventEntity eventEntity = new EventEntity(atomicCounterUpdated, streamEntity.getStreamId());

        if (!ttl.isZero())
            eventEntity.setTtl(LocalDateTime.now().plus(ttl).atZone(ZoneId.systemDefault()).toEpochSecond());
        eventEntity.setEventDescription(timestamp.toString() + "_" + timelineElementInternal.getElementId());

        // Lo iun ci va solo se è stata accettata, quindi escludo gli stati invalidation e refused
        if (StringUtils.hasText(newStatus)
                && NotificationStatusInt.valueOf(newStatus) != NotificationStatusInt.IN_VALIDATION
                && NotificationStatusInt.valueOf(newStatus) != NotificationStatusInt.REFUSED)
            eventEntity.setIun(timelineElementInternal.getIun());

        eventEntity.setNewStatus(newStatus);

        // il requestId ci va sempre, ed è il base64 dello iun
        eventEntity.setNotificationRequestId(Base64Utils.encodeToString(timelineElementInternal.getIun().getBytes(StandardCharsets.UTF_8)));

        WebhookTimelineElementEntity timelineElementEntity = mapperTimeline.dtoToEntity(timelineElementInternal);

        eventEntity.setElement(this.timelineElementJsonConverter.entityToJson(timelineElementEntity));

        return eventEntity;
    }

    public TimelineElementInternal getTimelineInternalFromEvent(EventEntity entity) throws PnInternalException{
        WebhookTimelineElementEntity timelineElementEntity = this.timelineElementJsonConverter.jsonToEntity(entity.getElement());
        return entityToDtoTimelineMapper.entityToDto(timelineElementEntity);
    }


    @Builder
    @Getter
    public static class RetrieveTimelineResult {
        private StatusService.NotificationStatusUpdate notificationStatusUpdate;
        private TimelineElementInternal event;
        private NotificationInt notificationInt;
    }

    public static boolean checkGroups(List<String> toCheckGroups, List<String> allowedGroups){
        List<String> safeToCheck = toCheckGroups != null ? toCheckGroups : Collections.emptyList();
        List<String> safeAllowedGroups = allowedGroups != null ? allowedGroups : Collections.emptyList();

        return safeAllowedGroups.isEmpty() || safeAllowedGroups.containsAll(safeToCheck) ;
    }

    public int getVersion (String version) {

        if (version != null && !version.isEmpty()){
            String versionNumberString = version.toLowerCase().replace("v", "");
            return Integer.parseInt(versionNumberString);
        }
        return Integer.parseInt(pnDeliveryPushConfigs.getWebhook().getCurrentVersion().replace("v", ""));

    }
}
