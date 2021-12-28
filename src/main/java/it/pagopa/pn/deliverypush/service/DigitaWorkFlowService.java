package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.address.AttemptAddressInfo;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressSource2;

public interface DigitaWorkFlowService {
    AttemptAddressInfo getNextAddressInfo(String iun, String taxId);
    
    DigitalAddress getAddressFromSource(DigitalAddressSource2 addressSource, NotificationRecipient recipient, Notification notification);

}
