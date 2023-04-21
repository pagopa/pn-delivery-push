package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.addressmanager.generated.openapi.clients.addressmanager.model.AcceptedResponse;
import it.pagopa.pn.addressmanager.generated.openapi.clients.addressmanager.model.AnalogAddress;
import it.pagopa.pn.addressmanager.generated.openapi.clients.addressmanager.model.NormalizeItemsRequest;
import it.pagopa.pn.addressmanager.generated.openapi.clients.addressmanager.model.NormalizeRequest;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.addressmanager.AddressManagerClient;
import it.pagopa.pn.deliverypush.service.AddressManagerService;
import it.pagopa.pn.deliverypush.service.mapper.AddressManagerMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@AllArgsConstructor
@Service
public class AddressManagerServiceImpl implements AddressManagerService {
    private final AddressManagerClient addressManagerClient;
    private final NotificationUtils notificationUtils;
    
    public Mono<AcceptedResponse> normalizeAddresses(NotificationInt notification, String correlationId){
        log.info("Start get normalizeAddress - iun={}", notification.getIun());
        
        NormalizeItemsRequest normalizeItemsRequest = getRequest(notification, correlationId);
        return addressManagerClient.normalizeAddresses(normalizeItemsRequest);
    }

    @NotNull
    private NormalizeItemsRequest getRequest(NotificationInt notification, String correlationId) {
        log.debug("Start getRequest - iun={} corrId={}", notification.getIun(), correlationId);
        
        List<NormalizeRequest> normalizeRequestList = new ArrayList<>();

        notification.getRecipients().forEach(recipient -> {
            int recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

            if(recipient.getPhysicalAddress() != null){
                
                NormalizeRequest normalizeRequest = new NormalizeRequest();

                AnalogAddress address = AddressManagerMapper.getAnalogAddressFromPhysical(recipient.getPhysicalAddress());

                normalizeRequest.setAddress(address);
                normalizeRequest.setId(Integer.toString(recIndex));

                normalizeRequestList.add(normalizeRequest);

                log.debug("Add normalize request for recIndex={} - iun={} corrId={}", recIndex, notification.getIun(), correlationId);
            } else {
                log.debug("Not add normalize request for recIndex={} - iun={} corrId={}", recIndex, notification.getIun(), correlationId);
            }
            
        });
        
        log.debug("Normalize itemRequest created with size={} - iun={} corrId={}", normalizeRequestList != null ? normalizeRequestList.size() : null, notification.getIun(), correlationId);

        NormalizeItemsRequest normalizeItemsRequest = new NormalizeItemsRequest();
        normalizeItemsRequest.setRequestItems(normalizeRequestList);
        normalizeItemsRequest.setCorrelationId(correlationId);

        return normalizeItemsRequest;
    }
}
