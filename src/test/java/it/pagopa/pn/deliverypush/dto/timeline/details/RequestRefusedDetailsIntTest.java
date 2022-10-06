package it.pagopa.pn.deliverypush.dto.timeline.details;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

class RequestRefusedDetailsIntTest {

    private RequestRefusedDetailsInt request;

    @BeforeEach
    public void setup() {
        List<String> errors = new ArrayList<>();
        errors.add("Failed");

        request = RequestRefusedDetailsInt.builder()
                .errors(errors)
                .build();
    }

    @Test
    void toLog() {
        String log = request.toLog();
        Assertions.assertEquals("errors=[Failed]", log);
    }

    @Test
    void getErrors() {
        List<String> actualErrors = request.getErrors();
        Assertions.assertEquals(1, actualErrors.size());
    }

    @Test
    void testToString() {
        String actual = request.toString();
        Assertions.assertEquals("RequestRefusedDetailsInt(errors=[Failed])", actual);
    }
}