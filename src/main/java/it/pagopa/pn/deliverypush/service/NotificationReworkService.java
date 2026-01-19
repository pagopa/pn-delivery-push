package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.deliverypush.dto.notificationrework.NotificationReworkRequestInternal;
import it.pagopa.pn.deliverypush.dto.notificationrework.NotificationUpdateReworkRequestInternal;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.ReworkItemsResponse;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.ReworkResponse;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.UpdateReworkRequest;
import reactor.core.publisher.Mono;

public interface NotificationReworkService {

    Mono<ReworkResponse> createNotificationReworkRequest(NotificationReworkRequestInternal notificationReworkRequestDto);

    Mono<ReworkItemsResponse> retrieveNotificationRework(String iun, String reworkId);

    Mono<Void> updateNotificationRework(String iun, NotificationUpdateReworkRequestInternal updateReworkRequest, String reworkId);
}
