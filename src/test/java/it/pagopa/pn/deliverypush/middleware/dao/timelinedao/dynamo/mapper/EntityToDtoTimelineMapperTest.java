package it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.mapper;

import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.AarCreationRequestDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.PublicRegistryCallDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.RequestRefusedDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendAnalogDetailsInt;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

class EntityToDtoTimelineMapperTest {
    private final EntityToDtoTimelineMapper mapper = new EntityToDtoTimelineMapper();
    
    @Test
    void entityToDtoSendAnalogDomicile() {

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

    @Test
    void entityToDtoRefusedError() {

        TimelineElementEntity entity = TimelineElementEntity.builder()
                .paId("PaId")
                .iun("iun")
                .category( TimelineElementCategoryEntity.REQUEST_REFUSED )
                .details( TimelineElementDetailsEntity.builder()
                        .refusalReasons( List.of( NotificationRefusedErrorEntity.builder()
                                .errorCode( "FILE_NOTFOUND" )
                                .detail( "Allegato non trovato. fileKey=81dde2a8-9719-4407-b7b3-63e7ea694869" )
                                .build()
                                )
                        )
                        .build())
                .build();

        TimelineElementInternal actual = mapper.entityToDto(entity);

        RequestRefusedDetailsInt requestRefusedDetailsInt = (RequestRefusedDetailsInt) actual.getDetails();

        Assertions.assertEquals( entity.getDetails().getRefusalReasons().get( 0 ).getErrorCode(), requestRefusedDetailsInt.getRefusalReasons().get( 0 ).getErrorCode() );
    }
    
    @Test
    void entityToDto() {

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

    @Test
    void entityToDtoAarCreationRequest() {

        TimelineElementEntity entity = TimelineElementEntity.builder()
                .paId("PaId")
                .iun("iun")
                .timelineElementId("AAR_CREATION_REQUEST.IUN_AAAA-WLRL-YUKX-202405-Z-1.RECINDEX_0")
                .category( TimelineElementCategoryEntity.AAR_CREATION_REQUEST )
                .notificationSentAt(Instant.now())
                .timestamp(Instant.now())
                .details( TimelineElementDetailsEntity.builder()
                        .aarKey("safestorage://PN_AAR-mock.pdf")
                        .numberOfPages(2)
                        .recIndex(0)
                        .aarWithRadd(true)
                        .build())
                .build();

        TimelineElementInternal actual = mapper.entityToDto(entity);

        AarCreationRequestDetailsInt details = (AarCreationRequestDetailsInt) actual.getDetails();

        Assertions.assertEquals(entity.getDetails().getAarKey(), details.getAarKey());
        Assertions.assertEquals(entity.getDetails().getNumberOfPages(), details.getNumberOfPages());
        Assertions.assertEquals(entity.getDetails().getRecIndex(), details.getRecIndex());
        Assertions.assertEquals(entity.getDetails().getAarWithRadd(), details.getAarWithRadd());
    }
    
}