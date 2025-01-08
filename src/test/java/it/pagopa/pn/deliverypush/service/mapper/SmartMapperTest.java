package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressSourceInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.*;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DigitalAddress;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DigitalAddressSource;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementDetailsV26;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

class SmartMapperTest {

    private SmartMapper smartMapper;

    @BeforeEach
    void setUp() {
        PnDeliveryPushConfigs pnDeliveryPushConfigs = Mockito.mock(PnDeliveryPushConfigs.class);
        Mockito.when(pnDeliveryPushConfigs.getFeatureUnreachableRefinementPostAARStartDate()).thenReturn(Instant.now());
        smartMapper = new SmartMapper(new TimelineMapperFactory(pnDeliveryPushConfigs));
    }

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

        var details = SmartMapper.mapToClass(sendDigitalDetails, TimelineElementDetailsV26.class);
        
        Assertions.assertEquals(sendDigitalDetails.getRecIndex(), details.getRecIndex());
        Assertions.assertEquals(sendDigitalDetails.getDigitalAddress().getAddress(), details.getDigitalAddress().getAddress() );
    }

    @Test
    void fromExternalToInternalSendDigitalDetails() {
        var timelineElementDetails = TimelineElementDetailsV26.builder()
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

        var details = SmartMapper.mapToClass(sendDigitalDetails, TimelineElementDetailsV26.class);

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

        TimelineElementDetailsV26 ret = SmartMapper.mapToClass(source, TimelineElementDetailsV26.class);

        Assertions.assertEquals(1, ret.getNotRefinedRecipientIndexes().size());

        source.getNotRefinedRecipientIndexes().clear();
        ret = SmartMapper.mapToClass(source, TimelineElementDetailsV26.class);

        Assertions.assertEquals(0, ret.getNotRefinedRecipientIndexes().size());

        NotHandledDetailsInt altro = new NotHandledDetailsInt();
        altro.setReason("test");
        ret = SmartMapper.mapToClass(altro, TimelineElementDetailsV26.class);

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
    void testMapSendAnalogProgress(){
        Instant sourceEventTimestamp = Instant.EPOCH;
        Instant sourceIngestionTimestamp = Instant.now();
        
        TimelineElementInternal sendAnalogProgress = TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.SEND_ANALOG_PROGRESS)
                .elementId("elementid")
                .iun("iun")
                .timestamp(sourceIngestionTimestamp)
                .details( SendAnalogProgressDetailsInt.builder()
                        .recIndex(0)
                        .notificationDate(sourceEventTimestamp)
                        .build())
                .build();


        TimelineElementInternal ret = smartMapper.mapTimelineInternal(sendAnalogProgress, Set.of(sendAnalogProgress));

        Assertions.assertNotSame(ret , sendAnalogProgress);
        Assertions.assertNotEquals(ret.getTimestamp(),sendAnalogProgress.getTimestamp());
        Assertions.assertEquals(sourceIngestionTimestamp, ret.getIngestionTimestamp());
        Assertions.assertEquals(sourceEventTimestamp, ret.getEventTimestamp());
        Assertions.assertEquals(sourceEventTimestamp, ret.getTimestamp());
    }

    @Test
    void testMapSendAnalogFeedback(){
        Instant sourceEventTimestamp = Instant.EPOCH;
        Instant sourceIngestionTimestamp = Instant.now();

        TimelineElementInternal sendAnalogFeedback = TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK)
                .elementId("elementid")
                .iun("iun")
                .timestamp(sourceIngestionTimestamp)
                .details( SendAnalogFeedbackDetailsInt.builder()
                        .recIndex(0)
                        .notificationDate(sourceEventTimestamp)
                        .build())
                .build();
        
        TimelineElementInternal ret = smartMapper.mapTimelineInternal(sendAnalogFeedback, Set.of(sendAnalogFeedback));

        Assertions.assertNotSame(ret , sendAnalogFeedback);
        Assertions.assertNotEquals(ret.getTimestamp(),sendAnalogFeedback.getTimestamp());
        Assertions.assertEquals(sourceIngestionTimestamp, ret.getIngestionTimestamp());
        Assertions.assertEquals(sourceEventTimestamp, ret.getEventTimestamp());
        Assertions.assertEquals(sourceEventTimestamp, ret.getTimestamp());
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

        TimelineElementInternal ret = smartMapper.mapTimelineInternal(refinementElement, Set.of(scheduleRefinementElement));

        Assertions.assertNotSame(ret , refinementElement);
        Assertions.assertNotEquals(ret.getTimestamp(),refinementElement.getTimestamp());
        Assertions.assertNotEquals(refinementTimestamp, ret.getTimestamp());
        Assertions.assertNotEquals(eventTimestamp, ret.getTimestamp());
        Assertions.assertEquals(scheduleRefinementTimestamp, ret.getTimestamp());
    }


    @Test
    void testMapTimelineInternalMapTimelineInternaNotificationView(){
        Instant notificationViewedTimestamp = Instant.EPOCH.plusMillis(100);
        Instant eventTimestamp = Instant.EPOCH.plusMillis(10);


        TimelineElementInternal notificationViewedElement = TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.NOTIFICATION_VIEWED)
                .elementId("elementid")
                .iun("iun")
                .timestamp(notificationViewedTimestamp)
                .details( NotificationViewedDetailsInt.builder()
                        .recIndex(0)
                        .eventTimestamp(eventTimestamp)
                        .build())
                .build();


        TimelineElementInternal ret = smartMapper.mapTimelineInternal(notificationViewedElement, Set.of(notificationViewedElement));

        Assertions.assertNotSame(ret , notificationViewedElement);
        Assertions.assertEquals(eventTimestamp, ret.getTimestamp());
    }

    @Test
    void testMapScheduleRefinement_afterFix() {
        Instant sourceEventTimestamp = Instant.EPOCH;
        Instant sourceIngestionTimestamp = Instant.now();

        TimelineElementInternal scheduleRefinement = TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.SCHEDULE_REFINEMENT)
                .details(ScheduleRefinementDetailsInt.builder().recIndex(0).build())
                .timestamp(sourceIngestionTimestamp)
                .notificationSentAt(Instant.now().plusSeconds(3600))
                .build();
        Set<TimelineElementInternal> timelineElementInternalSet = Set.of(
                TimelineElementInternal.builder()
                        .category(TimelineElementCategoryInt.ANALOG_FAILURE_WORKFLOW)
                        .timestamp(sourceEventTimestamp)
                        .details(AnalogFailureWorkflowDetailsInt.builder().recIndex(0).build())
                        .build()
        );

        TimelineElementInternal ret = smartMapper.mapTimelineInternal(scheduleRefinement, timelineElementInternalSet);

        Assertions.assertNotSame(ret, scheduleRefinement);
        Assertions.assertEquals(sourceIngestionTimestamp, ret.getIngestionTimestamp());
        Assertions.assertEquals(sourceEventTimestamp, ret.getTimestamp());
        Assertions.assertEquals(sourceEventTimestamp, ret.getEventTimestamp());
    }

    @Test
    void testMapScheduleRefinement_analogFailureWorkflowDateNull_afterFix() {
        Instant sourceEventTimestamp = Instant.EPOCH;
        Instant sourceIngestionTimestamp = Instant.now();

        TimelineElementInternal scheduleRefinement = TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.SCHEDULE_REFINEMENT)
                .details(ScheduleRefinementDetailsInt.builder().recIndex(0).build())
                .timestamp(sourceIngestionTimestamp)
                .notificationSentAt(Instant.now().plusSeconds(3600))
                .build();
        Set<TimelineElementInternal> timelineElementInternalSet = Set.of(
                TimelineElementInternal.builder()
                        .category(TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK)
                        .timestamp(sourceEventTimestamp)
                        .details(SendAnalogFeedbackDetailsInt.builder().recIndex(0).notificationDate(sourceEventTimestamp).build())
                        .build()
        );

        TimelineElementInternal ret = smartMapper.mapTimelineInternal(scheduleRefinement, timelineElementInternalSet);

        Assertions.assertNotSame(ret, scheduleRefinement);
        Assertions.assertEquals(sourceIngestionTimestamp, ret.getIngestionTimestamp());
        Assertions.assertEquals(sourceEventTimestamp, ret.getTimestamp());
        Assertions.assertEquals(sourceEventTimestamp, ret.getEventTimestamp());
    }

    @Test
    void testMapAnalogSuccessWorkflow_afterFix() {
        Instant sourceEventTimestamp = Instant.EPOCH;
        Instant sourceIngestionTimestamp = Instant.now();

        TimelineElementInternal analogSuccessWorkflow = TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.ANALOG_SUCCESS_WORKFLOW)
                .details(AnalogSuccessWorkflowDetailsInt.builder().recIndex(0).build())
                .timestamp(sourceIngestionTimestamp)
                .notificationSentAt(Instant.now().plusSeconds(3600))
                .build();
        Set<TimelineElementInternal> timelineElementInternalSet = Set.of(
                TimelineElementInternal.builder()
                        .category(TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK)
                        .timestamp(sourceEventTimestamp)
                        .details(SendAnalogFeedbackDetailsInt.builder().recIndex(0).notificationDate(sourceEventTimestamp).build())
                        .build()
        );

        TimelineElementInternal ret = smartMapper.mapTimelineInternal(analogSuccessWorkflow, timelineElementInternalSet);

        Assertions.assertNotSame(ret, analogSuccessWorkflow);
        Assertions.assertEquals(sourceIngestionTimestamp, ret.getIngestionTimestamp());
        Assertions.assertEquals(sourceEventTimestamp, ret.getTimestamp());
        Assertions.assertEquals(sourceEventTimestamp, ret.getEventTimestamp());
    }

    @Test
    void testMapAnalogSuccessWorkflow_analogWorkflowBusinessDateNull_afterFix() {
        Instant sourceEventTimestamp = Instant.EPOCH;
        Instant sourceIngestionTimestamp = Instant.now();

        TimelineElementInternal analogSuccessWorkflow = TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.ANALOG_SUCCESS_WORKFLOW)
                .details(AnalogSuccessWorkflowDetailsInt.builder().recIndex(0).build())
                .timestamp(sourceIngestionTimestamp)
                .notificationSentAt(Instant.now().plusSeconds(3600))
                .build();
        Set<TimelineElementInternal> timelineElementInternalSet = Set.of(
                TimelineElementInternal.builder()
                        .category(TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK)
                        .timestamp(sourceEventTimestamp)
                        .details(SendAnalogFeedbackDetailsInt.builder().recIndex(1).build())
                        .build()
        );

        Assertions.assertThrows(PnInternalException.class, () -> smartMapper.mapTimelineInternal(analogSuccessWorkflow, timelineElementInternalSet));
    }

    @Test
    void testMapCompletelyUnreachable_afterFix() {
        Instant sourceEventTimestamp = Instant.EPOCH;
        Instant sourceIngestionTimestamp = Instant.now();

        TimelineElementInternal completelyUnreachable = TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.COMPLETELY_UNREACHABLE)
                .details(CompletelyUnreachableDetailsInt.builder().recIndex(0).build())
                .timestamp(sourceIngestionTimestamp)
                .notificationSentAt(Instant.now().plusSeconds(3600))
                .build();
        Set<TimelineElementInternal> timelineElementInternalSet = Set.of(
                TimelineElementInternal.builder()
                        .category(TimelineElementCategoryInt.ANALOG_FAILURE_WORKFLOW)
                        .timestamp(sourceEventTimestamp)
                        .details(AnalogFailureWorkflowDetailsInt.builder().recIndex(0).build())
                        .build()
        );

        TimelineElementInternal ret = smartMapper.mapTimelineInternal(completelyUnreachable, timelineElementInternalSet);

        Assertions.assertNotSame(ret, completelyUnreachable);
        Assertions.assertEquals(sourceIngestionTimestamp, ret.getIngestionTimestamp());
        Assertions.assertEquals(sourceEventTimestamp, ret.getTimestamp());
        Assertions.assertEquals(sourceEventTimestamp, ret.getEventTimestamp());
    }

    @Test
    void testMapCompletelyUnreachable_analogFailureWorkflowDateNull_afterFix() {
        Instant sourceEventTimestamp = Instant.EPOCH;
        Instant sourceIngestionTimestamp = Instant.now();

        TimelineElementInternal completelyUnreachable = TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.COMPLETELY_UNREACHABLE)
                .details(CompletelyUnreachableDetailsInt.builder().recIndex(0).build())
                .timestamp(sourceIngestionTimestamp)
                .notificationSentAt(Instant.now().plusSeconds(3600))
                .build();
        Set<TimelineElementInternal> timelineElementInternalSet = Set.of(
                TimelineElementInternal.builder()
                        .category(TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK)
                        .timestamp(sourceEventTimestamp)
                        .details(SendAnalogFeedbackDetailsInt.builder().recIndex(0).build())
                        .build()
        );

        Assertions.assertThrows(PnInternalException.class, () -> smartMapper.mapTimelineInternal(completelyUnreachable, timelineElementInternalSet));
    }

    @Test
    void testMapRefinement_afterFix() {
        Instant sourceEventTimestamp = Instant.EPOCH;
        Instant sourceIngestionTimestamp = Instant.now();
        Instant schedulingDate = Instant.now().minusSeconds(3600);

        TimelineElementInternal refinement = TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.REFINEMENT)
                .details(RefinementDetailsInt.builder().recIndex(0).build())
                .timestamp(sourceIngestionTimestamp)
                .notificationSentAt(Instant.now().plusSeconds(3600))
                .build();
        Set<TimelineElementInternal> timelineElementInternalSet = Set.of(
                TimelineElementInternal.builder()
                        .category(TimelineElementCategoryInt.SCHEDULE_REFINEMENT)
                        .timestamp(sourceEventTimestamp)
                        .details(ScheduleRefinementDetailsInt.builder().recIndex(0).schedulingDate(schedulingDate).build())
                        .build()
        );

        TimelineElementInternal ret = smartMapper.mapTimelineInternal(refinement, timelineElementInternalSet);

        Assertions.assertNotSame(ret, refinement);
        Assertions.assertEquals(sourceIngestionTimestamp, ret.getIngestionTimestamp());
        Assertions.assertEquals(schedulingDate, ret.getTimestamp());
        Assertions.assertEquals(schedulingDate, ret.getEventTimestamp());
    }

    @Test
    void testMapSendDigitalDomicile_afterFix() {
        Instant sourceEventTimestamp = Instant.EPOCH;
        Instant sourceIngestionTimestamp = Instant.now();

        TimelineElementInternal sendDigitalDomicile = TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE)
                .details(SendDigitalDetailsInt.builder().
                        digitalAddress(LegalDigitalAddressInt.builder().type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.SERCQ).build()).
                        recIndex(0).build())
                .timestamp(sourceIngestionTimestamp)
                .notificationSentAt(Instant.now().plusSeconds(3600))
                .build();
        Set<TimelineElementInternal> timelineElementInternalSet = Set.of(
                TimelineElementInternal.builder()
                        .category(TimelineElementCategoryInt.AAR_GENERATION)
                        .timestamp(sourceEventTimestamp)
                        .details(AarGenerationDetailsInt.builder().recIndex(0).build())
                        .build()
        );

        TimelineElementInternal ret = smartMapper.mapTimelineInternal(sendDigitalDomicile, timelineElementInternalSet);

        Assertions.assertNotSame(ret, sendDigitalDomicile);
        Assertions.assertEquals(sourceIngestionTimestamp, ret.getIngestionTimestamp());
        Assertions.assertEquals(sourceEventTimestamp, ret.getTimestamp());
        Assertions.assertEquals(sourceEventTimestamp, ret.getEventTimestamp());
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

    @Test
    void testMapAnalogWorkflowRecipientDeceased(){
        Instant sourceEventTimestamp = Instant.EPOCH;
        Instant deceasedSourceIngestionTimestamp = Instant.now();
        Instant analogWorkflowRecipientDeceasedTimestamp = Instant.from(deceasedSourceIngestionTimestamp).minus(2, ChronoUnit.DAYS);

        TimelineElementInternal analogWorkflowRecipientDeceased = TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.ANALOG_WORKFLOW_RECIPIENT_DECEASED)
                .elementId("elementid")
                .iun("iun")
                .timestamp(deceasedSourceIngestionTimestamp)
                .details( AnalogWorfklowRecipientDeceasedDetailsInt.builder()
                        .recIndex(0)
                        .notificationDate(sourceEventTimestamp)
                        .build())
                .build();

        // è necessario avere anche un elemento di tipo SEND_ANALOG_FEEDBACK per poter fare il mapping, poichè è da lì che si prende l'eventTimestamp
        TimelineElementInternal sendAnalogFeedback = TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK)
                .elementId("elementid")
                .iun("iun")
                .timestamp(analogWorkflowRecipientDeceasedTimestamp)
                .details( SendAnalogFeedbackDetailsInt.builder()
                        .recIndex(0)
                        .notificationDate(sourceEventTimestamp)
                        .build())
                .build();

        TimelineElementInternal ret = SmartMapper.mapTimelineInternal(analogWorkflowRecipientDeceased, Set.of(analogWorkflowRecipientDeceased, sendAnalogFeedback));

        Assertions.assertNotSame(ret , analogWorkflowRecipientDeceased);
        Assertions.assertNotEquals(ret.getTimestamp(),analogWorkflowRecipientDeceased.getTimestamp());
        Assertions.assertEquals(deceasedSourceIngestionTimestamp, ret.getIngestionTimestamp());
        Assertions.assertEquals(sourceEventTimestamp, ret.getEventTimestamp());
        Assertions.assertEquals(sourceEventTimestamp, ret.getTimestamp());
    }

    @Test
    void testMapAnalogWorkflowRecipientDeceasedFails(){
        Instant sourceEventTimestamp = Instant.EPOCH;
        Instant deceasedSourceIngestionTimestamp = Instant.now();

        TimelineElementInternal analogWorkflowRecipientDeceased = TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.ANALOG_WORKFLOW_RECIPIENT_DECEASED)
                .elementId("elementid")
                .iun("iun")
                .timestamp(deceasedSourceIngestionTimestamp)
                .details( AnalogWorfklowRecipientDeceasedDetailsInt.builder()
                        .recIndex(0)
                        .notificationDate(sourceEventTimestamp)
                        .build())
                .build();

        Assertions.assertThrows(PnInternalException.class, () -> SmartMapper.mapTimelineInternal(analogWorkflowRecipientDeceased, Set.of(analogWorkflowRecipientDeceased)));

    }
}