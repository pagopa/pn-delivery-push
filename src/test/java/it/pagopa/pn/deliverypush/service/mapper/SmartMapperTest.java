package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.deliverypush.dto.address.DigitalAddressSourceInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.DownstreamIdInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendDigitalDetailsInt;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DigitalAddress;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DigitalAddressSource;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementDetailsV20;
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
}