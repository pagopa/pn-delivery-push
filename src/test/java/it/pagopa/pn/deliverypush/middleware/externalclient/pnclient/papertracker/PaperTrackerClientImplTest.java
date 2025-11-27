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
        String statusCode = "RECRN002B";

        WebClientResponseException webEx = new WebClientResponseException(
                "Invalid status code",
                HttpStatus.BAD_REQUEST.value(),
                "statusCode RECRN002B is PROGRESS",
                new HttpHeaders(),
                new byte[0],
                null
        );

        when(notificationReworkApi.retrieveSequenceAndFinalStatus(statusCode,  "AR",null))
                .thenReturn(Mono.error(webEx));

        // Act & Assert
        StepVerifier.create(client.retrieveSequenceAndFinalStatus(statusCode, null, "AR"))
                .expectErrorSatisfies(throwable -> {
                    WebClientResponseException ex = (WebClientResponseException) throwable;
                    assertEquals(400, ex.getStatusCode().value());
                    assertEquals("statusCode RECRN002B is PROGRESS", ex.getStatusText());
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
