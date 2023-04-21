package it.pagopa.pn.deliverypush.action.it.mockbean;

import it.pagopa.pn.addressmanager.generated.openapi.clients.addressmanager.model.*;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.addressmanager.AddressManagerClient;
import it.pagopa.pn.deliverypush.middleware.responsehandler.AddressManagerResponseHandler;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.awaitility.Awaitility.await;

@Component
@Setter
@Slf4j
public class AddressManagerClientMock implements AddressManagerClient {
    private final String TO_ERROR = "TO_ERROR";
    private final String TO_NORMALIZE = "TO_NORMALIZE";
    private final String NORMALIZED = "NORMALIZED";
    private AddressManagerResponseHandler addressManagerResponseHandler;
    private TimelineService timelineService;
    private TimelineUtils timelineUtils;

    public AddressManagerClientMock(@Lazy AddressManagerResponseHandler addressManagerResponseHandler,
                                    @Lazy TimelineService timelineService,
                                    @Lazy TimelineUtils timelineUtils) {
        this.addressManagerResponseHandler = addressManagerResponseHandler;
        this.timelineService = timelineService;
        this.timelineUtils = timelineUtils;
    }

    @Override
    public Mono<AcceptedResponse> normalizeAddresses(NormalizeItemsRequest normalizeItemsRequest) {
        new Thread( () ->{

            String iun = timelineUtils.getIunFromTimelineId(normalizeItemsRequest.getCorrelationId());
            await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                    Assertions.assertTrue(timelineService.getTimelineElement(iun, normalizeItemsRequest.getCorrelationId()).isPresent())
            );
            log.info("[TEST] Start handle normalizeAddress corrId={}", normalizeItemsRequest.getCorrelationId());

            List<NormalizeResult> resultItems = new ArrayList<>();

            for(NormalizeRequest request : normalizeItemsRequest.getRequestItems()){
                NormalizeResult result = new NormalizeResult();
                result.setId(request.getId());

                AnalogAddress address = request.getAddress();
                if (address != null) {
                    if(address.getAddressRow().contains(TO_NORMALIZE)){
                        address.setAddressRow(address.getAddressRow() + "_" + NORMALIZED);
                    } else {
                        if(address.getAddressRow().contains(TO_ERROR)){
                            result.setError("Address is not Valid");
                        }
                    }
                }
                
                result.setNormalizedAddress(address);
                resultItems.add(result);
            }

            NormalizeItemsResult response = new NormalizeItemsResult();
            response.setCorrelationId(normalizeItemsRequest.getCorrelationId());
            response.setResultItems(resultItems);

            addressManagerResponseHandler.handleResponseReceived(response);

            log.info("[TEST] END handle normalizeAddress corrId={}", normalizeItemsRequest.getCorrelationId());
        }).start();
        
        return Mono.just(new AcceptedResponse().correlationId(normalizeItemsRequest.getCorrelationId()));
    }
    
}