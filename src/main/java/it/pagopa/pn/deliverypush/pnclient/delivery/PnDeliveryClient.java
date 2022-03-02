package it.pagopa.pn.deliverypush.pnclient.delivery;

import it.pagopa.pn.api.dto.status.RequestUpdateStatusDto;
import it.pagopa.pn.api.dto.status.ResponseUpdateStatusDto;
import org.springframework.http.ResponseEntity;

public interface PnDeliveryClient {
    ResponseEntity<ResponseUpdateStatusDto> updateState(RequestUpdateStatusDto dto);
}
