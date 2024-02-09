package it.pagopa.pn.deliverypush.dto.timeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import it.pagopa.pn.deliverypush.dto.address.CourtesyDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressSourceInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.ContactPhaseInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.DeliveryModeInt;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TimelineEventIdBuilderTest {

    private static final String IUN = "KWKU-JHXN-HJXM-202304-U-1";


    @Test
    void buildSENDERACK_CREATION_REQUESTTest() {
        String timeLineEventIdExpected = "SENDERACK_LEGALFACT_CREATION_REQUEST.IUN_KWKU-JHXN-HJXM-202304-U-1";
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
    void buildVALIDATE_NORMALIZE_ADDRESSTest() {
        String timeLineEventIdExpected = "VALIDATE_NORMALIZE_ADDRESSES_REQUEST.IUN_KWKU-JHXN-HJXM-202304-U-1";
        String timeLineEventIdActual = new TimelineEventIdBuilder()
                .withCategory(TimelineEventId.VALIDATE_NORMALIZE_ADDRESSES_REQUEST.getValue())
                .withIun(IUN)
                .build();

        assertThat(timeLineEventIdActual).isEqualTo(timeLineEventIdExpected);

        String timeLineEventIdActualFromBuildEvent = TimelineEventId.VALIDATE_NORMALIZE_ADDRESSES_REQUEST.buildEventId(EventId
                .builder()
                .iun(IUN)
                .build());

        assertThat(timeLineEventIdActualFromBuildEvent).isEqualTo(timeLineEventIdExpected);
    }

    @Test
    void buildNORMALIZED_ADDRESSTest() {
        String timeLineEventIdExpected = "NORMALIZED_ADDRESS.IUN_KWKU-JHXN-HJXM-202304-U-1.RECINDEX_0";
        String timeLineEventIdActual = new TimelineEventIdBuilder()
                .withCategory(TimelineEventId.NORMALIZED_ADDRESS.getValue())
                .withIun(IUN)
                .withRecIndex(0)
                .build();

        assertThat(timeLineEventIdActual).isEqualTo(timeLineEventIdExpected);

        String timeLineEventIdActualFromBuildEvent = TimelineEventId.NORMALIZED_ADDRESS.buildEventId(EventId
                .builder()
                .iun(IUN)
                .recIndex(0)
                .build());

        assertThat(timeLineEventIdActualFromBuildEvent).isEqualTo(timeLineEventIdExpected);
    }

    @Test
    void buildREQUEST_ACCEPTEDTest() {
        String timeLineEventIdExpected = "REQUEST_ACCEPTED.IUN_KWKU-JHXN-HJXM-202304-U-1";
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
        String timeLineEventIdExpected = "SEND_COURTESY_MESSAGE.IUN_KWKU-JHXN-HJXM-202304-U-1.RECINDEX_0.COURTESYADDRESSTYPE_SMS";
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
        assertThat(elementIdFromIunAndRecIndex).isEqualTo("SEND_COURTESY_MESSAGE.IUN_KWKU-JHXN-HJXM-202304-U-1.RECINDEX_0");

    }

    @Test
    void buildSEND_COURTESY_MESSAGE_OPTINTest() {
        //vecchia versione 123456789_send_courtesy_message_0_type_SMS
        String timeLineEventIdExpected = "SEND_COURTESY_MESSAGE.IUN_KWKU-JHXN-HJXM-202304-U-1.RECINDEX_0.COURTESYADDRESSTYPE_APPIO.OPTIN";
        String timeLineEventIdActual = new TimelineEventIdBuilder()
                .withCategory(TimelineEventId.SEND_COURTESY_MESSAGE.getValue())
                .withIun(IUN)
                .withRecIndex(0)
                .withCourtesyAddressType(COURTESY_DIGITAL_ADDRESS_TYPE_INT.APPIO)
                .withOptin(Boolean.TRUE)
                .build();

        assertThat(timeLineEventIdActual).isEqualTo(timeLineEventIdExpected);

        String timeLineEventIdActualFromBuildEvent = TimelineEventId.SEND_COURTESY_MESSAGE.buildEventId(EventId
                .builder()
                .iun(IUN)
                .recIndex(0)
                .courtesyAddressType(COURTESY_DIGITAL_ADDRESS_TYPE_INT.APPIO)
                .optin(Boolean.TRUE)
                .build());


        assertThat(timeLineEventIdActualFromBuildEvent).isEqualTo(timeLineEventIdExpected);

        String elementIdFromIunAndRecIndex = TimelineEventId.SEND_COURTESY_MESSAGE.buildSearchEventIdByIunAndRecipientIndex(IUN, 0);

        //vecchia versione 123456789_send_courtesy_message_0_type_
        assertThat(elementIdFromIunAndRecIndex).isEqualTo("SEND_COURTESY_MESSAGE.IUN_KWKU-JHXN-HJXM-202304-U-1.RECINDEX_0");

    }

    @Test
    void buildGET_ADDRESSTest() {
        //vecchia versione 123456789_get_address_1_source_PLATFORM_attempt_1
        String timeLineEventIdExpected = "GET_ADDRESS.IUN_KWKU-JHXN-HJXM-202304-U-1.RECINDEX_0.SOURCE_PLATFORM.ATTEMPT_1";
        String timeLineEventIdActual = new TimelineEventIdBuilder()
                .withCategory(TimelineEventId.GET_ADDRESS.getValue())
                .withIun(IUN)
                .withRecIndex(0)
                .withSource(DigitalAddressSourceInt.PLATFORM)
                .withSentAttemptMade(1)
                .build();

        assertThat(timeLineEventIdActual).isEqualTo(timeLineEventIdExpected);

        String timeLineEventIdActualFromBuildEvent = TimelineEventId.GET_ADDRESS.buildEventId(EventId
                .builder()
                .iun(IUN)
                .recIndex(0)
                .source(DigitalAddressSourceInt.PLATFORM)
                .sentAttemptMade(1)
                .build());


        assertThat(timeLineEventIdActualFromBuildEvent).isEqualTo(timeLineEventIdExpected);

    }

    @Test
    void buildSEND_DIGITAL_FEEDBACKTest() {
        //vecchia versione 123456789_send_digital_feedback_1_source_PLATFORM_attempt_1
        String timeLineEventIdExpected = "SEND_DIGITAL_FEEDBACK.IUN_KWKU-JHXN-HJXM-202304-U-1.RECINDEX_0.SOURCE_PLATFORM.ATTEMPT_1";
        String timeLineEventIdActual = new TimelineEventIdBuilder()
                .withCategory(TimelineEventId.SEND_DIGITAL_FEEDBACK.getValue())
                .withIun(IUN)
                .withRecIndex(0)
                .withSource(DigitalAddressSourceInt.PLATFORM)
                .withSentAttemptMade(1)
                .build();

        assertThat(timeLineEventIdActual).isEqualTo(timeLineEventIdExpected);

        String timeLineEventIdActualFromBuildEvent = TimelineEventId.SEND_DIGITAL_FEEDBACK.buildEventId(EventId
                .builder()
                .iun(IUN)
                .recIndex(0)
                .source(DigitalAddressSourceInt.PLATFORM)
                .sentAttemptMade(1)
                .build());


        assertThat(timeLineEventIdActualFromBuildEvent).isEqualTo(timeLineEventIdExpected);

    }

    @Test
    void buildSEND_DIGITAL_PROGRESSTest() {
        //vecchia versione 123456789_digital_delivering_progress_1_source_PLATFORM_attempt_1_progidx_1
        String timeLineEventIdExpected = "DIGITAL_PROG.IUN_KWKU-JHXN-HJXM-202304-U-1.RECINDEX_0.SOURCE_PLATFORM.REPEAT_false.ATTEMPT_0.IDX_1";
        String timeLineEventIdActual = new TimelineEventIdBuilder()
                .withCategory(TimelineEventId.SEND_DIGITAL_PROGRESS.getValue())
                .withIun(IUN)
                .withRecIndex(0)
                .withSource(DigitalAddressSourceInt.PLATFORM)
                .withSentAttemptMade(0)
                .withProgressIndex(1)
                .withIsFirstSendRetry(false)
                .build();

        assertThat(timeLineEventIdActual).isEqualTo(timeLineEventIdExpected);
        Assertions.assertTrue(timeLineEventIdActual.length() < 100); //Non si può andare oltre i 100 per vincolo su externalChannel

        String timeLineEventIdActualFromBuildEvent = TimelineEventId.SEND_DIGITAL_PROGRESS.buildEventId(EventId
                .builder()
                .iun(IUN)
                .recIndex(0)
                .source(DigitalAddressSourceInt.PLATFORM)
                .isFirstSendRetry(false)
                .sentAttemptMade(0)
                .progressIndex(1)
                .build());


        assertThat(timeLineEventIdActualFromBuildEvent).isEqualTo(timeLineEventIdExpected);

        String eventIdByRecAndIndex = TimelineEventId.SEND_DIGITAL_PROGRESS.buildSearchEventIdByIunAndRecipientIndex(IUN, 0);
        //vecchio formato: 123456789_digital_delivering_progress_1_
        assertThat(eventIdByRecAndIndex).isEqualTo("DIGITAL_PROG.IUN_KWKU-JHXN-HJXM-202304-U-1.RECINDEX_0");
    }

    @Test
    void buildSEND_ANALOG_FEEDBACKTest() {
        //vecchia versione 123456789_send_analog_feedback_1_attempt_1
        String timeLineEventIdExpected = "SEND_ANALOG_FEEDBACK.IUN_KWKU-JHXN-HJXM-202304-U-1.RECINDEX_0.ATTEMPT_1";
        String timeLineEventIdActual = new TimelineEventIdBuilder()
                .withCategory(TimelineEventId.SEND_ANALOG_FEEDBACK.getValue())
                .withIun(IUN)
                .withRecIndex(0)
                .withSentAttemptMade(1)
                .build();

        assertThat(timeLineEventIdActual).isEqualTo(timeLineEventIdExpected);

        String timeLineEventIdActualFromBuildEvent = TimelineEventId.SEND_ANALOG_FEEDBACK.buildEventId(EventId
                .builder()
                .iun(IUN)
                .recIndex(0)
                .sentAttemptMade(1)
                .build());


        assertThat(timeLineEventIdActualFromBuildEvent).isEqualTo(timeLineEventIdExpected);

    }

    @Test
    void buildSSEND_ANALOG_PROGRESSTest() {
        //vecchia versione 123456789_send_analog_progress_1_attempt_1_progidx_1
        String timeLineEventIdExpected = "SEND_ANALOG_PROGRESS.IUN_KWKU-JHXN-HJXM-202304-U-1.RECINDEX_0.ATTEMPT_1.IDX_1";
        String timeLineEventIdActual = new TimelineEventIdBuilder()
                .withCategory(TimelineEventId.SEND_ANALOG_PROGRESS.getValue())
                .withIun(IUN)
                .withRecIndex(0)
                .withSentAttemptMade(1)
                .withProgressIndex(1)
                .build();

        assertThat(timeLineEventIdActual).isEqualTo(timeLineEventIdExpected);

        String timeLineEventIdActualFromBuildEvent = TimelineEventId.SEND_ANALOG_PROGRESS.buildEventId(EventId
                .builder()
                .iun(IUN)
                .recIndex(0)
                .sentAttemptMade(1)
                .progressIndex(1)
                .build());


        assertThat(timeLineEventIdActualFromBuildEvent).isEqualTo(timeLineEventIdExpected);

    }

    @Test
    void buildPrepareDigitalDomicile() {
        String timeLineEventIdExpected = "PREPARE_DIGITAL_DOMICILE.IUN_KWKU-JHXN-HJXM-202304-U-1.RECINDEX_0.SOURCE_PLATFORM.ATTEMPT_0.CORRELATIONID_1234";
        final String corrId = "1234";
        String timeLineEventIdActual = new TimelineEventIdBuilder()
                .withCategory(TimelineEventId.PREPARE_DIGITAL_DOMICILE.getValue())
                .withIun(IUN)
                .withRecIndex(0)
                .withSource(DigitalAddressSourceInt.PLATFORM)
                .withSentAttemptMade(0)
                .withCorrelationId(corrId)
                .build();

        assertThat(timeLineEventIdActual).isEqualTo(timeLineEventIdExpected);

        String timeLineEventIdActualFromBuildEvent = TimelineEventId.PREPARE_DIGITAL_DOMICILE.buildEventId(EventId
                .builder()
                .iun(IUN)
                .recIndex(0)
                .source(DigitalAddressSourceInt.PLATFORM)
                .sentAttemptMade(0)
                .isFirstSendRetry(Boolean.FALSE)
                .relatedTimelineId(corrId)
                .build());


        assertThat(timeLineEventIdActualFromBuildEvent).isEqualTo(timeLineEventIdExpected);
    }

    @Test
    void buildSEND_DIGITAL_DOMICILETest() {
        //vecchia versione 123456789_send_digital_domicile_1_source_PLATFORM_attempt_0
        String timeLineEventIdExpected = "SEND_DIGITAL.IUN_KWKU-JHXN-HJXM-202304-U-1.RECINDEX_0.SOURCE_PLATFORM.REPEAT_false.ATTEMPT_0";
        String timeLineEventIdActual = new TimelineEventIdBuilder()
                .withCategory(TimelineEventId.SEND_DIGITAL_DOMICILE.getValue())
                .withIun(IUN)
                .withRecIndex(0)
                .withSource(DigitalAddressSourceInt.PLATFORM)
                .withSentAttemptMade(0)
                .withIsFirstSendRetry(Boolean.FALSE)
                .build();

        assertThat(timeLineEventIdActual).isEqualTo(timeLineEventIdExpected);
        Assertions.assertTrue(timeLineEventIdActual.length() < 100); //Non si può andare oltre i 100 per vincolo su externalChannel

        String timeLineEventIdActualFromBuildEvent = TimelineEventId.SEND_DIGITAL_DOMICILE.buildEventId(EventId
                .builder()
                .iun(IUN)
                .recIndex(0)
                .source(DigitalAddressSourceInt.PLATFORM)
                .sentAttemptMade(0)
                .isFirstSendRetry(Boolean.FALSE)
                .build());


        assertThat(timeLineEventIdActualFromBuildEvent).isEqualTo(timeLineEventIdExpected);

    }

    @Test
    void buildPREPARE_SIMPLE_REGISTERED_LETTERTest() {
        //vecchia versione 123456789_prepare_simple_registered_letter_1
        String timeLineEventIdExpected = "PREPARE_SIMPLE_REGISTERED_LETTER.IUN_KWKU-JHXN-HJXM-202304-U-1.RECINDEX_0";
        String timeLineEventIdActual = new TimelineEventIdBuilder()
                .withCategory(TimelineEventId.PREPARE_SIMPLE_REGISTERED_LETTER.getValue())
                .withIun(IUN)
                .withRecIndex(0)
                .build();

        assertThat(timeLineEventIdActual).isEqualTo(timeLineEventIdExpected);

        String timeLineEventIdActualFromBuildEvent = TimelineEventId.PREPARE_SIMPLE_REGISTERED_LETTER.buildEventId(EventId
                .builder()
                .iun(IUN)
                .recIndex(0)
                .build());


        assertThat(timeLineEventIdActualFromBuildEvent).isEqualTo(timeLineEventIdExpected);

    }

    @Test
    void buildSEND_SIMPLE_REGISTERED_LETTERTest() {
        //vecchia versione 123456789_send_simple_registered_letter_1
        String timeLineEventIdExpected = "SEND_SIMPLE_REGISTERED_LETTER.IUN_KWKU-JHXN-HJXM-202304-U-1.RECINDEX_0";
        String timeLineEventIdActual = new TimelineEventIdBuilder()
                .withCategory(TimelineEventId.SEND_SIMPLE_REGISTERED_LETTER.getValue())
                .withIun(IUN)
                .withRecIndex(0)
                .build();

        assertThat(timeLineEventIdActual).isEqualTo(timeLineEventIdExpected);

        String timeLineEventIdActualFromBuildEvent = TimelineEventId.SEND_SIMPLE_REGISTERED_LETTER.buildEventId(EventId
                .builder()
                .iun(IUN)
                .recIndex(0)
                .build());


        assertThat(timeLineEventIdActualFromBuildEvent).isEqualTo(timeLineEventIdExpected);

    }

    @Test
    void buildSEND_SIMPLE_REGISTERED_LETTER_PROGRESSTest() {
        String timeLineEventIdExpected = "SEND_SIMPLE_REGISTERED_LETTER_PROGRESS.IUN_KWKU-JHXN-HJXM-202304-U-1.RECINDEX_0.IDX_1";
        String timeLineEventIdActual = new TimelineEventIdBuilder()
                .withCategory(TimelineEventId.SEND_SIMPLE_REGISTERED_LETTER_PROGRESS.getValue())
                .withIun(IUN)
                .withRecIndex(0)
                .withProgressIndex(1)
                .build();

        assertThat(timeLineEventIdActual).isEqualTo(timeLineEventIdExpected);

        String timeLineEventIdActualFromBuildEvent = TimelineEventId.SEND_SIMPLE_REGISTERED_LETTER_PROGRESS.buildEventId(EventId
                .builder()
                .iun(IUN)
                .recIndex(0)
                .progressIndex(1)
                .build());

        assertThat(timeLineEventIdActualFromBuildEvent).isEqualTo(timeLineEventIdExpected);

    }

    @Test
    void buildPREPARE_ANALOG_DOMICILETest() {
        //vecchia versione 123456789_prepare_analog_domicile_1_attempt_1
        String timeLineEventIdExpected = "PREPARE_ANALOG_DOMICILE.IUN_KWKU-JHXN-HJXM-202304-U-1.RECINDEX_0.ATTEMPT_1";
        String timeLineEventIdActual = new TimelineEventIdBuilder()
                .withCategory(TimelineEventId.PREPARE_ANALOG_DOMICILE.getValue())
                .withIun(IUN)
                .withRecIndex(0)
                .withSentAttemptMade(1)
                .build();

        assertThat(timeLineEventIdActual).isEqualTo(timeLineEventIdExpected);

        String timeLineEventIdActualFromBuildEvent = TimelineEventId.PREPARE_ANALOG_DOMICILE.buildEventId(EventId
                .builder()
                .iun(IUN)
                .recIndex(0)
                .sentAttemptMade(1)
                .build());


        assertThat(timeLineEventIdActualFromBuildEvent).isEqualTo(timeLineEventIdExpected);

    }

    @Test
    void buildSEND_ANALOG_DOMICILETest() {
        //vecchia versione 123456789_send_analog_domicile_1_attempt_1
        String timeLineEventIdExpected = "SEND_ANALOG_DOMICILE.IUN_KWKU-JHXN-HJXM-202304-U-1.RECINDEX_0.ATTEMPT_1";
        String timeLineEventIdActual = new TimelineEventIdBuilder()
                .withCategory(TimelineEventId.SEND_ANALOG_DOMICILE.getValue())
                .withIun(IUN)
                .withRecIndex(0)
                .withSentAttemptMade(1)
                .build();

        assertThat(timeLineEventIdActual).isEqualTo(timeLineEventIdExpected);

        String timeLineEventIdActualFromBuildEvent = TimelineEventId.SEND_ANALOG_DOMICILE.buildEventId(EventId
                .builder()
                .iun(IUN)
                .recIndex(0)
                .sentAttemptMade(1)
                .build());


        assertThat(timeLineEventIdActualFromBuildEvent).isEqualTo(timeLineEventIdExpected);

    }

    @Test
    void buildDIGITAL_DELIVERY_CREATION_REQUESTTest() {
        //vecchia versione digital_delivery_creation_request_iun_123456789_RECINDEX_0
        String timeLineEventIdExpected = "DIGITAL_DELIVERY_CREATION_REQUEST.IUN_KWKU-JHXN-HJXM-202304-U-1.RECINDEX_0";
        String timeLineEventIdActual = new TimelineEventIdBuilder()
                .withCategory(TimelineEventId.DIGITAL_DELIVERY_CREATION_REQUEST.getValue())
                .withIun(IUN)
                .withRecIndex(0)
                .build();

        assertThat(timeLineEventIdActual).isEqualTo(timeLineEventIdExpected);

        String timeLineEventIdActualFromBuildEvent = TimelineEventId.DIGITAL_DELIVERY_CREATION_REQUEST.buildEventId(EventId
                .builder()
                .iun(IUN)
                .recIndex(0)
                .build());


        assertThat(timeLineEventIdActualFromBuildEvent).isEqualTo(timeLineEventIdExpected);

    }

    @Test
    void buildDIGITAL_SUCCESS_WORKFLOWTest() {
        //vecchia versione 123456789_digital_success_workflow_1
        String timeLineEventIdExpected = "DIGITAL_SUCCESS_WORKFLOW.IUN_KWKU-JHXN-HJXM-202304-U-1.RECINDEX_0";
        String timeLineEventIdActual = new TimelineEventIdBuilder()
                .withCategory(TimelineEventId.DIGITAL_SUCCESS_WORKFLOW.getValue())
                .withIun(IUN)
                .withRecIndex(0)
                .build();

        assertThat(timeLineEventIdActual).isEqualTo(timeLineEventIdExpected);

        String timeLineEventIdActualFromBuildEvent = TimelineEventId.DIGITAL_SUCCESS_WORKFLOW.buildEventId(EventId
                .builder()
                .iun(IUN)
                .recIndex(0)
                .build());


        assertThat(timeLineEventIdActualFromBuildEvent).isEqualTo(timeLineEventIdExpected);

    }

    @Test
    void buildDIGITAL_FAILURE_WORKFLOWTest() {
        //vecchia versione 123456789_digital_failure_workflow_1
        String timeLineEventIdExpected = "DIGITAL_FAILURE_WORKFLOW.IUN_KWKU-JHXN-HJXM-202304-U-1.RECINDEX_0";
        String timeLineEventIdActual = new TimelineEventIdBuilder()
                .withCategory(TimelineEventId.DIGITAL_FAILURE_WORKFLOW.getValue())
                .withIun(IUN)
                .withRecIndex(0)
                .build();

        assertThat(timeLineEventIdActual).isEqualTo(timeLineEventIdExpected);

        String timeLineEventIdActualFromBuildEvent = TimelineEventId.DIGITAL_FAILURE_WORKFLOW.buildEventId(EventId
                .builder()
                .iun(IUN)
                .recIndex(0)
                .build());


        assertThat(timeLineEventIdActualFromBuildEvent).isEqualTo(timeLineEventIdExpected);

    }

    @Test
    void buildANALOG_SUCCESS_WORKFLOWTest() {
        //vecchia versione 123456789_analog_success_workflow_1
        String timeLineEventIdExpected = "ANALOG_SUCCESS_WORKFLOW.IUN_KWKU-JHXN-HJXM-202304-U-1.RECINDEX_0";
        String timeLineEventIdActual = new TimelineEventIdBuilder()
                .withCategory(TimelineEventId.ANALOG_SUCCESS_WORKFLOW.getValue())
                .withIun(IUN)
                .withRecIndex(0)
                .build();

        assertThat(timeLineEventIdActual).isEqualTo(timeLineEventIdExpected);

        String timeLineEventIdActualFromBuildEvent = TimelineEventId.ANALOG_SUCCESS_WORKFLOW.buildEventId(EventId
                .builder()
                .iun(IUN)
                .recIndex(0)
                .build());


        assertThat(timeLineEventIdActualFromBuildEvent).isEqualTo(timeLineEventIdExpected);

    }

    @Test
    void buildANALOG_FAILURE_WORKFLOWTest() {
        //vecchia versione 123456789_analog_failure_workflow_1
        String timeLineEventIdExpected = "ANALOG_FAILURE_WORKFLOW.IUN_KWKU-JHXN-HJXM-202304-U-1.RECINDEX_0";
        String timeLineEventIdActual = new TimelineEventIdBuilder()
                .withCategory(TimelineEventId.ANALOG_FAILURE_WORKFLOW.getValue())
                .withIun(IUN)
                .withRecIndex(0)
                .build();

        assertThat(timeLineEventIdActual).isEqualTo(timeLineEventIdExpected);

        String timeLineEventIdActualFromBuildEvent = TimelineEventId.ANALOG_FAILURE_WORKFLOW.buildEventId(EventId
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
    void buildCOMPLETELY_UNREACHABLETest() {
        //vecchia versione 123456789_completely_unreachable_1
        String timeLineEventIdExpected = "COMPLETELY_UNREACHABLE.IUN_KWKU-JHXN-HJXM-202304-U-1.RECINDEX_0";
        String timeLineEventIdActual = new TimelineEventIdBuilder()
                .withCategory(TimelineEventId.COMPLETELY_UNREACHABLE.getValue())
                .withIun(IUN)
                .withRecIndex(0)
                .build();

        assertThat(timeLineEventIdActual).isEqualTo(timeLineEventIdExpected);

        String timeLineEventIdActualFromBuildEvent = TimelineEventId.COMPLETELY_UNREACHABLE.buildEventId(EventId
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
    void buildSCHEDULE_DIGITAL_WORKFLOWTest() {
        //vecchia versione 123456789_schedule_digital_workflow_1_source_PLATFORM_retry_1
        String timeLineEventIdExpected = "SCHEDULE_DIGITAL_WORKFLOW.IUN_KWKU-JHXN-HJXM-202304-U-1.RECINDEX_0.SOURCE_PLATFORM.ATTEMPT_1";
        String timeLineEventIdActual = new TimelineEventIdBuilder()
                .withCategory(TimelineEventId.SCHEDULE_DIGITAL_WORKFLOW.getValue())
                .withIun(IUN)
                .withRecIndex(0)
                .withSource(DigitalAddressSourceInt.PLATFORM)
                .withSentAttemptMade(1)
                .build();

        assertThat(timeLineEventIdActual).isEqualTo(timeLineEventIdExpected);

        String timeLineEventIdActualFromBuildEvent = TimelineEventId.SCHEDULE_DIGITAL_WORKFLOW.buildEventId(EventId
                .builder()
                .iun(IUN)
                .recIndex(0)
                .source(DigitalAddressSourceInt.PLATFORM)
                .sentAttemptMade(1)
                .build());


        assertThat(timeLineEventIdActualFromBuildEvent).isEqualTo(timeLineEventIdExpected);

    }

    @Test
    void buildSCHEDULE_ANALOG_WORKFLOWTest() {
        //vecchia versione 123456789_schedule_analog_workflow_1_retry_1
        String timeLineEventIdExpected = "SCHEDULE_ANALOG_WORKFLOW.IUN_KWKU-JHXN-HJXM-202304-U-1.RECINDEX_0.ATTEMPT_1";
        String timeLineEventIdActual = new TimelineEventIdBuilder()
                .withCategory(TimelineEventId.SCHEDULE_ANALOG_WORKFLOW.getValue())
                .withIun(IUN)
                .withRecIndex(0)
                .withSentAttemptMade(1)
                .build();

        assertThat(timeLineEventIdActual).isEqualTo(timeLineEventIdExpected);

        String timeLineEventIdActualFromBuildEvent = TimelineEventId.SCHEDULE_ANALOG_WORKFLOW.buildEventId(EventId
                .builder()
                .iun(IUN)
                .recIndex(0)
                .sentAttemptMade(1)
                .build());


        assertThat(timeLineEventIdActualFromBuildEvent).isEqualTo(timeLineEventIdExpected);

    }

    @Test
    void buildSCHEDULE_REFINEMENT_WORKFLOWTest() {
        //vecchia versione 123456789_schedule_refinement_workflow_1
        String timeLineEventIdExpected = "SCHEDULE_REFINEMENT_WORKFLOW.IUN_KWKU-JHXN-HJXM-202304-U-1.RECINDEX_0";
        String timeLineEventIdActual = new TimelineEventIdBuilder()
                .withCategory(TimelineEventId.SCHEDULE_REFINEMENT_WORKFLOW.getValue())
                .withIun(IUN)
                .withRecIndex(0)
                .build();

        assertThat(timeLineEventIdActual).isEqualTo(timeLineEventIdExpected);

        String timeLineEventIdActualFromBuildEvent = TimelineEventId.SCHEDULE_REFINEMENT_WORKFLOW.buildEventId(EventId
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
    void buildNATIONAL_REGISTRY_CALLTest() {
        //vecchia versione 123456789_1_DIGITAL_CHOOSE_DELIVERY_1_public_registry_call
        String timeLineEventIdExpected = "NATIONAL_REGISTRY_CALL.IUN_KWKU-JHXN-HJXM-202304-U-1.RECINDEX_0.DELIVERYMODE_DIGITAL.CONTACTPHASE_CHOOSE_DELIVERY.ATTEMPT_1";
        String timeLineEventIdActual = new TimelineEventIdBuilder()
                .withCategory(TimelineEventId.NATIONAL_REGISTRY_CALL.getValue())
                .withIun(IUN)
                .withRecIndex(0)
                .withDeliveryMode(DeliveryModeInt.DIGITAL)
                .withContactPhase(ContactPhaseInt.CHOOSE_DELIVERY)
                .withSentAttemptMade(1)
                .build();

        assertThat(timeLineEventIdActual).isEqualTo(timeLineEventIdExpected);

        String timeLineEventIdActualFromBuildEvent = TimelineEventId.NATIONAL_REGISTRY_CALL.buildEventId(EventId
                .builder()
                .iun(IUN)
                .recIndex(0)
                .deliveryMode(DeliveryModeInt.DIGITAL)
                .contactPhase(ContactPhaseInt.CHOOSE_DELIVERY)
                .sentAttemptMade(1)
                .build());


        assertThat(timeLineEventIdActualFromBuildEvent).isEqualTo(timeLineEventIdExpected);

    }

    @Test
    void buildNATIONAL_REGISTRY_RESPONSETest() {
        //vecchia versione public_registry_response_corr12345
        String timeLineEventIdExpected = "NATIONAL_REGISTRY_RESPONSE.CORRELATIONID_corr12345";
        String timeLineEventIdActual = new TimelineEventIdBuilder()
                .withCategory(TimelineEventId.NATIONAL_REGISTRY_RESPONSE.getValue())
                .withCorrelationId("corr12345")
                .build();

        assertThat(timeLineEventIdActual).isEqualTo(timeLineEventIdExpected);

        String timeLineEventIdActualFromBuildEvent = TimelineEventId.NATIONAL_REGISTRY_RESPONSE.buildEventId("corr12345");


        assertThat(timeLineEventIdActualFromBuildEvent).isEqualTo(timeLineEventIdExpected);

    }

    @Test
    void buildAAR_CREATION_REQUESTTest() {
        //vecchia versione aar_creation_request_iun_123456789_RECINDEX_0
        String timeLineEventIdExpected = "AAR_CREATION_REQUEST.IUN_KWKU-JHXN-HJXM-202304-U-1.RECINDEX_0";
        String timeLineEventIdActual = new TimelineEventIdBuilder()
                .withCategory(TimelineEventId.AAR_CREATION_REQUEST.getValue())
                .withIun(IUN)
                .withRecIndex(0)
                .build();

        assertThat(timeLineEventIdActual).isEqualTo(timeLineEventIdExpected);

        String timeLineEventIdActualFromBuildEvent = TimelineEventId.AAR_CREATION_REQUEST.buildEventId(EventId
                .builder()
                .iun(IUN)
                .recIndex(0)
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
    void buildNOT_HANDLEDTest() {
        //vecchia versione 123456789_not_handled_1
        String timeLineEventIdExpected = "NOT_HANDLED.IUN_KWKU-JHXN-HJXM-202304-U-1.RECINDEX_0";
        String timeLineEventIdActual = new TimelineEventIdBuilder()
                .withCategory(TimelineEventId.NOT_HANDLED.getValue())
                .withIun(IUN)
                .withRecIndex(0)
                .build();

        assertThat(timeLineEventIdActual).isEqualTo(timeLineEventIdExpected);

        String timeLineEventIdActualFromBuildEvent = TimelineEventId.NOT_HANDLED.buildEventId(EventId
                .builder()
                .iun(IUN)
                .recIndex(0)
                .build());


        assertThat(timeLineEventIdActualFromBuildEvent).isEqualTo(timeLineEventIdExpected);

    }

    @Test
    void buildNOTIFICATION_PAIDForPagoPaPaymentTest() {
        //vecchia versione 123456789_notification_paid
        String timeLineEventIdExpected = "NOTIFICATION_PAID.IUN_KWKU-JHXN-HJXM-202304-U-1.CODE_PPA30200010000001942177777777777";
        String noticeCode = "302000100000019421"; //stringa di 18 caratteri
        String creditorTaxId = "77777777777"; //stringa di 11 caratteri
        String timeLineEventIdActual = new TimelineEventIdBuilder()
                .withCategory(TimelineEventId.NOTIFICATION_PAID.getValue())
                .withIun(IUN)
                .withPaymentCode("PPA" + noticeCode + creditorTaxId)
                .build();

        assertThat(timeLineEventIdActual).isEqualTo(timeLineEventIdExpected);

        String timeLineEventIdActualFromBuildEvent = TimelineEventId.NOTIFICATION_PAID.buildEventId(EventId
                .builder()
                .iun(IUN)
                .noticeCode(noticeCode)
                .creditorTaxId(creditorTaxId)
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
        String expectedEventId = "NOTIFICATION_RADD_RETIREVED.IUN_testIun.RECINDEX_1";

        String actualEventId = TimelineEventId.NOTIFICATION_RADD_RETRIEVED.buildEventId(eventId);

        assertEquals(expectedEventId, actualEventId);
    }
}
