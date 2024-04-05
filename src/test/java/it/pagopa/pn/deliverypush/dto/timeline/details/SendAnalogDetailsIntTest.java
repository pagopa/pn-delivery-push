package it.pagopa.pn.deliverypush.dto.timeline.details;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.paperchannel.CategorizedAttachmentsResultInt;
import it.pagopa.pn.deliverypush.dto.ext.paperchannel.ResultFilterInt;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

class SendAnalogDetailsIntTest {
    private SendAnalogDetailsInt detailsInt;
    @BeforeEach
    void setUp() {
        detailsInt = new SendAnalogDetailsInt();
        detailsInt.setPhysicalAddress(PhysicalAddressInt.builder().address("address").build());
        detailsInt.setRecIndex(3);
        detailsInt.setRelatedRequestId("abc");
        detailsInt.setAnalogCost(100);
        detailsInt.setServiceLevel(ServiceLevelInt.REGISTERED_LETTER_890);
        detailsInt.setSentAttemptMade(2);
        detailsInt.setCategorizedAttachmentsResult(CategorizedAttachmentsResultInt.builder().acceptedAttachments(List.of(ResultFilterInt.builder().fileKey("fileKey").build())).build());
    }

    @Test
    void testEquals() {
        SendAnalogDetailsInt expected = buildSendAnalogDetailsInt();
        Assertions.assertEquals(Boolean.TRUE, expected.equals(detailsInt));
    }
    @Test
    void getRecIndex() {
        Assertions.assertEquals(3, detailsInt.getRecIndex());
    }
    @Test
    void getPhysicalAddress() {
        Assertions.assertEquals(PhysicalAddressInt.builder().address("address").build(), detailsInt.getPhysicalAddress());
    }
    @Test
    void getServiceLevel() {
        Assertions.assertEquals(ServiceLevelInt.REGISTERED_LETTER_890, detailsInt.getServiceLevel());
    }
    @Test
    void getSentAttemptMade() {
        Assertions.assertEquals(2, detailsInt.getSentAttemptMade());
    }
    @Test
    void getRelatedRequestId() {
        Assertions.assertEquals("abc", detailsInt.getRelatedRequestId());
    }
    @Test
    void getAnalogCost() {
        Assertions.assertEquals(100, detailsInt.getAnalogCost());
    }
    @Test
    void getCategorizedAttachmentsResult(){
        Assertions.assertEquals(CategorizedAttachmentsResultInt.builder().acceptedAttachments(List.of(ResultFilterInt.builder().fileKey("fileKey").build())).build(), detailsInt.getCategorizedAttachmentsResult());
    }

    private SendAnalogDetailsInt buildSendAnalogDetailsInt() {
        return SendAnalogDetailsInt.builder().serviceLevel(ServiceLevelInt.REGISTERED_LETTER_890).physicalAddress(PhysicalAddressInt.builder().address("address").build()).analogCost(100).relatedRequestId("abc").sentAttemptMade(2).recIndex(3).categorizedAttachmentsResult(CategorizedAttachmentsResultInt.builder().acceptedAttachments(List.of(ResultFilterInt.builder().fileKey("fileKey").build())).build()).build();
    }
}