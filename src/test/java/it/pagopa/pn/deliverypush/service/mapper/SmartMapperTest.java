package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.deliverypush.dto.address.DigitalAddressSourceInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.*;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DigitalAddress;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DigitalAddressSource;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementDetailsV20;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SmartMapperTest {
    @Test
    void fromInternalToExternalSendDigitalDetails() {
        SendDigitalDetailsInt sendDigitalDetails = SendDigitalDetailsInt.builder()
                .recIndex(0)
                .digitalAddressSource(DigitalAddressSourceInt.PLATFORM)
                .digitalAddress(LegalDigitalAddressInt.builder()
                        .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                        .address("testAddress@gmail.com")
                        .build())
                .retryNumber(0)
                .downstreamId(DownstreamIdInt.builder()
                        .messageId("messageId")
                        .systemId("systemId")
                        .build())
                .build();

        var details = SmartMapper.mapToClass(sendDigitalDetails, TimelineElementDetailsV20.class);
        
        Assertions.assertEquals(sendDigitalDetails.getRecIndex(), details.getRecIndex());
        Assertions.assertEquals(sendDigitalDetails.getDigitalAddress().getAddress(), details.getDigitalAddress().getAddress() );
    }

    @Test
    void fromExternalToInternalSendDigitalDetails() {
        var timelineElementDetails = TimelineElementDetailsV20.builder()
                .recIndex(0)
                .digitalAddressSource(DigitalAddressSource.PLATFORM)
                .digitalAddress(DigitalAddress.builder()
                        .type("PEC")
                        .address("testAddress@gmail.com")
                        .build())
                .retryNumber(0)
                .build();

        SendDigitalDetailsInt details = SmartMapper.mapToClass(timelineElementDetails, SendDigitalDetailsInt.class);

        Assertions.assertEquals(timelineElementDetails.getRecIndex(), details.getRecIndex());
        Assertions.assertEquals(timelineElementDetails.getDigitalAddress().getAddress(), details.getDigitalAddress().getAddress() );
    }

    @Test
    void fromInternalToPrepareAnalogDomicileFailureDetails() {
        PrepareAnalogDomicileFailureDetailsInt sendDigitalDetails = PrepareAnalogDomicileFailureDetailsInt.builder()
                .recIndex(0)
                .foundAddress(PhysicalAddressInt.builder()
                        .foreignState("ITALIA")
                        .zip("30000")
                        .address("Via casa mia")
                        .province("MI")
                        .build())
                .build();

        var details = SmartMapper.mapToClass(sendDigitalDetails, TimelineElementDetailsV20.class);

        Assertions.assertEquals(sendDigitalDetails.getRecIndex(), details.getRecIndex());
        Assertions.assertEquals(sendDigitalDetails.getFoundAddress().getAddress(), details.getFoundAddress().getAddress() );
        Assertions.assertNull(details.getPhysicalAddress() );
    }

    @Test
    void testNotRefinedRecipientPostMappingTransformer(){
        NotificationCancelledDetailsInt source = new NotificationCancelledDetailsInt();
        List<Integer> list = new ArrayList<>();
        list.add(1);
        source.setNotRefinedRecipientIndexes(list);
        source.setNotificationCost(100);

        TimelineElementDetailsV20 ret = SmartMapper.mapToClass(source, TimelineElementDetailsV20.class);

        Assertions.assertEquals(1, ret.getNotRefinedRecipientIndexes().size());

        source.getNotRefinedRecipientIndexes().clear();
        ret = SmartMapper.mapToClass(source, TimelineElementDetailsV20.class);

        Assertions.assertEquals(0, ret.getNotRefinedRecipientIndexes().size());

        NotHandledDetailsInt altro = new NotHandledDetailsInt();
        altro.setReason("test");
        ret = SmartMapper.mapToClass(altro, TimelineElementDetailsV20.class);

        Assertions.assertNull(ret.getNotRefinedRecipientIndexes());
    }


    @Test
    void testTimelineElementInternalMappingTransformer(){
        Instant elementTimestamp = Instant.EPOCH.plusMillis(100);

        Instant eventTimestamp = Instant.EPOCH.plusMillis(10);

        TimelineElementInternal source = TimelineElementInternal.builder()
                .elementId("elementid")
                .iun("iun")
                .timestamp(elementTimestamp)
                .details(SendDigitalFeedbackDetailsInt.builder()
                        .notificationDate(eventTimestamp)
                        .build())
                .build();

        TimelineElementInternal ret = SmartMapper.mapToClass(source, TimelineElementInternal.class);

        Assertions.assertNotSame(ret, source);
        Assertions.assertEquals(eventTimestamp, ret.getTimestamp());
    }

    @Test
    void testMapTimelineInternalTransformer(){
        Instant refinementTimestamp = Instant.EPOCH.plusMillis(100);
        Instant scheduleRefinementTimestamp = Instant.EPOCH.plusMillis(500);

        Instant eventTimestamp = Instant.EPOCH.plusMillis(10);


        TimelineElementInternal refinementElement = TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.REFINEMENT)
                .elementId("elementid")
                .iun("iun")
                .timestamp(refinementTimestamp)
                .details( RefinementDetailsInt.builder()
                        .recIndex(0)
                        .build())
                .build();

        TimelineElementInternal scheduleRefinementElement = TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.SCHEDULE_REFINEMENT)
                .elementId("elementid")
                .iun("iun")
                .timestamp(Instant.now())
                .details( ScheduleRefinementDetailsInt.builder()
                        .recIndex(0)
                        .schedulingDate(scheduleRefinementTimestamp)
                        .build())
                .build();

        TimelineElementInternal ret = SmartMapper.mapTimelineInternal(refinementElement, Set.of(scheduleRefinementElement));

        Assertions.assertNotSame(ret , refinementElement);
        Assertions.assertNotEquals(ret.getTimestamp(),refinementElement.getTimestamp());
        Assertions.assertNotEquals(refinementTimestamp, ret.getTimestamp());
        Assertions.assertNotEquals(eventTimestamp, ret.getTimestamp());
        Assertions.assertEquals(scheduleRefinementTimestamp, ret.getTimestamp());
    }





    @Test
    void testTimelineElementInternalMappingTransformerNo1(){
        Instant elementTimestamp = Instant.EPOCH.plusMillis(100);

        Instant eventTimestamp = Instant.EPOCH.plusMillis(10);

        TimelineElementInternal source = TimelineElementInternal.builder()
                .elementId("elementid")
                .iun("iun")
                .timestamp(elementTimestamp)
                .details(SendDigitalFeedbackDetailsInt.builder()
                        .notificationDate(null)
                        .build())
                .build();

        TimelineElementInternal ret = SmartMapper.mapToClass(source, TimelineElementInternal.class);


        Assertions.assertEquals(elementTimestamp, ret.getTimestamp());
    }

    @Test
    void testTimelineElementInternalMappingTransformerNo2(){
        Instant elementTimestamp = Instant.EPOCH.plusMillis(100);

        Instant eventTimestamp = Instant.EPOCH.plusMillis(10);

        TimelineElementInternal source = TimelineElementInternal.builder()
                .elementId("elementid")
                .iun("iun")
                .timestamp(elementTimestamp)
                .details(AarGenerationDetailsInt.builder()
                        .build())
                .build();

        TimelineElementInternal ret = SmartMapper.mapToClass(source, TimelineElementInternal.class);


        Assertions.assertEquals(elementTimestamp, ret.getTimestamp());
    }
}