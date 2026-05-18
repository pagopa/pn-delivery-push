package it.pagopa.pn.deliverypush.dto.notificationrework;

import it.pagopa.pn.deliverypush.middleware.dao.notificationreworkdao.dynamo.entity.RequestType;
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
    private String productType;
    private RequestType requestType;
    private String task;

}
