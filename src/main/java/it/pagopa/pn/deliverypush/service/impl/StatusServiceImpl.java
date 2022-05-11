package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.generated.openapi.clients.delivery.model.RequestUpdateStatusDto;
import it.pagopa.pn.deliverypush.dto.ext.delivery.RequestUpdateStatusDtoInt;
import it.pagopa.pn.deliverypush.pnclient.delivery.PnDeliveryClient;
import it.pagopa.pn.deliverypush.service.StatusService;
import it.pagopa.pn.deliverypush.service.mapper.RequestUpdateStatusDtoMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StatusServiceImpl implements StatusService {
    private final PnDeliveryClient pnDeliveryClient;

    public StatusServiceImpl(PnDeliveryClient pnDeliveryClient) {
        this.pnDeliveryClient = pnDeliveryClient;
    }

    @Override
    public void updateStatus(RequestUpdateStatusDtoInt dto) {
        RequestUpdateStatusDto updateStatusDto = RequestUpdateStatusDtoMapper.internalToExternal(dto);
        ResponseEntity<Void> resp = pnDeliveryClient.updateStatus(updateStatusDto);
        
        if (resp.getStatusCode().is2xxSuccessful()) {
            log.info("Status changed to {} for iun {}", dto.getNextState(), dto.getIun());
        } else {
            log.error("Status not updated correctly - iun {}", dto.getIun());
            throw new PnInternalException("Status not updated correctly - iun " + dto.getIun());
        }
    }
    
}
