package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.deliverypush.dto.notificationrework.NotificationReworkRequestInternal;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.ReworkResponse;
import reactor.core.publisher.Mono;

public interface NotificationReworkService {

    Mono<ReworkResponse> createNotificationReworkRequest(NotificationReworkRequestInternal notificationReworkRequestDto);
}
