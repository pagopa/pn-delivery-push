package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.NotificationRADDRetrievedDetailsInt;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;

@SpringBootTest
class SmartMapperTest {
    @Autowired
    private SmartMapper smartMapper;


    @Test
    void testTimelineElementInternalMappingTransformer(){
        Instant elementTimestamp = Instant.EPOCH.plusMillis(100);

        Instant eventTimestamp = Instant.EPOCH.plusMillis(10);

        TimelineElementInternal source = TimelineElementInternal.builder()
                .elementId("elementid")
                .iun("iun")
                .timestamp(elementTimestamp)
                .details(NotificationRADDRetrievedDetailsInt.builder()
                        .eventTimestamp(eventTimestamp)
                        .build())
                .build();

        TimelineElementInternal ret = smartMapper.mapToClass(source, TimelineElementInternal.class);

        Assertions.assertNotSame(ret, source);
        Assertions.assertEquals(eventTimestamp, ret.getTimestamp());
    }

}