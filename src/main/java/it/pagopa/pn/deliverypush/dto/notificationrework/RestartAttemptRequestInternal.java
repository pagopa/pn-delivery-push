package it.pagopa.pn.deliverypush.dto.notificationrework;

import it.pagopa.pn.deliverypush.middleware.dao.notificationreworkdao.dynamo.entity.RequestTypeEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class RestartAttemptRequestInternal extends NotificationReworkRequestInternal {

	public RestartAttemptRequestInternal() {
		setRequestType(RequestTypeEnum.RESTART);
	}
}
