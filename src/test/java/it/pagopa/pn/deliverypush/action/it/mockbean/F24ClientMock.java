package it.pagopa.pn.deliverypush.action.it.mockbean;

import it.pagopa.pn.api.dto.events.*;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
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
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

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

            PnF24MetadataValidationEndEvent.Detail.DetailBuilder builder = PnF24MetadataValidationEndEvent.Detail.builder();

            builder.clientId("pn-delivery");
            if (iun.contains(F24_VALIDATION_FAIL)) {
                builder.metadataValidationEnd(buildMetadataValidationEndWithErrors(iun));
            } else {
                builder.metadataValidationEnd(buildMetadataValidationEndOk(iun));
            }

            f24ResponseHandler.handleEventF24(builder.build());

            log.info("[TEST] End handle validate setId={}", iun);
        }).start();


        return Mono.just(new RequestAccepted().description("OK").status("OK"));
    }

    @Override
    public Mono<RequestAccepted> preparePDF(String requestId, String iun, Integer cost) {
        new Thread(() -> {
            AtomicReference<TimelineElementInternal> timelineElementInternal = new AtomicReference<>();

             await().atMost(Duration.ofSeconds(30)).until(() -> {
                 Optional<TimelineElementInternal> timelineElement =
                         timelineService.getTimelineElement(iun, "GENERATE_F24_REQUEST.IUN_" + iun);
                    if(timelineElement.isPresent()){
                        timelineElementInternal.set(timelineElement.get());
                        return true;
                    }
                    return false;
             });

            log.info("[TEST] Start handle preparePdf setId={}", iun);

            PnF24PdfSetReadyEvent.Detail.DetailBuilder builder = PnF24PdfSetReadyEvent.Detail.builder();

            builder.clientId("pn-delivery");
            builder.pdfSetReady(PnF24PdfSetReadyEventPayload.builder()
                            .generatedPdfsUrls(List.of(
                                    PnF24PdfSetReadyEventItem.builder().pathTokens("0_0").uri("uri1").build(),
                                    PnF24PdfSetReadyEventItem.builder().pathTokens("0_0").uri("uri2").build(),
                                    PnF24PdfSetReadyEventItem.builder().pathTokens("1_0").uri("uri1").build(),
                                    PnF24PdfSetReadyEventItem.builder().pathTokens("1_0").uri("uri1").build(),
                                    PnF24PdfSetReadyEventItem.builder().pathTokens("1_0").uri("uri3").build()
                            ))
                            .requestId(timelineElementInternal.get().getElementId())
                            .status("OK")
                    .build());

            f24ResponseHandler.handleEventF24(builder.build());

            log.info("[TEST] End handle preparePDF setId={}", iun);
        }).start();
        return Mono.just(new RequestAccepted().description("OK").status("OK"));
    }

    private PnF24MetadataValidationEndEventPayload buildMetadataValidationEndWithErrors(String setId) {
        return PnF24MetadataValidationEndEventPayload.builder()
                .setId(setId)
                .status("KO")
                .errors(
                        List.of(
                                PnF24MetadataValidationIssue.builder()
                                        .code("ERROR_TEST")
                                        .detail("ERROR_DETAIL")
                                        .element("rec0paym0")
                                        .build(),

                                PnF24MetadataValidationIssue.builder()
                                        .code("ERROR_TEST")
                                        .detail("ERROR_DETAIL_2")
                                        .element("rec0paym1")
                                        .build()))
                .build();
    }

    private PnF24MetadataValidationEndEventPayload buildMetadataValidationEndOk(String setId) {
        return PnF24MetadataValidationEndEventPayload.builder().setId(setId).status("OK").build();
    }

    public void clear() {
    }
}
