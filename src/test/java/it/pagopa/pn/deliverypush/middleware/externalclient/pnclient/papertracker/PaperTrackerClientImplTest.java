package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.papertracker;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.papertracker.api.NotificationReworkApi;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.papertracker.model.SequenceItem;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.papertracker.model.SequenceResponse;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.Problem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaperTrackerClientImplTest {

    @Mock
    private NotificationReworkApi notificationReworkApi;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private PaperTrackerClientImpl client;
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

        when(notificationReworkApi.retrieveSequenceAndFinalStatus(statusCode, failureCause))
                .thenReturn(Mono.just(expected));

        // Act & Assert
        StepVerifier.create(client.retrieveSequenceAndFinalStatus(statusCode, failureCause))
                .expectNext(expected)
                .verifyComplete();
    }

    @Test
    void retrieveSequenceAndFinalStatus_mapsWebClientErrorToPnInternalException_withStatus() throws JsonProcessingException {
        // Arrange
        String statusCode = "RECRN002B";

        WebClientResponseException webEx = new WebClientResponseException(
                "Invalid status code",
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                new HttpHeaders(),
                new byte[0],
                null
        );

        Problem problem = Problem.builder()
                .status(400)
                .title("Bad Request")
                .detail("statusCode RECRN002B is PROGRESS")
                .build();

        when(objectMapper.readValue(anyString(), any(Class.class))).thenReturn(problem);
        when(notificationReworkApi.retrieveSequenceAndFinalStatus(statusCode, null))
                .thenReturn(Mono.error(webEx));

        // Act & Assert
        StepVerifier.create(client.retrieveSequenceAndFinalStatus(statusCode, null))
                .expectErrorSatisfies(throwable -> {
                    PnInternalException ex = (PnInternalException) throwable;
                    assertEquals(400, ex.getProblem().getStatus());
                    assertEquals("statusCode RECRN002B is PROGRESS", ex.getProblem().getDetail());
                })
                .verify();
    }

    @Test
    void retrieveSequenceAndFinalStatus_mapsGenericErrorToPnInternalException() {
        // Arrange
        String statusCode = "RECRN001C";
        String originalMsg = "error";

        when(notificationReworkApi.retrieveSequenceAndFinalStatus(statusCode, null))
                .thenReturn(Mono.error(new RuntimeException(originalMsg)));

        // Act & Assert
        StepVerifier.create(client.retrieveSequenceAndFinalStatus(statusCode, null))
                .expectErrorSatisfies(throwable -> assertTrue(Objects.requireNonNull(throwable.getMessage()).contains(originalMsg), "message should contain original error message"))
                .verify();
    }
}
