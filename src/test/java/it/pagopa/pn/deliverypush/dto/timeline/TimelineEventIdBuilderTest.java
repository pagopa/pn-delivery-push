package it.pagopa.pn.deliverypush.dto.timeline;

import it.pagopa.pn.deliverypush.dto.address.CourtesyDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TimelineEventIdBuilderTest {

    private static final String IUN = "KWKU-JHXN-HJXM-202304-U-1";


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
        String expectedEventId = "NOTIFICATION_RADD_RETRIEVED.IUN_testIun.RECINDEX_1";

        String actualEventId = TimelineEventId.NOTIFICATION_RADD_RETRIEVED.buildEventId(eventId);

        assertEquals(expectedEventId, actualEventId);
    }

    @Test
    void buildNOTIFICATION_CANCELLED_DOCUMENT_CREATION_REQUESTTest() {
        String timeLineEventIdExpected = String.format("NOTIFICATION_CANCELLED_DOCUMENT_CREATION_REQUEST.IUN_%s", IUN);
        String timeLineEventIdActual = new TimelineEventIdBuilder()
                .withCategory(TimelineEventId.NOTIFICATION_CANCELLED_DOCUMENT_CREATION_REQUEST.getValue())
                .withIun(IUN)
                .build();

        assertThat(timeLineEventIdActual).isEqualTo(timeLineEventIdExpected);

        String timeLineEventIdActualFromBuildEvent = TimelineEventId.NOTIFICATION_CANCELLED_DOCUMENT_CREATION_REQUEST.buildEventId(EventId
                .builder()
                .iun(IUN)
                .build());

        assertThat(timeLineEventIdActualFromBuildEvent).isEqualTo(timeLineEventIdExpected);
    }
}
