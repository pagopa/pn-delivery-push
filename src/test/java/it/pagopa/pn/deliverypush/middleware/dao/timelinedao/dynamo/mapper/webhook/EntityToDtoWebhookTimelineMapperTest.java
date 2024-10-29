package it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.mapper.webhook;

import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendAnalogDetailsInt;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity.*;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity.webhook.WebhookTimelineElementEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;

class EntityToDtoWebhookTimelineMapperTest {
    private final EntityToDtoWebhookTimelineMapper mapper = new EntityToDtoWebhookTimelineMapper();

    @Test
    void entityToDtoSendAnalogDomicile() {

        WebhookTimelineElementEntity entity = WebhookTimelineElementEntity.builder()
                .paId("PaId")
                .iun("iun")
                .category(TimelineElementCategoryEntity.SEND_ANALOG_DOMICILE)
                .notificationSentAt(Instant.now())
                .ingestionTimestamp(Instant.now())
                .eventTimestamp(Instant.EPOCH)
                .timestamp(Instant.EPOCH)
                .details(
                        TimelineElementDetailsEntity.builder()
                                .recIndex(0)
                                .physicalAddress(
                                        PhysicalAddressEntity.builder()
                                                .address("addr")
                                                .at("at")
                                                .foreignState("IT")
                                                .build()
                                )
                                .serviceLevel(ServiceLevelEntity.REGISTERED_LETTER_890)
                                .sentAttemptMade(0)
                                .relatedRequestId("abc")
                                .productType("NR_AR")
                                .analogCost(100)
                                .build()
                )
                .build();

        TimelineElementInternal actual = mapper.entityToDto(entity);

        Assertions.assertEquals(entity.getIun(), actual.getIun());
        Assertions.assertEquals(entity.getTimelineElementId(), actual.getElementId());
        Assertions.assertEquals(entity.getNotificationSentAt(), actual.getNotificationSentAt());
        Assertions.assertEquals(entity.getTimestamp(), actual.getTimestamp());
        Assertions.assertEquals(entity.getIngestionTimestamp(), actual.getIngestionTimestamp());
        Assertions.assertEquals(entity.getEventTimestamp(), actual.getEventTimestamp());

        Assertions.assertEquals(entity.getPaId(), actual.getPaId());
        Assertions.assertEquals(entity.getCategory().name(), actual.getCategory().name());

        SendAnalogDetailsInt details = (SendAnalogDetailsInt) actual.getDetails();

        Assertions.assertEquals(entity.getDetails().getRecIndex(), details.getRecIndex());
        Assertions.assertEquals(entity.getDetails().getSentAttemptMade(), details.getSentAttemptMade());
        Assertions.assertEquals(entity.getDetails().getRelatedRequestId(), details.getRelatedRequestId());
        Assertions.assertEquals(entity.getDetails().getServiceLevel().getValue(), details.getServiceLevel().getValue());
        Assertions.assertEquals(entity.getDetails().getAnalogCost(), details.getAnalogCost());
        Assertions.assertEquals(entity.getDetails().getProductType(), details.getProductType());
        Assertions.assertEquals(entity.getDetails().getAnalogCost(), details.getAnalogCost());
        Assertions.assertEquals(entity.getDetails().getPhysicalAddress().getAddress(), details.getPhysicalAddress().getAddress());
        Assertions.assertEquals(entity.getDetails().getPhysicalAddress().getForeignState(), details.getPhysicalAddress().getForeignState());
    }
}