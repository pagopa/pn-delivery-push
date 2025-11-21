package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.papertracker;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.papertracker.api.NotificationReworkApi;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.papertracker.model.SequenceResponse;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.Problem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaperTrackerClientImpl implements PaperTrackerClient{

    private final NotificationReworkApi notificationReworkApi;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<SequenceResponse> retrieveSequenceAndFinalStatus(String statusCode, String deliveryFailureCause) {
        return notificationReworkApi.retrieveSequenceAndFinalStatus(statusCode, deliveryFailureCause)
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
