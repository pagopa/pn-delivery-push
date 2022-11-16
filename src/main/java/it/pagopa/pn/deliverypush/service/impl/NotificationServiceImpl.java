package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.generated.openapi.clients.delivery.model.SentNotification;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.delivery.PnDeliveryClient;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.delivery.PnDeliveryClientReactive;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.mapper.NotificationMapper;
import it.pagopa.pn.deliverypush.service.mapper.NotificationMapperReactive;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_NOTIFICATIONFAILED;

@Service
@Slf4j
public class NotificationServiceImpl implements NotificationService {
    private final PnDeliveryClient pnDeliveryClient;
    private final PnDeliveryClientReactive pnDeliveryClientReactive;

    public NotificationServiceImpl(PnDeliveryClient pnDeliveryClient,
                                   PnDeliveryClientReactive pnDeliveryClientReactive) {
        this.pnDeliveryClient = pnDeliveryClient;
        this.pnDeliveryClientReactive = pnDeliveryClientReactive;
    }

    @Override
    public NotificationInt getNotificationByIun(String iun) {
        ResponseEntity<SentNotification> resp = pnDeliveryClient.getSentNotification(iun);

        if (resp.getStatusCode().is2xxSuccessful()) {
            log.debug("Get notification OK for - iun {}", iun);
            SentNotification sentNotification = resp.getBody();

            if (sentNotification != null) {
                return NotificationMapper.externalToInternal(sentNotification);
            } else {
                log.error("Get notification is not valid for - iun {}", iun);
                throw new PnInternalException("Get notification is not valid for - iun " + iun, ERROR_CODE_DELIVERYPUSH_NOTIFICATIONFAILED);
            }
        } else {
            log.error("Get notification Failed for - iun {}", iun);
            throw new PnInternalException("Get notification Failed for - iun " + iun, ERROR_CODE_DELIVERYPUSH_NOTIFICATIONFAILED);
        }
    }

    @Override
    public Mono<NotificationInt> getNotificationByIunReactive(String iun) {
        return pnDeliveryClientReactive.getSentNotification(iun)
                .onErrorResume( error -> {
                    log.error("Get notification is not valid for - iun {}", iun);
                    throw new PnInternalException("Get notification is not valid for - iun " + iun, ERROR_CODE_DELIVERYPUSH_NOTIFICATIONFAILED);
                })
                .switchIfEmpty(
                    Mono.error(new PnInternalException("Get notification is not valid for - iun " + iun,
                            ERROR_CODE_DELIVERYPUSH_NOTIFICATIONFAILED))
                )
                .map(NotificationMapperReactive::externalToInternal);
    }
}
