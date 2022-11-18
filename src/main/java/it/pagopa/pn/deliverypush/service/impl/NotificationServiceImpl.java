package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.generated.openapi.clients.delivery.model.SentNotification;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.exceptions.PnNotFoundException;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.delivery.PnDeliveryClient;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.delivery.PnDeliveryClientReactive;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.mapper.NotificationMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_NOTIFICATIONFAILED;
import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_RECIPIENTS_TOKEN_FAILED;


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
                String error = String.format("Get notification is not valid for - iun %s", iun);
                log.error(error);
                throw new PnInternalException(error, ERROR_CODE_DELIVERYPUSH_NOTIFICATIONFAILED);
            }
        } else {
            log.error("Get notification Failed for - iun {}", iun);
            throw new PnInternalException("Get notification Failed for - iun " + iun, ERROR_CODE_DELIVERYPUSH_NOTIFICATIONFAILED);
        }
    }

    @Override
    public Map<String, String> getRecipientsQuickAccessLinkToken(String iun) {
        ResponseEntity<Map<String, String>> resp = pnDeliveryClient.getQuickAccessLinkTokensPrivate(iun);

        if (resp.getStatusCode().is2xxSuccessful()) {
            log.debug("Get QuickAccessLinkToken OK for - iun {}", iun);
            return resp.getBody();
        } else {
            log.error("Get QuickAccessLinkToken Failed for - iun {}", iun);
            throw new PnInternalException("Get QuickAccessLinkToken Failed for - iun " + iun, ERROR_CODE_DELIVERYPUSH_RECIPIENTS_TOKEN_FAILED);
        }
    }

    @Override
    public Mono<NotificationInt> getNotificationByIunReactive(String iun) {
        return pnDeliveryClientReactive.getSentNotification(iun)
                .onErrorResume( error -> {
                    log.error("Get notification error ={} - iun {}", error,  iun);
                    throw new PnInternalException("Get notification error - iun " + iun, ERROR_CODE_DELIVERYPUSH_NOTIFICATIONFAILED, error);
                })
                .switchIfEmpty(
                    Mono.error(new PnNotFoundException("Not found", "Get notification is not valid for - iun " + iun,
                            ERROR_CODE_DELIVERYPUSH_NOTIFICATIONFAILED))
                )
                .map(NotificationMapper::externalToInternal);
    }
}
