package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.*;
import org.junit.jupiter.api.Test;

class SmartMapperTest {
    //TODO DA Definire test
    @Test
    void mapToClass() {
        SendDigitalDetails sendDigitalDetails = SendDigitalDetails.builder()
                .recIndex(0)
                .digitalAddressSource(DigitalAddressSource.PLATFORM)
                .digitalAddress(DigitalAddress.builder()
                        .type("PEC")
                        .address("testAddress@gmail.com")
                        .build())
                .retryNumber(0)
                .downstreamId(DownstreamId.builder()
                        .messageId("messageId")
                        .systemId("systemId")
                        .build())
                .build();

        TimelineElementDetails details = SmartMapper.mapToClass(sendDigitalDetails, TimelineElementDetails.class);
        System.out.println("details "+ details);
    }

    @Test
    void mapToClass2() {
        TimelineElementDetails timelineElementDetails = TimelineElementDetails.builder()
                .recIndex(0)
                .digitalAddressSource(DigitalAddressSource.PLATFORM)
                .digitalAddress(DigitalAddress.builder()
                        .type("PEC")
                        .address("testAddress@gmail.com")
                        .build())
                .retryNumber(0)
                .downstreamId(DownstreamId.builder()
                        .messageId("messageId")
                        .systemId("systemId")
                        .build())
                .investigation(true)
                .build();

        SendDigitalDetails details = SmartMapper.mapToClass(timelineElementDetails, SendDigitalDetails.class);
        System.out.println("details "+ details);
    }
}