package it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity.TimelineElementEntity;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


class TimelineElementJsonConverterTest {
    private ObjectMapper objectMapper = new ObjectMapper();
    private TimelineElementJsonConverter converter = new TimelineElementJsonConverter(objectMapper);


    @BeforeEach
    void setUp() {
        this.objectMapper = new ObjectMapper();
        this.converter = new TimelineElementJsonConverter(this.objectMapper);
    }

    @Test
    void test_convertEntityToJson() {
        TimelineElementEntity entity = Mockito.mock(TimelineElementEntity.class);

        String expected = """
        {"timelineElementId":null,"iun":null,"statusInfo":null,"notificationSentAt":null,"paId":null,"legalFactIds":[],"details":null,"category":null,"timestamp":null}""";

        String json = converter.entityToJson(entity);
        assertNotNull(json);
        assertTrue(json.contains(expected));
    }

}
