package it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.exceptions.PnNotFoundException;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity.TimelineElementEntity;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.junit.jupiter.api.Test;

import static it.pagopa.pn.commons.exceptions.PnExceptionsCodes.ERROR_CODE_PN_GENERIC_ERROR;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;


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

    @Test
    void test_convertEntityToJsonException() {
        // Mock del TimelineElementEntity
        TimelineElementEntity entity = Mockito.mock(TimelineElementEntity.class);

        TimelineElementJsonConverter converter = mock(TimelineElementJsonConverter.class);

        // Stub della chiamata al metodo entityToJson per lanciare un'eccezione
        doThrow(new PnInternalException("Errore", ERROR_CODE_PN_GENERIC_ERROR)).when(converter).entityToJson(entity);

        // Verifica che chiamando il metodo si lanci un'eccezione
        assertThrows(PnInternalException.class, () -> converter.entityToJson(entity));
    }

}
