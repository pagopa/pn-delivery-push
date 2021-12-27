package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.api.dto.notification.address.AttemptAddressInfo;

public interface DigitalService {
    AttemptAddressInfo getNextAddressInfo(String iun, String taxId);
}
