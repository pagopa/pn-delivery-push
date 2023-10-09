package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.deliverypush.dto.address.DigitalAddressSourceInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.DownstreamIdInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.NotHandledDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.NotificationCancelledDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.PrepareAnalogDomicileFailureDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendDigitalDetailsInt;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DigitalAddress;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DigitalAddressSource;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementDetailsV20;
import java.util.ArrayList;
import java.util.List;
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
}