package it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.mapper;

import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.PublicRegistryCallDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendAnalogDetailsInt;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

class EntityToDtoTimelineMapperTest {
    private EntityToDtoTimelineMapper mapper;
    
    @Test
    void entityToDtoSendAnalogDomicile() {
        mapper = new EntityToDtoTimelineMapper();

        TimelineElementEntity entity = TimelineElementEntity.builder()
                .paId("PaId")
                .iun("iun")
                .category(TimelineElementCategoryEntity.SEND_ANALOG_DOMICILE)
                .notificationSentAt(Instant.now())
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
                                .investigation(true)
                                .build()  
                )
                .build();

        TimelineElementInternal actual = mapper.entityToDto(entity);

        Assertions.assertEquals(entity.getIun(), actual.getIun());
        Assertions.assertEquals(entity.getTimelineElementId(), actual.getElementId());
        Assertions.assertEquals(entity.getNotificationSentAt(), actual.getNotificationSentAt());
        Assertions.assertEquals(entity.getTimestamp(), actual.getTimestamp());
        Assertions.assertEquals(entity.getPaId(), actual.getPaId());
        Assertions.assertEquals(entity.getCategory().name(), actual.getCategory().name());

        SendAnalogDetailsInt details = (SendAnalogDetailsInt) actual.getDetails();
        
        Assertions.assertEquals(entity.getDetails().getRecIndex(), details.getRecIndex());
        Assertions.assertEquals(entity.getDetails().getSentAttemptMade(), details.getSentAttemptMade());
        Assertions.assertEquals(entity.getDetails().getInvestigation(), details.getInvestigation());
        Assertions.assertEquals(entity.getDetails().getServiceLevel().getValue(), details.getServiceLevel().getValue());
        Assertions.assertEquals(entity.getDetails().getPhysicalAddress().getAddress(), details.getPhysicalAddress().getAddress());
        Assertions.assertEquals(entity.getDetails().getPhysicalAddress().getForeignState(), details.getPhysicalAddress().getForeignState());
    }
    
    @Test
    void entityToDto() {
        mapper = new EntityToDtoTimelineMapper();
        
        TimelineElementEntity entity = TimelineElementEntity.builder()
                .paId("PaId")
                .iun("iun")
                .category(TimelineElementCategoryEntity.PUBLIC_REGISTRY_CALL)
                .details(
                        TimelineElementDetailsEntity.builder()
                                .recIndex(0)
                                .deliveryMode(
                                        DeliveryModeEntity.DIGITAL
                                )
                                .contactPhase(
                                        ContactPhaseEntity.SEND_ATTEMPT
                                )
                                .sentAttemptMade(0)
                                .sendDate(Instant.now())
                                .build()
                )
                .build();
        
        TimelineElementInternal internal = mapper.entityToDto(entity);
        PublicRegistryCallDetailsInt details = (PublicRegistryCallDetailsInt) internal.getDetails();
        
        Assertions.assertEquals(entity.getDetails().getRecIndex(), details.getRecIndex());
        Assertions.assertEquals(entity.getDetails().getSentAttemptMade(), details.getSentAttemptMade());
        Assertions.assertEquals(entity.getDetails().getDeliveryMode().getValue(), details.getDeliveryMode().getValue());
    }
    
}