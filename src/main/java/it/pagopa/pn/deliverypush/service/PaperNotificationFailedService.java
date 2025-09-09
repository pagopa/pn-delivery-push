package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.ResponsePaperNotificationFailedDto;
import reactor.core.publisher.Flux;

public interface PaperNotificationFailedService {
    void deleteNotificationFailed(String recipientId, String iun);

    Flux<ResponsePaperNotificationFailedDto> getPaperNotificationByRecipientId(String recipientId, Boolean getAAR);
}
