package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.papertracker;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.pagopa.pn.commons.exceptions.PnRuntimeException;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.papertracker.api.NotificationReworkApi;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.papertracker.model.SequenceItem;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.papertracker.model.SequenceResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaperTrackerClientImplTest {

    @Mock
    private NotificationReworkApi notificationReworkApi;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private PaperTrackerClientImpl client;

    @BeforeEach
    void setup() {
        objectMapper.registerModule(new JavaTimeModule());
        client = new PaperTrackerClientImpl(notificationReworkApi, objectMapper);
    }


    @Test
    void retrieveSequenceAndFinalStatus_success() {
        // Arrange
        String statusCode = "RECRN002C";
        String failureCause = "M06";
        SequenceResponse expected = new SequenceResponse();
        expected.setFinalStatusCode(SequenceResponse.FinalStatusCodeEnum.KO);
        SequenceItem sequenceItem = new SequenceItem();
        sequenceItem.setStatusCode("RECRN001A");
        expected.setSequence(List.of(sequenceItem));

        when(notificationReworkApi.retrieveSequenceAndFinalStatus(statusCode, "AR", failureCause ))
                .thenReturn(Mono.just(expected));

        // Act & Assert
        StepVerifier.create(client.retrieveSequenceAndFinalStatus(statusCode, failureCause, "AR"))
                .expectNext(expected)
                .verifyComplete();
    }

    @Test
    void retrieveSequenceAndFinalStatus_mapsWebClientErrorToPnInternalException_withStatus() {
        // Arrange
        String statusCode = "RECAG003A";
        String jsonBody = """
        {
          "type" : "GENERIC_ERROR",
          "status" : 400,
          "title" : "Handled error",
          "detail" : "See logs for details in pn-paper-tracker",
          "traceId" : "trace_id:87424e79-f153-4a4a-867c-eb437a31c865",
          "timestamp" : "2025-11-28T10:11:59.6388503Z",
          "errors" : [ {
            "code" : "PN_PAPER_TRACKER_INVALID_STATUS_CODE",
            "element" : null,
            "detail" : "statusCode RECAG003A is PROGRESS"
          } ]
        }
        """;

        WebClientResponseException webEx = new WebClientResponseException(
                "Invalid status code",
                HttpStatus.BAD_REQUEST.value(),
                "statusCode RECAG003A is PROGRESS",
                new HttpHeaders(),
                jsonBody.getBytes(),
                null
        );

        when(notificationReworkApi.retrieveSequenceAndFinalStatus(statusCode,  "AR",null))
                .thenReturn(Mono.error(webEx));

        // Act & Assert
        StepVerifier.create(client.retrieveSequenceAndFinalStatus(statusCode, null, "AR"))
                .expectErrorSatisfies(throwable -> {
                    PnRuntimeException ex = (PnRuntimeException) throwable;
                    assertEquals(400, ex.getProblem().getStatus());
                    assertEquals("statusCode RECAG003A is PROGRESS", ex.getProblem().getErrors().get(0).getDetail());
                })
                .verify();
    }

    @Test
    void retrieveSequenceAndFinalStatus_mapsGenericErrorToPnInternalException() {
        // Arrange
        String statusCode = "RECRN001C";
        String originalMsg = "error";

        when(notificationReworkApi.retrieveSequenceAndFinalStatus(statusCode, "AR", null))
                .thenReturn(Mono.error(new RuntimeException(originalMsg)));

        // Act & Assert
        StepVerifier.create(client.retrieveSequenceAndFinalStatus(statusCode,null,"AR"))
                .expectErrorSatisfies(throwable -> assertTrue(Objects.requireNonNull(throwable.getMessage()).contains(originalMsg), "message should contain original error message"))
                .verify();
    }
}
