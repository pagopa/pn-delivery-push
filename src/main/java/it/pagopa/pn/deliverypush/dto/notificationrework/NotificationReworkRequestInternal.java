package it.pagopa.pn.deliverypush.dto.notificationrework;

import lombok.Data;

@Data
public class NotificationReworkRequestInternal {

    private String iun;
    private String attemptId;
    private String pcRetry;
    private String recIndex;
    private String reason;
    private String expectedStatusCode;
    private String expectedDeliveryFailureCause;

}
