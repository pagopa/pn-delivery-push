package it.pagopa.pn.deliverypush.dto.notificationrework;

import it.pagopa.pn.deliverypush.middleware.dao.notificationreworkdao.dynamo.entity.ReworkRequestType;
import lombok.Data;

import java.util.List;

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
    private ReworkRequestType reworkRequestType;
    private String task;
    private List<String> elementsToInvalidate;

}
