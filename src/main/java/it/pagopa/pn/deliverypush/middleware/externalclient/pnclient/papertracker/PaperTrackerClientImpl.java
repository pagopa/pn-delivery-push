package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.papertracker;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.papertracker.api.NotificationReworkApi;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.papertracker.model.SequenceResponse;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.Problem;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;


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
                .onErrorResume(WebClientResponseException.class, ex -> {
                    try {
                        Problem problem = objectMapper.readValue(ex.getResponseBodyAsString(), Problem.class);
                        log.error("WebClientResponseException during retrieve sequence and finalStatus from paper-tracker for statusCode={} and deliveryFailureCause={}, problem={}",
                                statusCode, deliveryFailureCause, problem);
                        return Mono.error(new PnInternalException(problem.getDetail(), ex.getRawStatusCode(), "PN_PAPER_TRACKER_BAD_REQUEST"));
                    } catch (JsonProcessingException e) {
                        return Mono.error(new PnInternalException(e.getMessage(), HttpStatus.SC_INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR"));
                    }
                });
    }
}
