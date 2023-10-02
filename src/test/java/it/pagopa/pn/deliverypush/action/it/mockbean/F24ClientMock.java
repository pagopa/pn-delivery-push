package it.pagopa.pn.deliverypush.action.it.mockbean;

import it.pagopa.pn.deliverypush.generated.openapi.msclient.f24.model.*;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.f24.PnF24Client;
import it.pagopa.pn.deliverypush.middleware.responsehandler.F24ResponseHandler;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.springframework.context.annotation.Lazy;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

import static org.awaitility.Awaitility.await;

@Setter
@Slf4j
public class F24ClientMock implements PnF24Client {
    public static final String F24_VALIDATION_FAIL = "INVALID_F24";
    private F24ResponseHandler f24ResponseHandler;
    private TimelineService timelineService;

    public F24ClientMock(@Lazy F24ResponseHandler f24ResponseHandler, @Lazy TimelineService timelineService) {
        this.f24ResponseHandler = f24ResponseHandler;
        this.timelineService = timelineService;
    }

    @Override
    public Mono<RequestAccepted> validate(String iun) {
        new Thread(() -> {
            await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                    Assertions.assertTrue(timelineService.getTimelineElement(iun, "VALIDATE_F24_REQUEST.IUN_" + iun).isPresent())
            );

            log.info("[TEST] Start handle validate setId={}", iun);

            AsyncF24Event response = new AsyncF24Event();
            response.setClientId("pn-delivery");
            if (iun.contains(F24_VALIDATION_FAIL)) {
                response.setMetadataValidationEnd(buildMetadataValidationEndWithErrors(iun));
            } else {
                response.setMetadataValidationEnd(buildMetadataValidationEndOk(iun));
            }

            f24ResponseHandler.handleResponseReceived(response);

            log.info("[TEST] End handle validate setId={}", iun);
        }).start();


        return Mono.just(new RequestAccepted().description("OK").status("OK"));
    }

    private MetadataValidationEndEvent buildMetadataValidationEndWithErrors(String setId) {
        MetadataValidationEndEvent metadataValidationEndEvent = new MetadataValidationEndEvent();
        metadataValidationEndEvent.setId(setId);
        metadataValidationEndEvent.setStatus("KO");
        metadataValidationEndEvent.setErrors(
                List.of(new ValidationIssue()
                                .code("ERROR_TEST")
                                .detail("ERROR_DETAIL")
                                .element("rec0paym0"),
                        new ValidationIssue()
                                .code("ERROR_TEST")
                                .detail("ERROR_DETAIL_2")
                                .element("rec0paym1")
                )
        );
        return metadataValidationEndEvent;
    }

    private MetadataValidationEndEvent buildMetadataValidationEndOk(String setId) {
        MetadataValidationEndEvent metadataValidationEndEvent = new MetadataValidationEndEvent();
        metadataValidationEndEvent.setId(setId);
        metadataValidationEndEvent.setStatus("KO");
        return metadataValidationEndEvent;
    }

    public void clear() {
    }
}
