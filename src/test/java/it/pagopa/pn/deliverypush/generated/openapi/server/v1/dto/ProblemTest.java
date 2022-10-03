package it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

class ProblemTest {

    private Problem problem;

    @BeforeEach
    void setUp() {
        List<ProblemError> errors = new ArrayList<>();
        errors.add(ProblemError.builder()
                .code("code")
                .build());

        problem = Problem.builder()
                .title("001")
                .type("002")
                .detail("003")
                .status(1)
                .traceId("004")
                .errors(errors)
                .build();
    }

    @Test
    void getType() {
        String value = problem.getType();
        Assertions.assertEquals("002", value);
    }

    @Test
    void getStatus() {
        int value = problem.getStatus();
        Assertions.assertEquals(1, value);
    }

    @Test
    void getTitle() {
        String value = problem.getTitle();
        Assertions.assertEquals("001", value);
    }

    @Test
    void getDetail() {
        String value = problem.getDetail();
        Assertions.assertEquals("003", value);
    }

    @Test
    void getTraceId() {
        String value = problem.getTraceId();
        Assertions.assertEquals("004", value);
    }

    @Test
    void getErrors() {
        List<ProblemError> errors = problem.getErrors();
        Assertions.assertEquals(1, errors.size());
    }


    @Test
    void testEquals() {
        List<ProblemError> errors = new ArrayList<>();
        errors.add(ProblemError.builder()
                .code("code")
                .build());

        Problem tmp = Problem.builder()
                .title("001")
                .type("002")
                .detail("003")
                .status(1)
                .traceId("004")
                .errors(errors)
                .build();
        Boolean isEqual = problem.equals(tmp);
        Assertions.assertEquals(Boolean.TRUE, isEqual);
    }
}