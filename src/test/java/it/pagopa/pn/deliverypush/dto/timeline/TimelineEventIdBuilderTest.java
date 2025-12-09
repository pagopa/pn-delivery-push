package it.pagopa.pn.deliverypush.dto.timeline;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TimelineEventIdBuilderTest {

    private static final String IUN = "KWKU-JHXN-HJXM-202304-U-1";

    @Test
    void buildANALOG_WORKFLOW_RECIPIENT_DECEASEDTest() {
        //vecchia versione 123456789_analog_success_workflow_1
        String timeLineEventIdExpected = "ANALOG_WORKFLOW_RECIPIENT_DECEASED.IUN_KWKU-JHXN-HJXM-202304-U-1.RECINDEX_0";
        String timeLineEventIdActual = new TimelineEventIdBuilder()
                .withCategory(TimelineEventId.ANALOG_WORKFLOW_RECIPIENT_DECEASED.getValue())
                .withIun(IUN)
                .withRecIndex(0)
                .build();

        assertThat(timeLineEventIdActual).isEqualTo(timeLineEventIdExpected);

        String timeLineEventIdActualFromBuildEvent = TimelineEventId.ANALOG_WORKFLOW_RECIPIENT_DECEASED.buildEventId(EventId
                .builder()
                .iun(IUN)
                .recIndex(0)
                .build());


        assertThat(timeLineEventIdActualFromBuildEvent).isEqualTo(timeLineEventIdExpected);

    }

    @Test
    void buildNOTIFICATION_VIEWED_CREATION_REQUESTTest() {
        //vecchia versione notification_viewed_creation_request_iun_123456789_RECINDEX_0
        String timeLineEventIdExpected = "NOTIFICATION_VIEWED_CREATION_REQUEST.IUN_KWKU-JHXN-HJXM-202304-U-1.RECINDEX_0";
        String timeLineEventIdActual = new TimelineEventIdBuilder()
                .withCategory(TimelineEventId.NOTIFICATION_VIEWED_CREATION_REQUEST.getValue())
                .withIun(IUN)
                .withRecIndex(0)
                .build();

        assertThat(timeLineEventIdActual).isEqualTo(timeLineEventIdExpected);

        String timeLineEventIdActualFromBuildEvent = TimelineEventId.NOTIFICATION_VIEWED_CREATION_REQUEST.buildEventId(EventId
                .builder()
                .iun(IUN)
                .recIndex(0)
                .build());


        assertThat(timeLineEventIdActualFromBuildEvent).isEqualTo(timeLineEventIdExpected);

    }

    @Test
    void buildNOTIFICATION_VIEWEDTest() {
        //vecchia versione 123456789_notification_viewed_1
        String timeLineEventIdExpected = "NOTIFICATION_VIEWED.IUN_KWKU-JHXN-HJXM-202304-U-1.RECINDEX_0";
        String timeLineEventIdActual = new TimelineEventIdBuilder()
                .withCategory(TimelineEventId.NOTIFICATION_VIEWED.getValue())
                .withIun(IUN)
                .withRecIndex(0)
                .build();

        assertThat(timeLineEventIdActual).isEqualTo(timeLineEventIdExpected);

        String timeLineEventIdActualFromBuildEvent = TimelineEventId.NOTIFICATION_VIEWED.buildEventId(EventId
                .builder()
                .iun(IUN)
                .recIndex(0)
                .build());


        assertThat(timeLineEventIdActualFromBuildEvent).isEqualTo(timeLineEventIdExpected);

    }

    @Test
    void buildREFINEMENTTest() {
        //vecchia versione 123456789_refinement_1
        String timeLineEventIdExpected = "REFINEMENT.IUN_KWKU-JHXN-HJXM-202304-U-1.RECINDEX_0";
        String timeLineEventIdActual = new TimelineEventIdBuilder()
                .withCategory(TimelineEventId.REFINEMENT.getValue())
                .withIun(IUN)
                .withRecIndex(0)
                .build();

        assertThat(timeLineEventIdActual).isEqualTo(timeLineEventIdExpected);

        String timeLineEventIdActualFromBuildEvent = TimelineEventId.REFINEMENT.buildEventId(EventId
                .builder()
                .iun(IUN)
                .recIndex(0)
                .build());


        assertThat(timeLineEventIdActualFromBuildEvent).isEqualTo(timeLineEventIdExpected);

    }

    @Test
    void buildREQUEST_REFUSEDTest() {
        //vecchia versione 123456789_request_refused
        String timeLineEventIdExpected = "REQUEST_REFUSED.IUN_KWKU-JHXN-HJXM-202304-U-1";
        String timeLineEventIdActual = new TimelineEventIdBuilder()
                .withCategory(TimelineEventId.REQUEST_REFUSED.getValue())
                .withIun(IUN)
                .build();

        assertThat(timeLineEventIdActual).isEqualTo(timeLineEventIdExpected);

        String timeLineEventIdActualFromBuildEvent = TimelineEventId.REQUEST_REFUSED.buildEventId(EventId
                .builder()
                .iun(IUN)
                .build());


        assertThat(timeLineEventIdActualFromBuildEvent).isEqualTo(timeLineEventIdExpected);

    }

    @Test
    void buildAAR_GENERATIONTest() {
        //vecchia versione 123456789_aar_gen_1
        String timeLineEventIdExpected = "AAR_GEN.IUN_KWKU-JHXN-HJXM-202304-U-1.RECINDEX_0";
        String timeLineEventIdActual = new TimelineEventIdBuilder()
                .withCategory(TimelineEventId.AAR_GENERATION.getValue())
                .withIun(IUN)
                .withRecIndex(0)
                .build();

        assertThat(timeLineEventIdActual).isEqualTo(timeLineEventIdExpected);

        String timeLineEventIdActualFromBuildEvent = TimelineEventId.AAR_GENERATION.buildEventId(EventId
                .builder()
                .iun(IUN)
                .recIndex(0)
                .build());


        assertThat(timeLineEventIdActualFromBuildEvent).isEqualTo(timeLineEventIdExpected);

    }

    @Test
    void buildNOTIFICATION_CANCELLATION_REQUESTTest() {
        String timeLineEventIdExpected = String.format("NOTIFICATION_CANCELLATION_REQUEST.IUN_%s", IUN);
        String timeLineEventIdActual = new TimelineEventIdBuilder()
                .withCategory(TimelineEventId.NOTIFICATION_CANCELLATION_REQUEST.getValue())
                .withIun(IUN)
                .build();

        assertThat(timeLineEventIdActual).isEqualTo(timeLineEventIdExpected);

        String timeLineEventIdActualFromBuildEvent = TimelineEventId.NOTIFICATION_CANCELLATION_REQUEST.buildEventId(EventId
                .builder()
                .iun(IUN)
                .build());

        assertThat(timeLineEventIdActualFromBuildEvent).isEqualTo(timeLineEventIdExpected);
    }

    @Test
    void buildNOTIFICATION_CANCELLEDTest() {
        String timeLineEventIdExpected = String.format("NOTIFICATION_CANCELLED.IUN_%s", IUN);
        String timeLineEventIdActual = new TimelineEventIdBuilder()
                .withCategory(TimelineEventId.NOTIFICATION_CANCELLED.getValue())
                .withIun(IUN)
                .build();

        assertThat(timeLineEventIdActual).isEqualTo(timeLineEventIdExpected);

        String timeLineEventIdActualFromBuildEvent = TimelineEventId.NOTIFICATION_CANCELLED.buildEventId(EventId
                .builder()
                .iun(IUN)
                .build());

        assertThat(timeLineEventIdActualFromBuildEvent).isEqualTo(timeLineEventIdExpected);
    }

    @Test
    void buildNOTIFICATION_RADD_RETRIEVEDTest() {

        EventId eventId = new EventId().toBuilder().iun("testIun").recIndex(1).build();
        String expectedEventId = "NOTIFICATION_RADD_RETRIEVED.IUN_testIun.RECINDEX_1";

        String actualEventId = TimelineEventId.NOTIFICATION_RADD_RETRIEVED.buildEventId(eventId);

        assertEquals(expectedEventId, actualEventId);
    }

}
