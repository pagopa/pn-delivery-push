package it.pagopa.pn.deliverypush.dto.notificationrework;

import lombok.Data;

@Data
public class NotificationUpdateReworkRequestInternal {
    private String iun;
    private String expectedStatusCode;
    private String expectedDeliveryFailureCause;
    private String productType;
}
