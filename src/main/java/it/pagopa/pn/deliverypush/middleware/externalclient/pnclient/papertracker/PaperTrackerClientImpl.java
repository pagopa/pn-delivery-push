package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.papertracker;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.common.rest.error.v1.dto.Problem;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.exceptions.PnRuntimeException;
import it.pagopa.pn.commons.exceptions.dto.ProblemError;
import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.papertracker.api.NotificationReworkApi;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.papertracker.model.SequenceResponse;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.List;


@Component
@RequiredArgsConstructor
@CustomLog
public class PaperTrackerClientImpl implements PaperTrackerClient{

    private final NotificationReworkApi notificationReworkApi;
    private final ObjectMapper objectMapper;

    private static final String CLIENT_NAME = PnLogger.EXTERNAL_SERVICES.PN_PAPER_TRACKER;
    private static final String OPERATION_RETRIEVE_SEQUENCE_AND_FINAL_STATUS = "RETRIEVE_SEQUENCE_AND_FINAL_STATUS";

    @Override
    public Mono<SequenceResponse> retrieveSequenceAndFinalStatus(String statusCode, String deliveryFailureCause, String productType) {
        log.logInvokingAsyncExternalService(CLIENT_NAME, OPERATION_RETRIEVE_SEQUENCE_AND_FINAL_STATUS, String.format("%s - %s - %s", statusCode, deliveryFailureCause, productType));
        return notificationReworkApi.retrieveSequenceAndFinalStatus(statusCode, productType, deliveryFailureCause)
                .onErrorResume(err -> {
                    log.error("Exception in retrieveSequenceAndFinalStatus statusCode = {}, deliveryFailureCause = {}",statusCode, deliveryFailureCause, err);
                    if (err instanceof WebClientResponseException webClientException && webClientException.getStatusCode().equals(HttpStatus.BAD_REQUEST)) {
                        return retrieveProblemFromWebClientException(webClientException)
                                .flatMap(problem ->
                                        Mono.error(new PnRuntimeException("Exception invoking retrieveSequenceAndFinalStatus",
                                                PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_ERROR_INVOKING_TRACKER, HttpStatus.BAD_REQUEST.value(), mapToProblemErrors(problem.getErrors()))));
                    }
                    return Mono.error(new PnInternalException("Exception invoking retrieveSequenceAndFinalStatus", PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_ERROR_INVOKING_TRACKER, err));
                });
    }

    private List<ProblemError> mapToProblemErrors(List<it.pagopa.pn.common.rest.error.v1.dto.ProblemError> errors) {
        return errors.stream()
                .map(error -> ProblemError
                        .builder()
                        .code(error.getCode())
                        .detail(error.getDetail())
                        .build())
                .toList();
    }

    private Mono<Problem> retrieveProblemFromWebClientException(WebClientResponseException webClientException) {
        String responseBody = webClientException.getResponseBodyAsString();
        try {
            return Mono.just(objectMapper.readValue(responseBody, Problem.class));
        } catch (Exception e) {
            log.error("Error parsing error response from Paper Tracker: {}", responseBody, e);
            return Mono.error(new PnInternalException("Error parsing error response from Paper Tracker", PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_ERROR_INVOKING_TRACKER));
        }
    }
}