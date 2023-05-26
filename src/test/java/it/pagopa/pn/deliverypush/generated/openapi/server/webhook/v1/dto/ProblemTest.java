package it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

class ProblemTest {

    private Problem problem;

    @BeforeEach
    void setUp() {
        problem = new Problem();
        problem.setDetail("001");
        problem.setStatus(2);
        problem.setTitle("003");
        problem.setType("004");
        problem.setTraceId("005");
        problem.setErrors(Collections.singletonList(ProblemError.builder().detail("001").build()));
    }

    @Test
    void type() {
        Problem expected = Problem.builder()
                .detail("001")
                .status(2)
                .title("003")
                .type("004")
                .traceId("005")
                .errors(Collections.singletonList(ProblemError.builder().detail("001").build()))
                .build();
        Assertions.assertEquals(expected, problem.type("004"));
    }

    @Test
    void getType() {
        Assertions.assertEquals("004", problem.getType());
    }

    @Test
    void status() {
        Problem expected = Problem.builder()
                .detail("001")
                .status(2)
                .title("003")
                .type("004")
                .traceId("005")
                .errors(Collections.singletonList(ProblemError.builder().detail("001").build()))
                .build();
        Assertions.assertEquals(expected, problem.status(2));
    }

    @Test
    void getStatus() {
        Assertions.assertEquals(2, problem.getStatus());
    }

    @Test
    void title() {
        Problem expected = Problem.builder()
                .detail("001")
                .status(2)
                .title("003")
                .type("004")
                .traceId("005")
                .errors(Collections.singletonList(ProblemError.builder().detail("001").build()))
                .build();
        Assertions.assertEquals(expected, problem.title("003"));
    }

    @Test
    void getTitle() {
        Assertions.assertEquals("003", problem.getTitle());
    }

    @Test
    void detail() {
        Problem expected = Problem.builder()
                .detail("001")
                .status(2)
                .title("003")
                .type("004")
                .traceId("005")
                .errors(Collections.singletonList(ProblemError.builder().detail("001").build()))
                .build();
        Assertions.assertEquals(expected, problem.detail("001"));
    }

    @Test
    void getDetail() {
        Assertions.assertEquals("001", problem.getDetail());
    }

    @Test
    void traceId() {
        Problem expected = Problem.builder()
                .detail("001")
                .status(2)
                .title("003")
                .type("004")
                .traceId("005")
                .errors(Collections.singletonList(ProblemError.builder().detail("001").build()))
                .build();
        Assertions.assertEquals(expected, problem.traceId("005"));
    }

    @Test
    void getTraceId() {
        Assertions.assertEquals("005", problem.getTraceId());
    }

    @Test
    void errors() {
        Problem expected = Problem.builder()
                .detail("001")
                .status(2)
                .title("003")
                .type("004")
                .traceId("005")
                .errors(Collections.singletonList(ProblemError.builder().detail("001").build()))
                .build();
        Assertions.assertEquals(expected, problem.errors(Collections.singletonList(ProblemError.builder().detail("001").build())));
    }

    @Test
    void getErrors() {
        Assertions.assertEquals(Collections.singletonList(ProblemError.builder().detail("001").build()), problem.getErrors());
    }

    @Test
    void testEquals() {
        Problem expected = Problem.builder()
                .detail("001")
                .status(2)
                .title("003")
                .type("004")
                .traceId("005")
                .errors(Collections.singletonList(ProblemError.builder().detail("001").build()))
                .build();
        Assertions.assertEquals(Boolean.TRUE, expected.equals(problem));
    }
}