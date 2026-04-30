package it.pagopa.pn.deliverypush.dto.notificationrework;

import it.pagopa.pn.deliverypush.middleware.dao.notificationreworkdao.dynamo.entity.RequestTypeEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class NotificationReworkRequestInternal extends GenericRequestInternal {

    private String pcRetry;
    private String expectedStatusCode;
    private String expectedDeliveryFailureCause;
    private String productType;
    private RequestTypeEnum requestType;

}
