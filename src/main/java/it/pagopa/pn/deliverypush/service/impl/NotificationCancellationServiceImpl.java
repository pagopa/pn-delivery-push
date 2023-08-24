package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.CxTypeAuthFleet;
import it.pagopa.pn.deliverypush.service.NotificationCancellationService;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.utils.AuthUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@AllArgsConstructor
@Slf4j
public class NotificationCancellationServiceImpl implements NotificationCancellationService {
    private NotificationService notificationService;
    private final AuthUtils authUtils;

    @Override
    public Mono<Void> startCancellationProcess(String iun, String paId, CxTypeAuthFleet cxType) {
        return Mono.fromCallable(() -> notificationService.getNotificationByIun(iun))
                .flatMap(notification -> 
                        authUtils.checkPaId(notification, paId, cxType)
                                .then(Mono.fromRunnable(() -> log.info("Completato inserimento in timeline")))
                );
    }
}
