package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;

class TimelineMapperAfterFixTest {

    private TimelineMapperAfterFix timelineMapperAfterFix;

    @BeforeEach
    void setUp() {
        timelineMapperAfterFix = new TimelineMapperAfterFix();
    }


    @Test
    void testMapScheduleRefinement() {
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

        timelineMapperAfterFix.remapSpecificTimelineElementData(timelineElementInternalSet, scheduleRefinement, sourceIngestionTimestamp);

        Assertions.assertEquals(sourceIngestionTimestamp, scheduleRefinement.getIngestionTimestamp());
        Assertions.assertEquals(sourceEventTimestamp, scheduleRefinement.getTimestamp());
        Assertions.assertEquals(sourceEventTimestamp, scheduleRefinement.getEventTimestamp());
    }

    @Test
    void testMapScheduleRefinement_analogFailureWorkflowDateNull() {
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

        timelineMapperAfterFix.remapSpecificTimelineElementData(timelineElementInternalSet, scheduleRefinement, sourceIngestionTimestamp);

        Assertions.assertEquals(sourceIngestionTimestamp, scheduleRefinement.getIngestionTimestamp());
        Assertions.assertEquals(sourceEventTimestamp, scheduleRefinement.getTimestamp());
        Assertions.assertEquals(sourceEventTimestamp, scheduleRefinement.getEventTimestamp());
    }

    @Test
    void testMapAnalogSuccessWorkflow() {
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

        timelineMapperAfterFix.remapSpecificTimelineElementData(timelineElementInternalSet, analogSuccessWorkflow, sourceIngestionTimestamp);

        Assertions.assertEquals(sourceIngestionTimestamp, analogSuccessWorkflow.getIngestionTimestamp());
        Assertions.assertEquals(sourceEventTimestamp, analogSuccessWorkflow.getTimestamp());
        Assertions.assertEquals(sourceEventTimestamp, analogSuccessWorkflow.getEventTimestamp());
    }

    @Test
    void testMapAnalogSuccessWorkflow_analogWorkflowBusinessDateNull() {
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

        Assertions.assertThrows(PnInternalException.class, () -> timelineMapperAfterFix.remapSpecificTimelineElementData(timelineElementInternalSet, analogSuccessWorkflow, sourceIngestionTimestamp));
    }

    @Test
    void testMapCompletelyUnreachable() {
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

        timelineMapperAfterFix.remapSpecificTimelineElementData(timelineElementInternalSet, completelyUnreachable, sourceIngestionTimestamp);

        Assertions.assertEquals(sourceIngestionTimestamp, completelyUnreachable.getIngestionTimestamp());
        Assertions.assertEquals(sourceEventTimestamp, completelyUnreachable.getTimestamp());
        Assertions.assertEquals(sourceEventTimestamp, completelyUnreachable.getEventTimestamp());
    }

    @Test
    void testMapCompletelyUnreachable_analogFailureWorkflowDateNull() {
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

        Assertions.assertThrows(PnInternalException.class, () -> timelineMapperAfterFix.remapSpecificTimelineElementData(timelineElementInternalSet, completelyUnreachable, sourceIngestionTimestamp));
    }

    @Test
    void testMapRefinement() {
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

        timelineMapperAfterFix.remapSpecificTimelineElementData(timelineElementInternalSet, refinement, sourceIngestionTimestamp);

        Assertions.assertEquals(sourceIngestionTimestamp, refinement.getIngestionTimestamp());
        Assertions.assertEquals(schedulingDate, refinement.getTimestamp());
        Assertions.assertEquals(schedulingDate, refinement.getEventTimestamp());
    }

    @Test
    void testMapSendDigitalDomicile() {
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

        timelineMapperAfterFix.remapSpecificTimelineElementData(timelineElementInternalSet, sendDigitalDomicile, sourceIngestionTimestamp);

        Assertions.assertEquals(sourceIngestionTimestamp, sendDigitalDomicile.getIngestionTimestamp());
        Assertions.assertEquals(sourceEventTimestamp, sendDigitalDomicile.getTimestamp());
        Assertions.assertEquals(sourceEventTimestamp, sendDigitalDomicile.getEventTimestamp());
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

        timelineMapperAfterFix.remapSpecificTimelineElementData(Set.of(analogWorkflowRecipientDeceased, sendAnalogFeedback), analogWorkflowRecipientDeceased, deceasedSourceIngestionTimestamp);

        Assertions.assertEquals(deceasedSourceIngestionTimestamp, analogWorkflowRecipientDeceased.getIngestionTimestamp());
        Assertions.assertEquals(sourceEventTimestamp, analogWorkflowRecipientDeceased.getEventTimestamp());
        Assertions.assertEquals(sourceEventTimestamp, analogWorkflowRecipientDeceased.getTimestamp());
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

        Assertions.assertThrows(PnInternalException.class, () -> timelineMapperAfterFix.remapSpecificTimelineElementData(Set.of(analogWorkflowRecipientDeceased), analogWorkflowRecipientDeceased, deceasedSourceIngestionTimestamp));

    }

}
