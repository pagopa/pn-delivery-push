package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.deliverypush.dto.ext.delivery.RequestUpdateStatusDtoInt;

public interface StatusService {
    void updateStatus(RequestUpdateStatusDtoInt dto);
}
