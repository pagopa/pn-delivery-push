package it.pagopa.pn.deliverypush.service.utils;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status.NotificationStatusInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity.TimelineElementEntity;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.mapper.DtoToEntityTimelineMapper;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.mapper.TimelineElementJsonConverter;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.entity.EventEntity;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.entity.StreamEntity;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.StatusService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.service.mapper.SmartMapper;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.Base64Utils;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class WebhookUtils {
    private final DtoToEntityTimelineMapper mapperTimeline;
    private final TimelineElementJsonConverter timelineElementJsonConverter;
    private final TimelineService timelineService;
    private final StatusService statusService;
    private final NotificationService notificationService;
    private final Duration ttl;

    public WebhookUtils(TimelineService timelineService, StatusService statusService, NotificationService notificationService,
                            PnDeliveryPushConfigs pnDeliveryPushConfigs, DtoToEntityTimelineMapper mapperTimeline, TimelineElementJsonConverter timelineElementJsonConverter) {
        this.timelineService = timelineService;
        this.statusService = statusService;
        this.notificationService = notificationService;
        PnDeliveryPushConfigs.Webhook webhookConf = pnDeliveryPushConfigs.getWebhook();
        this.ttl = webhookConf.getTtl();
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
                                        String newStatus, TimelineElementInternal timelineElementInternal, NotificationInt notificationInt) throws PnInternalException{

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

        TimelineElementEntity timelineElementEntity = mapperTimeline.dtoToEntity(timelineElementInternal);

        eventEntity.setElement(this.timelineElementJsonConverter.entityToJson(timelineElementEntity));

        return eventEntity;
    }


    @Builder
    @Getter
    public static class RetrieveTimelineResult {
        private StatusService.NotificationStatusUpdate notificationStatusUpdate;
        private TimelineElementInternal event;
        private NotificationInt notificationInt;
    }

    public static boolean checkGroups(List<String> toCheckGroups, List<String> allowedGroups){
        return
            (isEmpty(toCheckGroups) && isEmpty(allowedGroups)) ||
            allowedGroups.containsAll(toCheckGroups);
    }

    private static boolean isEmpty(List list){
        return list == null || list.isEmpty();
    }

    public int getVersion (String version) {

        String versionNumberString = version.toLowerCase().replace("v", "");
        return Integer.parseInt(versionNumberString);

    }
}
