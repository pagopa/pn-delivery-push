package it.pagopa.pn.deliverypush.service.utils;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status.NotificationStatusInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactsIdInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.*;
import it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.RefusedReason;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.entity.EventEntity;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.entity.StreamEntity;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.mapper.DtoToEntityRefusedReasonMapper;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.safestorage.PnSafeStorageClient;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.StatusService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.service.mapper.NotificationRefusedMapper;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.Base64Utils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class WebhookUtils {


    private final TimelineService timelineService;
    private final StatusService statusService;
    private final NotificationService notificationService;
    private final Duration ttl;

    public WebhookUtils(TimelineService timelineService, StatusService statusService, NotificationService notificationService, PnDeliveryPushConfigs pnDeliveryPushConfigs) {
        this.timelineService = timelineService;
        this.statusService = statusService;
        this.notificationService = notificationService;
        PnDeliveryPushConfigs.Webhook webhookConf = pnDeliveryPushConfigs.getWebhook();
        this.ttl = webhookConf.getTtl();
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
                .event(event.get())
                .notificationStatusUpdate(notificationStatusUpdate)
                .notificationInt(notificationInt)
                .build();
    }


    public EventEntity buildEventEntity(Long atomicCounterUpdated, StreamEntity streamEntity,
                                        String newStatus, TimelineElementInternal timelineElementInternal, NotificationInt notificationInt) {
        // creo l'evento e lo salvo
        EventEntity eventEntity = new EventEntity(atomicCounterUpdated, streamEntity.getStreamId());
        if (!ttl.isZero())
            eventEntity.setTtl(LocalDateTime.now().plus(ttl).atZone(ZoneId.systemDefault()).toEpochSecond());

        Instant timestamp = timelineElementInternal.getTimestamp();

        eventEntity.setEventDescription(timestamp.toString() + "_" + timelineElementInternal.getElementId());
        eventEntity.setTimestamp(timestamp);
        // Lo iun ci va solo se è stata accettata, quindi escludo gli stati invalidation e refused
        if (StringUtils.hasText(newStatus)
                && NotificationStatusInt.valueOf(newStatus) != NotificationStatusInt.IN_VALIDATION
                && NotificationStatusInt.valueOf(newStatus) != NotificationStatusInt.REFUSED)
            eventEntity.setIun(timelineElementInternal.getIun());
        eventEntity.setNewStatus(newStatus);
        eventEntity.setTimelineEventCategory(timelineElementInternal.getCategory().getValue());
        // il requestId ci va sempre, ed è il base64 dello iun
        eventEntity.setNotificationRequestId(Base64Utils.encodeToString(timelineElementInternal.getIun().getBytes(StandardCharsets.UTF_8)));
        TimelineElementDetailsInt details = timelineElementInternal.getDetails();
        return enrichWithAdditionalData(eventEntity, timelineElementInternal, details, notificationInt);
    }

    private EventEntity enrichWithAdditionalData(EventEntity eventEntity, TimelineElementInternal timelineElementInternal,  TimelineElementDetailsInt details, NotificationInt notificationInt) {

        enrichWithRecipientIndex(eventEntity, details);

        enrichWithAnalogCost(eventEntity, details);

        enrichWithChannel(eventEntity, notificationInt, details);

        enrichWithLegalFacts(eventEntity, timelineElementInternal);

        enrichWithValidationErrors(eventEntity, details);

        return eventEntity;
    }

    private void enrichWithChannel(EventEntity eventEntity, NotificationInt notificationInt, TimelineElementDetailsInt details) {
        // aggiungo il canale: per gli eventi analogici, è ricavato dalla notifica
        // per gli eventi digitali: è sempre PEC
        // per gli eventi di raccomandata semplice: è sempre simple registered letter
        // per gli eventi di courtesy: dipende dal tipo di messaggio di cortesia
        if (details instanceof SendAnalogFeedbackDetailsInt
                || details instanceof BaseAnalogDetailsInt
                || details instanceof SendAnalogProgressDetailsInt) {
            eventEntity.setChannel(notificationInt.getPhysicalCommunicationType().name());
        }
        else if (details instanceof BaseRegisteredLetterDetailsInt) {
            eventEntity.setChannel(PhysicalAddressInt.ANALOG_TYPE.SIMPLE_REGISTERED_LETTER.name());
        }
        else if (details instanceof SendDigitalFeedbackDetailsInt
                || details instanceof SendDigitalDetailsInt
                || details instanceof SendDigitalProgressDetailsInt) {
            eventEntity.setChannel(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC.getValue());
        }
        else if (details instanceof CourtesyAddressRelatedTimelineElement courtesyAddressRelatedTimelineElement) {
            eventEntity.setChannel(courtesyAddressRelatedTimelineElement.getDigitalAddress().getType().getValue());
        }
    }

    private void enrichWithAnalogCost(EventEntity eventEntity, TimelineElementDetailsInt details) {
        // aggiungo il costo di spedizione della raccomandata
        if (details instanceof SendAnalogDetailsInt sendAnalogDetailsInt) {
            eventEntity.setAnalogCost(sendAnalogDetailsInt.getAnalogCost());
        }
        else if (details instanceof SimpleRegisteredLetterDetailsInt simpleRegisteredLetterDetailsInt) {
            eventEntity.setAnalogCost(simpleRegisteredLetterDetailsInt.getAnalogCost());
        }
    }

    private void enrichWithRecipientIndex(EventEntity eventEntity, TimelineElementDetailsInt details) {
        // aggiungo il rec index
        if (details instanceof RecipientRelatedTimelineElementDetails recipientRelatedTimelineElementDetails) {
            eventEntity.setRecipientIndex(recipientRelatedTimelineElementDetails.getRecIndex());
        }
    }

    private void enrichWithLegalFacts(EventEntity eventEntity, TimelineElementInternal timelineElementInternal) {
        // aggiungo gli eventuali legalfact
        if (!CollectionUtils.isEmpty(timelineElementInternal.getLegalFactsIds()))
        {
            eventEntity.setLegalfactIds(timelineElementInternal.getLegalFactsIds().stream().filter(Objects::nonNull).map(LegalFactsIdInt::getKey).map(legalFactKey -> legalFactKey.replace(PnSafeStorageClient.SAFE_STORAGE_URL_PREFIX,"")).toList());
        }
    }

    private void enrichWithValidationErrors(EventEntity eventEntity, TimelineElementDetailsInt details) {
        // aggiungo gli errori di validazione
        if (details instanceof RequestRefusedDetailsInt requestRefusedDetailsInt) {
            List<RefusedReason> refusedReasons = requestRefusedDetailsInt.getRefusalReasons().stream().map(NotificationRefusedMapper::internalToExternal).toList();
            eventEntity.setValidationErrors( refusedReasons.stream().map(DtoToEntityRefusedReasonMapper::dtoToEntity).toList() );
        }
    }


    @Builder
    @Getter
    public static class RetrieveTimelineResult {
        private StatusService.NotificationStatusUpdate notificationStatusUpdate;
        private TimelineElementInternal event;
        private NotificationInt notificationInt;
    }

}
