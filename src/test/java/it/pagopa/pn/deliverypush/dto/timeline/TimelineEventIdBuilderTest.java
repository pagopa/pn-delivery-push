package it.pagopa.pn.deliverypush.dto.timeline;

import it.pagopa.pn.deliverypush.dto.address.CourtesyDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressSourceInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.ContactPhaseInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.DeliveryModeInt;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TimelineEventIdBuilderTest {

    private static final String IUN = "123456789";


    @Test
    void buildSENDERACK_CREATION_REQUESTTest() {
        String timeLineEventIdExpected = "senderack_legalfact_creation_request-IUN_123456789";
        String timeLineEventIdActual = new TimelineEventIdBuilder()
                .withCategory(TimelineEventId.SENDERACK_CREATION_REQUEST.getValue())
                .withIun(IUN)
                .build();

        assertThat(timeLineEventIdActual).isEqualTo(timeLineEventIdExpected);

        String timeLineEventIdActualFromBuildEvent = TimelineEventId.SENDERACK_CREATION_REQUEST.buildEventId(EventId
                .builder()
                .iun(IUN)
                .build());


        assertThat(timeLineEventIdActualFromBuildEvent).isEqualTo(timeLineEventIdExpected);

    }

    @Test
    void buildREQUEST_ACCEPTEDTest() {
        String timeLineEventIdExpected = "request_accepted-IUN_123456789";
        String timeLineEventIdActual = new TimelineEventIdBuilder()
                .withCategory(TimelineEventId.REQUEST_ACCEPTED.getValue())
                .withIun(IUN)
                .build();

        assertThat(timeLineEventIdActual).isEqualTo(timeLineEventIdExpected);

        String timeLineEventIdActualFromBuildEvent = TimelineEventId.REQUEST_ACCEPTED.buildEventId(EventId
                .builder()
                .iun(IUN)
                .build());


        assertThat(timeLineEventIdActualFromBuildEvent).isEqualTo(timeLineEventIdExpected);

    }

    @Test
    void buildSEND_COURTESY_MESSAGETest() {
        //vecchia versione 123456789_send_courtesy_message_0_type_SMS
        String timeLineEventIdExpected = "send_courtesy_message-IUN_123456789-RECINDEX_0-COURTESYADDRESSTYPE_SMS";
        String timeLineEventIdActual = new TimelineEventIdBuilder()
                .withCategory(TimelineEventId.SEND_COURTESY_MESSAGE.getValue())
                .withIun(IUN)
                .withRecIndex(0)
                .withCourtesyAddressType(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.SMS)
                .build();

        assertThat(timeLineEventIdActual).isEqualTo(timeLineEventIdExpected);

        String timeLineEventIdActualFromBuildEvent = TimelineEventId.SEND_COURTESY_MESSAGE.buildEventId(EventId
                .builder()
                .iun(IUN)
                .recIndex(0)
                .courtesyAddressType(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.SMS)
                .build());


        assertThat(timeLineEventIdActualFromBuildEvent).isEqualTo(timeLineEventIdExpected);

        String elementIdFromIunAndRecIndex = TimelineEventId.SEND_COURTESY_MESSAGE.buildSearchEventIdByIunAndRecipientIndex(IUN, 0);

        //vecchia versione 123456789_send_courtesy_message_0_type_
        assertThat(elementIdFromIunAndRecIndex).isEqualTo("send_courtesy_message-IUN_123456789-RECINDEX_0");

    }

    @Test
    void buildGET_ADDRESSTest() {
        //vecchia versione 123456789_get_address_1_source_PLATFORM_attempt_1
        String timeLineEventIdExpected = "get_address-IUN_123456789-RECINDEX_1-SOURCE_PLATFORM-SENTATTEMPTMADE_1";
        String timeLineEventIdActual = new TimelineEventIdBuilder()
                .withCategory(TimelineEventId.GET_ADDRESS.getValue())
                .withIun(IUN)
                .withRecIndex(1)
                .withSource(DigitalAddressSourceInt.PLATFORM)
                .withSentAttemptMade(1)
                .build();

        assertThat(timeLineEventIdActual).isEqualTo(timeLineEventIdExpected);

        String timeLineEventIdActualFromBuildEvent = TimelineEventId.GET_ADDRESS.buildEventId(EventId
                .builder()
                .iun(IUN)
                .recIndex(1)
                .source(DigitalAddressSourceInt.PLATFORM)
                .sentAttemptMade(1)
                .build());


        assertThat(timeLineEventIdActualFromBuildEvent).isEqualTo(timeLineEventIdExpected);

    }

    @Test
    void buildSEND_DIGITAL_FEEDBACKTest() {
        //vecchia versione 123456789_send_digital_feedback_1_source_PLATFORM_attempt_1
        String timeLineEventIdExpected = "send_digital_feedback-IUN_123456789-RECINDEX_1-SOURCE_PLATFORM-SENTATTEMPTMADE_1";
        String timeLineEventIdActual = new TimelineEventIdBuilder()
                .withCategory(TimelineEventId.SEND_DIGITAL_FEEDBACK.getValue())
                .withIun(IUN)
                .withRecIndex(1)
                .withSource(DigitalAddressSourceInt.PLATFORM)
                .withSentAttemptMade(1)
                .build();

        assertThat(timeLineEventIdActual).isEqualTo(timeLineEventIdExpected);

        String timeLineEventIdActualFromBuildEvent = TimelineEventId.SEND_DIGITAL_FEEDBACK.buildEventId(EventId
                .builder()
                .iun(IUN)
                .recIndex(1)
                .source(DigitalAddressSourceInt.PLATFORM)
                .sentAttemptMade(1)
                .build());


        assertThat(timeLineEventIdActualFromBuildEvent).isEqualTo(timeLineEventIdExpected);

    }

    @Test
    void buildSEND_DIGITAL_PROGRESSTest() {
        //vecchia versione 123456789_digital_delivering_progress_1_source_PLATFORM_attempt_1_progidx_1
        String timeLineEventIdExpected = "digital_delivering_progress-IUN_123456789-RECINDEX_1-SOURCE_PLATFORM-SENTATTEMPTMADE_0-PROGRESSINDEX_1";
        String timeLineEventIdActual = new TimelineEventIdBuilder()
                .withCategory(TimelineEventId.SEND_DIGITAL_PROGRESS.getValue())
                .withIun(IUN)
                .withRecIndex(1)
                .withSource(DigitalAddressSourceInt.PLATFORM)
                .withSentAttemptMade(0)
                .withProgressIndex(1)
                .build();

        assertThat(timeLineEventIdActual).isEqualTo(timeLineEventIdExpected);

        String timeLineEventIdActualFromBuildEvent = TimelineEventId.SEND_DIGITAL_PROGRESS.buildEventId(EventId
                .builder()
                .iun(IUN)
                .recIndex(1)
                .source(DigitalAddressSourceInt.PLATFORM)
                .sentAttemptMade(0)
                .progressIndex(1)
                .build());


        assertThat(timeLineEventIdActualFromBuildEvent).isEqualTo(timeLineEventIdExpected);

        String eventIdByRecAndIndex = TimelineEventId.SEND_DIGITAL_PROGRESS.buildSearchEventIdByIunAndRecipientIndex(IUN, 1);
        //vecchio formato: 123456789_digital_delivering_progress_1_
        assertThat(eventIdByRecAndIndex).isEqualTo("digital_delivering_progress-IUN_123456789-RECINDEX_1");

    }

    @Test
    void buildSEND_ANALOG_FEEDBACKTest() {
        //vecchia versione 123456789_send_analog_feedback_1_attempt_1
        String timeLineEventIdExpected = "send_analog_feedback-IUN_123456789-RECINDEX_1-SENTATTEMPTMADE_1";
        String timeLineEventIdActual = new TimelineEventIdBuilder()
                .withCategory(TimelineEventId.SEND_ANALOG_FEEDBACK.getValue())
                .withIun(IUN)
                .withRecIndex(1)
                .withSentAttemptMade(1)
                .build();

        assertThat(timeLineEventIdActual).isEqualTo(timeLineEventIdExpected);

        String timeLineEventIdActualFromBuildEvent = TimelineEventId.SEND_ANALOG_FEEDBACK.buildEventId(EventId
                .builder()
                .iun(IUN)
                .recIndex(1)
                .sentAttemptMade(1)
                .build());


        assertThat(timeLineEventIdActualFromBuildEvent).isEqualTo(timeLineEventIdExpected);

    }

    @Test
    void buildSSEND_ANALOG_PROGRESSTest() {
        //vecchia versione 123456789_send_analog_progress_1_attempt_1_progidx_1
        String timeLineEventIdExpected = "send_analog_progress-IUN_123456789-RECINDEX_1-SENTATTEMPTMADE_1-PROGRESSINDEX_1";
        String timeLineEventIdActual = new TimelineEventIdBuilder()
                .withCategory(TimelineEventId.SEND_ANALOG_PROGRESS.getValue())
                .withIun(IUN)
                .withRecIndex(1)
                .withSentAttemptMade(1)
                .withProgressIndex(1)
                .build();

        assertThat(timeLineEventIdActual).isEqualTo(timeLineEventIdExpected);

        String timeLineEventIdActualFromBuildEvent = TimelineEventId.SEND_ANALOG_PROGRESS.buildEventId(EventId
                .builder()
                .iun(IUN)
                .recIndex(1)
                .sentAttemptMade(1)
                .progressIndex(1)
                .build());


        assertThat(timeLineEventIdActualFromBuildEvent).isEqualTo(timeLineEventIdExpected);

    }

    @Test
    void buildSEND_DIGITAL_DOMICILETest() {
        //vecchia versione 123456789_send_digital_domicile_1_source_PLATFORM_attempt_0
        String timeLineEventIdExpected = "send_digital_domicile-IUN_123456789-RECINDEX_1-SOURCE_PLATFORM-SENTATTEMPTMADE_0";
        String timeLineEventIdActual = new TimelineEventIdBuilder()
                .withCategory(TimelineEventId.SEND_DIGITAL_DOMICILE.getValue())
                .withIun(IUN)
                .withRecIndex(1)
                .withSource(DigitalAddressSourceInt.PLATFORM)
                .withSentAttemptMade(0)
                .build();

        assertThat(timeLineEventIdActual).isEqualTo(timeLineEventIdExpected);

        String timeLineEventIdActualFromBuildEvent = TimelineEventId.SEND_DIGITAL_DOMICILE.buildEventId(EventId
                .builder()
                .iun(IUN)
                .recIndex(1)
                .source(DigitalAddressSourceInt.PLATFORM)
                .sentAttemptMade(0)
                .build());


        assertThat(timeLineEventIdActualFromBuildEvent).isEqualTo(timeLineEventIdExpected);

    }

    @Test
    void buildPREPARE_SIMPLE_REGISTERED_LETTERTest() {
        //vecchia versione 123456789_prepare_simple_registered_letter_1
        String timeLineEventIdExpected = "prepare_simple_registered_letter-IUN_123456789-RECINDEX_1";
        String timeLineEventIdActual = new TimelineEventIdBuilder()
                .withCategory(TimelineEventId.PREPARE_SIMPLE_REGISTERED_LETTER.getValue())
                .withIun(IUN)
                .withRecIndex(1)
                .build();

        assertThat(timeLineEventIdActual).isEqualTo(timeLineEventIdExpected);

        String timeLineEventIdActualFromBuildEvent = TimelineEventId.PREPARE_SIMPLE_REGISTERED_LETTER.buildEventId(EventId
                .builder()
                .iun(IUN)
                .recIndex(1)
                .build());


        assertThat(timeLineEventIdActualFromBuildEvent).isEqualTo(timeLineEventIdExpected);

    }

    @Test
    void buildSEND_SIMPLE_REGISTERED_LETTERTest() {
        //vecchia versione 123456789_send_simple_registered_letter_1
        String timeLineEventIdExpected = "send_simple_registered_letter-IUN_123456789-RECINDEX_1";
        String timeLineEventIdActual = new TimelineEventIdBuilder()
                .withCategory(TimelineEventId.SEND_SIMPLE_REGISTERED_LETTER.getValue())
                .withIun(IUN)
                .withRecIndex(1)
                .build();

        assertThat(timeLineEventIdActual).isEqualTo(timeLineEventIdExpected);

        String timeLineEventIdActualFromBuildEvent = TimelineEventId.SEND_SIMPLE_REGISTERED_LETTER.buildEventId(EventId
                .builder()
                .iun(IUN)
                .recIndex(1)
                .build());


        assertThat(timeLineEventIdActualFromBuildEvent).isEqualTo(timeLineEventIdExpected);

    }

    @Test
    void buildPREPARE_ANALOG_DOMICILETest() {
        //vecchia versione 123456789_prepare_analog_domicile_1_attempt_1
        String timeLineEventIdExpected = "prepare_analog_domicile-IUN_123456789-RECINDEX_1-SENTATTEMPTMADE_1";
        String timeLineEventIdActual = new TimelineEventIdBuilder()
                .withCategory(TimelineEventId.PREPARE_ANALOG_DOMICILE.getValue())
                .withIun(IUN)
                .withRecIndex(1)
                .withSentAttemptMade(1)
                .build();

        assertThat(timeLineEventIdActual).isEqualTo(timeLineEventIdExpected);

        String timeLineEventIdActualFromBuildEvent = TimelineEventId.PREPARE_ANALOG_DOMICILE.buildEventId(EventId
                .builder()
                .iun(IUN)
                .recIndex(1)
                .sentAttemptMade(1)
                .build());


        assertThat(timeLineEventIdActualFromBuildEvent).isEqualTo(timeLineEventIdExpected);

    }

    @Test
    void buildSEND_ANALOG_DOMICILETest() {
        //vecchia versione 123456789_send_analog_domicile_1_attempt_1
        String timeLineEventIdExpected = "send_analog_domicile-IUN_123456789-RECINDEX_1-SENTATTEMPTMADE_1";
        String timeLineEventIdActual = new TimelineEventIdBuilder()
                .withCategory(TimelineEventId.SEND_ANALOG_DOMICILE.getValue())
                .withIun(IUN)
                .withRecIndex(1)
                .withSentAttemptMade(1)
                .build();

        assertThat(timeLineEventIdActual).isEqualTo(timeLineEventIdExpected);

        String timeLineEventIdActualFromBuildEvent = TimelineEventId.SEND_ANALOG_DOMICILE.buildEventId(EventId
                .builder()
                .iun(IUN)
                .recIndex(1)
                .sentAttemptMade(1)
                .build());


        assertThat(timeLineEventIdActualFromBuildEvent).isEqualTo(timeLineEventIdExpected);

    }

    @Test
    void buildDIGITAL_DELIVERY_CREATION_REQUESTTest() {
        //vecchia versione digital_delivery_creation_request_iun_123456789_recindex_1
        String timeLineEventIdExpected = "digital_delivery_creation_request-IUN_123456789-RECINDEX_1";
        String timeLineEventIdActual = new TimelineEventIdBuilder()
                .withCategory(TimelineEventId.DIGITAL_DELIVERY_CREATION_REQUEST.getValue())
                .withIun(IUN)
                .withRecIndex(1)
                .build();

        assertThat(timeLineEventIdActual).isEqualTo(timeLineEventIdExpected);

        String timeLineEventIdActualFromBuildEvent = TimelineEventId.DIGITAL_DELIVERY_CREATION_REQUEST.buildEventId(EventId
                .builder()
                .iun(IUN)
                .recIndex(1)
                .build());


        assertThat(timeLineEventIdActualFromBuildEvent).isEqualTo(timeLineEventIdExpected);

    }

    @Test
    void buildDIGITAL_SUCCESS_WORKFLOWTest() {
        //vecchia versione 123456789_digital_success_workflow_1
        String timeLineEventIdExpected = "digital_success_workflow-IUN_123456789-RECINDEX_1";
        String timeLineEventIdActual = new TimelineEventIdBuilder()
                .withCategory(TimelineEventId.DIGITAL_SUCCESS_WORKFLOW.getValue())
                .withIun(IUN)
                .withRecIndex(1)
                .build();

        assertThat(timeLineEventIdActual).isEqualTo(timeLineEventIdExpected);

        String timeLineEventIdActualFromBuildEvent = TimelineEventId.DIGITAL_SUCCESS_WORKFLOW.buildEventId(EventId
                .builder()
                .iun(IUN)
                .recIndex(1)
                .build());


        assertThat(timeLineEventIdActualFromBuildEvent).isEqualTo(timeLineEventIdExpected);

    }

    @Test
    void buildDIGITAL_FAILURE_WORKFLOWTest() {
        //vecchia versione 123456789_digital_failure_workflow_1
        String timeLineEventIdExpected = "digital_failure_workflow-IUN_123456789-RECINDEX_1";
        String timeLineEventIdActual = new TimelineEventIdBuilder()
                .withCategory(TimelineEventId.DIGITAL_FAILURE_WORKFLOW.getValue())
                .withIun(IUN)
                .withRecIndex(1)
                .build();

        assertThat(timeLineEventIdActual).isEqualTo(timeLineEventIdExpected);

        String timeLineEventIdActualFromBuildEvent = TimelineEventId.DIGITAL_FAILURE_WORKFLOW.buildEventId(EventId
                .builder()
                .iun(IUN)
                .recIndex(1)
                .build());


        assertThat(timeLineEventIdActualFromBuildEvent).isEqualTo(timeLineEventIdExpected);

    }

    @Test
    void buildANALOG_SUCCESS_WORKFLOWTest() {
        //vecchia versione 123456789_analog_success_workflow_1
        String timeLineEventIdExpected = "analog_success_workflow-IUN_123456789-RECINDEX_1";
        String timeLineEventIdActual = new TimelineEventIdBuilder()
                .withCategory(TimelineEventId.ANALOG_SUCCESS_WORKFLOW.getValue())
                .withIun(IUN)
                .withRecIndex(1)
                .build();

        assertThat(timeLineEventIdActual).isEqualTo(timeLineEventIdExpected);

        String timeLineEventIdActualFromBuildEvent = TimelineEventId.ANALOG_SUCCESS_WORKFLOW.buildEventId(EventId
                .builder()
                .iun(IUN)
                .recIndex(1)
                .build());


        assertThat(timeLineEventIdActualFromBuildEvent).isEqualTo(timeLineEventIdExpected);

    }

    @Test
    void buildANALOG_FAILURE_WORKFLOWTest() {
        //vecchia versione 123456789_analog_failure_workflow_1
        String timeLineEventIdExpected = "analog_failure_workflow-IUN_123456789-RECINDEX_1";
        String timeLineEventIdActual = new TimelineEventIdBuilder()
                .withCategory(TimelineEventId.ANALOG_FAILURE_WORKFLOW.getValue())
                .withIun(IUN)
                .withRecIndex(1)
                .build();

        assertThat(timeLineEventIdActual).isEqualTo(timeLineEventIdExpected);

        String timeLineEventIdActualFromBuildEvent = TimelineEventId.ANALOG_FAILURE_WORKFLOW.buildEventId(EventId
                .builder()
                .iun(IUN)
                .recIndex(1)
                .build());


        assertThat(timeLineEventIdActualFromBuildEvent).isEqualTo(timeLineEventIdExpected);

    }

    @Test
    void buildNOTIFICATION_VIEWED_CREATION_REQUESTTest() {
        //vecchia versione notification_viewed_creation_request_iun_123456789_recIndex_1
        String timeLineEventIdExpected = "notification_viewed_creation_request-IUN_123456789-RECINDEX_1";
        String timeLineEventIdActual = new TimelineEventIdBuilder()
                .withCategory(TimelineEventId.NOTIFICATION_VIEWED_CREATION_REQUEST.getValue())
                .withIun(IUN)
                .withRecIndex(1)
                .build();

        assertThat(timeLineEventIdActual).isEqualTo(timeLineEventIdExpected);

        String timeLineEventIdActualFromBuildEvent = TimelineEventId.NOTIFICATION_VIEWED_CREATION_REQUEST.buildEventId(EventId
                .builder()
                .iun(IUN)
                .recIndex(1)
                .build());


        assertThat(timeLineEventIdActualFromBuildEvent).isEqualTo(timeLineEventIdExpected);

    }

    @Test
    void buildNOTIFICATION_VIEWEDTest() {
        //vecchia versione 123456789_notification_viewed_1
        String timeLineEventIdExpected = "notification_viewed-IUN_123456789-RECINDEX_1";
        String timeLineEventIdActual = new TimelineEventIdBuilder()
                .withCategory(TimelineEventId.NOTIFICATION_VIEWED.getValue())
                .withIun(IUN)
                .withRecIndex(1)
                .build();

        assertThat(timeLineEventIdActual).isEqualTo(timeLineEventIdExpected);

        String timeLineEventIdActualFromBuildEvent = TimelineEventId.NOTIFICATION_VIEWED.buildEventId(EventId
                .builder()
                .iun(IUN)
                .recIndex(1)
                .build());


        assertThat(timeLineEventIdActualFromBuildEvent).isEqualTo(timeLineEventIdExpected);

    }

    @Test
    void buildCOMPLETELY_UNREACHABLETest() {
        //vecchia versione 123456789_completely_unreachable_1
        String timeLineEventIdExpected = "completely_unreachable-IUN_123456789-RECINDEX_1";
        String timeLineEventIdActual = new TimelineEventIdBuilder()
                .withCategory(TimelineEventId.COMPLETELY_UNREACHABLE.getValue())
                .withIun(IUN)
                .withRecIndex(1)
                .build();

        assertThat(timeLineEventIdActual).isEqualTo(timeLineEventIdExpected);

        String timeLineEventIdActualFromBuildEvent = TimelineEventId.COMPLETELY_UNREACHABLE.buildEventId(EventId
                .builder()
                .iun(IUN)
                .recIndex(1)
                .build());


        assertThat(timeLineEventIdActualFromBuildEvent).isEqualTo(timeLineEventIdExpected);

    }

    @Test
    void buildREFINEMENTTest() {
        //vecchia versione 123456789_refinement_1
        String timeLineEventIdExpected = "refinement-IUN_123456789-RECINDEX_1";
        String timeLineEventIdActual = new TimelineEventIdBuilder()
                .withCategory(TimelineEventId.REFINEMENT.getValue())
                .withIun(IUN)
                .withRecIndex(1)
                .build();

        assertThat(timeLineEventIdActual).isEqualTo(timeLineEventIdExpected);

        String timeLineEventIdActualFromBuildEvent = TimelineEventId.REFINEMENT.buildEventId(EventId
                .builder()
                .iun(IUN)
                .recIndex(1)
                .build());


        assertThat(timeLineEventIdActualFromBuildEvent).isEqualTo(timeLineEventIdExpected);

    }

    @Test
    void buildSCHEDULE_DIGITAL_WORKFLOWTest() {
        //vecchia versione 123456789_schedule_digital_workflow_1_source_PLATFORM_retry_1
        String timeLineEventIdExpected = "schedule_digital_workflow-IUN_123456789-RECINDEX_1-SOURCE_PLATFORM-SENTATTEMPTMADE_1";
        String timeLineEventIdActual = new TimelineEventIdBuilder()
                .withCategory(TimelineEventId.SCHEDULE_DIGITAL_WORKFLOW.getValue())
                .withIun(IUN)
                .withRecIndex(1)
                .withSource(DigitalAddressSourceInt.PLATFORM)
                .withSentAttemptMade(1)
                .build();

        assertThat(timeLineEventIdActual).isEqualTo(timeLineEventIdExpected);

        String timeLineEventIdActualFromBuildEvent = TimelineEventId.SCHEDULE_DIGITAL_WORKFLOW.buildEventId(EventId
                .builder()
                .iun(IUN)
                .recIndex(1)
                .source(DigitalAddressSourceInt.PLATFORM)
                .sentAttemptMade(1)
                .build());


        assertThat(timeLineEventIdActualFromBuildEvent).isEqualTo(timeLineEventIdExpected);

    }

    @Test
    void buildSCHEDULE_ANALOG_WORKFLOWTest() {
        //vecchia versione 123456789_schedule_analog_workflow_1_retry_1
        String timeLineEventIdExpected = "schedule_analog_workflow-IUN_123456789-RECINDEX_1-SENTATTEMPTMADE_1";
        String timeLineEventIdActual = new TimelineEventIdBuilder()
                .withCategory(TimelineEventId.SCHEDULE_ANALOG_WORKFLOW.getValue())
                .withIun(IUN)
                .withRecIndex(1)
                .withSentAttemptMade(1)
                .build();

        assertThat(timeLineEventIdActual).isEqualTo(timeLineEventIdExpected);

        String timeLineEventIdActualFromBuildEvent = TimelineEventId.SCHEDULE_ANALOG_WORKFLOW.buildEventId(EventId
                .builder()
                .iun(IUN)
                .recIndex(1)
                .sentAttemptMade(1)
                .build());


        assertThat(timeLineEventIdActualFromBuildEvent).isEqualTo(timeLineEventIdExpected);

    }

    @Test
    void buildSCHEDULE_REFINEMENT_WORKFLOWTest() {
        //vecchia versione 123456789_schedule_refinement_workflow_1
        String timeLineEventIdExpected = "schedule_refinement_workflow-IUN_123456789-RECINDEX_1";
        String timeLineEventIdActual = new TimelineEventIdBuilder()
                .withCategory(TimelineEventId.SCHEDULE_REFINEMENT_WORKFLOW.getValue())
                .withIun(IUN)
                .withRecIndex(1)
                .build();

        assertThat(timeLineEventIdActual).isEqualTo(timeLineEventIdExpected);

        String timeLineEventIdActualFromBuildEvent = TimelineEventId.SCHEDULE_REFINEMENT_WORKFLOW.buildEventId(EventId
                .builder()
                .iun(IUN)
                .recIndex(1)
                .build());


        assertThat(timeLineEventIdActualFromBuildEvent).isEqualTo(timeLineEventIdExpected);

    }

    @Test
    void buildREQUEST_REFUSEDTest() {
        //vecchia versione 123456789_request_refused
        String timeLineEventIdExpected = "request_refused-IUN_123456789";
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
    void buildPUBLIC_REGISTRY_CALLTest() {
        //vecchia versione 123456789_1_DIGITAL_CHOOSE_DELIVERY_1_public_registry_call
        String timeLineEventIdExpected = "public_registry_call-IUN_123456789-RECINDEX_1-DELIVERYMODE_DIGITAL-CONTACTPHASE_CHOOSE_DELIVERY-SENTATTEMPTMADE_1";
        String timeLineEventIdActual = new TimelineEventIdBuilder()
                .withCategory(TimelineEventId.PUBLIC_REGISTRY_CALL.getValue())
                .withIun(IUN)
                .withRecIndex(1)
                .withDeliveryMode(DeliveryModeInt.DIGITAL)
                .withContactPhase(ContactPhaseInt.CHOOSE_DELIVERY)
                .withSentAttemptMade(1)
                .build();

        assertThat(timeLineEventIdActual).isEqualTo(timeLineEventIdExpected);

        String timeLineEventIdActualFromBuildEvent = TimelineEventId.PUBLIC_REGISTRY_CALL.buildEventId(EventId
                .builder()
                .iun(IUN)
                .recIndex(1)
                .deliveryMode(DeliveryModeInt.DIGITAL)
                .contactPhase(ContactPhaseInt.CHOOSE_DELIVERY)
                .sentAttemptMade(1)
                .build());


        assertThat(timeLineEventIdActualFromBuildEvent).isEqualTo(timeLineEventIdExpected);

    }

    @Test
    void buildPUBLIC_REGISTRY_RESPONSETest() {
        //vecchia versione public_registry_response_corr12345
        String timeLineEventIdExpected = "public_registry_response-CORRELATIONID_corr12345";
        String timeLineEventIdActual = new TimelineEventIdBuilder()
                .withCategory(TimelineEventId.PUBLIC_REGISTRY_RESPONSE.getValue())
                .withCorrelationId("corr12345")
                .build();

        assertThat(timeLineEventIdActual).isEqualTo(timeLineEventIdExpected);

        String timeLineEventIdActualFromBuildEvent = TimelineEventId.PUBLIC_REGISTRY_RESPONSE.buildEventId("corr12345");


        assertThat(timeLineEventIdActualFromBuildEvent).isEqualTo(timeLineEventIdExpected);

    }

    @Test
    void buildAAR_CREATION_REQUESTTest() {
        //vecchia versione aar_creation_request_iun_123456789_recIndex_1
        String timeLineEventIdExpected = "aar_creation_request-IUN_123456789-RECINDEX_1";
        String timeLineEventIdActual = new TimelineEventIdBuilder()
                .withCategory(TimelineEventId.AAR_CREATION_REQUEST.getValue())
                .withIun(IUN)
                .withRecIndex(1)
                .build();

        assertThat(timeLineEventIdActual).isEqualTo(timeLineEventIdExpected);

        String timeLineEventIdActualFromBuildEvent = TimelineEventId.AAR_CREATION_REQUEST.buildEventId(EventId
                .builder()
                .iun(IUN)
                .recIndex(1)
                .build());


        assertThat(timeLineEventIdActualFromBuildEvent).isEqualTo(timeLineEventIdExpected);

    }

    @Test
    void buildAAR_GENERATIONTest() {
        //vecchia versione 123456789_aar_gen_1
        String timeLineEventIdExpected = "aar_gen-IUN_123456789-RECINDEX_1";
        String timeLineEventIdActual = new TimelineEventIdBuilder()
                .withCategory(TimelineEventId.AAR_GENERATION.getValue())
                .withIun(IUN)
                .withRecIndex(1)
                .build();

        assertThat(timeLineEventIdActual).isEqualTo(timeLineEventIdExpected);

        String timeLineEventIdActualFromBuildEvent = TimelineEventId.AAR_GENERATION.buildEventId(EventId
                .builder()
                .iun(IUN)
                .recIndex(1)
                .build());


        assertThat(timeLineEventIdActualFromBuildEvent).isEqualTo(timeLineEventIdExpected);

    }

    @Test
    void buildNOT_HANDLEDTest() {
        //vecchia versione 123456789_not_handled_1
        String timeLineEventIdExpected = "not_handled-IUN_123456789-RECINDEX_1";
        String timeLineEventIdActual = new TimelineEventIdBuilder()
                .withCategory(TimelineEventId.NOT_HANDLED.getValue())
                .withIun(IUN)
                .withRecIndex(1)
                .build();

        assertThat(timeLineEventIdActual).isEqualTo(timeLineEventIdExpected);

        String timeLineEventIdActualFromBuildEvent = TimelineEventId.NOT_HANDLED.buildEventId(EventId
                .builder()
                .iun(IUN)
                .recIndex(1)
                .build());


        assertThat(timeLineEventIdActualFromBuildEvent).isEqualTo(timeLineEventIdExpected);

    }

    @Test
    void buildNOTIFICATION_PAIDTest() {
        //vecchia versione 123456789_notification_paid
        String timeLineEventIdExpected = "notification_paid-IUN_123456789";
        String timeLineEventIdActual = new TimelineEventIdBuilder()
                .withCategory(TimelineEventId.NOTIFICATION_PAID.getValue())
                .withIun(IUN)
                .build();

        assertThat(timeLineEventIdActual).isEqualTo(timeLineEventIdExpected);

        String timeLineEventIdActualFromBuildEvent = TimelineEventId.NOTIFICATION_PAID.buildEventId(EventId
                .builder()
                .iun(IUN)
                .build());


        assertThat(timeLineEventIdActualFromBuildEvent).isEqualTo(timeLineEventIdExpected);

    }

}
