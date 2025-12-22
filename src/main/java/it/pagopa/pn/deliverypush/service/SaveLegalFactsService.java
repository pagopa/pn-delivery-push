package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.mandate.DelegateInfoInt;
import reactor.core.publisher.Mono;

import java.time.Instant;

public interface SaveLegalFactsService {
    Mono<String> sendCreationRequestForNotificationViewedLegalFact(
            NotificationInt notification,
            NotificationRecipientInt recipient,
            DelegateInfoInt delegateInfo,
            Instant timeStamp
    );
}
