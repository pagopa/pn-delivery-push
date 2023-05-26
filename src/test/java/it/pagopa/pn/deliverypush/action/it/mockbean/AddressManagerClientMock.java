package it.pagopa.pn.deliverypush.action.it.mockbean;

import it.pagopa.pn.deliverypush.generated.openapi.msclient.addressmanager.model.*;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.addressmanager.AddressManagerClient;
import it.pagopa.pn.deliverypush.middleware.responsehandler.AddressManagerResponseHandler;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.service.mapper.AddressManagerMapper;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.springframework.context.annotation.Lazy;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.awaitility.Awaitility.await;

@Setter
@Slf4j
public class AddressManagerClientMock implements AddressManagerClient {
    public static final String ADDRESS_MANAGER_NOT_VALID_ADDRESS = "ADDRESS_MANAGER_NOT_VALID_ADDRESS";
    public static final String ADDRESS_MANAGER_TO_NORMALIZE = "TO_NORMALIZE";
    public static final String ADDRESS_MANAGER_NORMALIZED = "NORMALIZED";
    
    private AddressManagerResponseHandler addressManagerResponseHandler;
    private TimelineService timelineService;
    private TimelineUtils timelineUtils;
    private ConcurrentMap<String, AnalogAddress> mapNormalizedAddress;
    
    public AddressManagerClientMock(@Lazy AddressManagerResponseHandler addressManagerResponseHandler,
                                    @Lazy TimelineService timelineService,
                                    @Lazy TimelineUtils timelineUtils) {
        this.addressManagerResponseHandler = addressManagerResponseHandler;
        this.timelineService = timelineService;
        this.timelineUtils = timelineUtils;
    }
    
    public void clear() {
        this.mapNormalizedAddress = new ConcurrentHashMap<>();
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
                
                AnalogAddress requestAddress = request.getAddress();
                if (requestAddress != null && 
                        requestAddress.getAddressRow() != null 
                        && requestAddress.getAddressRow().contains(ADDRESS_MANAGER_NOT_VALID_ADDRESS)
                ) {
                    result.setError("Address is not Valid");
                } else {
                    String key = getKey(iun, Integer.valueOf(request.getId()));
                    AnalogAddress normalizedAddress = mapNormalizedAddress.get(key);
                    if(normalizedAddress != null){
                        result.setNormalizedAddress(normalizedAddress);
                    } else {
                        result.setNormalizedAddress(requestAddress);
                    }
                }
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

    public void addNormalizedAddress(String iun, int id, PhysicalAddressInt physicalAddressInt){
        String key = getKey(iun, id);
        AnalogAddress address = AddressManagerMapper.getAnalogAddressFromPhysical(physicalAddressInt);
        mapNormalizedAddress.put(key, address);
    }

    @NotNull
    private static String getKey(String iun, int id) {
        return iun + "_" + id;
    }
    
}