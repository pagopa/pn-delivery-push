package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.delivery.model.SentNotification;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.exceptions.PnNotFoundException;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.delivery.PnDeliveryClient;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.delivery.PnDeliveryClientReactive;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.mapper.NotificationMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Map;

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
        SentNotification sentNotification = pnDeliveryClient.getSentNotification(iun);
        log.debug("Get notification OK for - iun {}", iun);

        if (sentNotification != null) {
            //return NotificationMapper.externalToInternal(sentNotification);
            
            return NotificationInt.builder()
                    .iun("IUN_01")
                    .paProtocolNumber("protocol_01")
                    .sender(NotificationSenderInt.builder()
                            .paId("Milano1")
                            .build()
                    )
                    .recipients(Collections.singletonList(
                            NotificationRecipientInt.builder()
                                    .taxId("testIdRecipient")
                                    .denomination("Nome Cognome/Ragione Sociale")
                                    .physicalAddress(
                                            PhysicalAddressInt.builder()
                                                    .address("test address")
                                                    .build()
                                    )
                                    .payment(null)
                                    .build()
                    ))
                    .build();

        } else {
            log.error("Get notification is not valid for - iun {}", iun);
            throw new PnInternalException("Get notification is not valid for - iun " + iun, ERROR_CODE_DELIVERYPUSH_NOTIFICATIONFAILED);
        }        
    }

    @Override
    public Map<String, String> getRecipientsQuickAccessLinkToken(String iun) {
       Map<String, String> resp = pnDeliveryClient.getQuickAccessLinkTokensPrivate(iun);
       log.debug("Get QuickAccessLinkToken OK for - iun {}", iun);
       return resp;         
    }

    @Override
    public Mono<NotificationInt> getNotificationByIunReactive(String iun) {
        return pnDeliveryClientReactive.getSentNotification(iun)
                .onErrorResume( error -> {
                    log.error("Get notification error ={} - iun {}", error,  iun);
                    return Mono.error(new PnInternalException("Get notification error - iun " + iun, ERROR_CODE_DELIVERYPUSH_NOTIFICATIONFAILED, error));
                })
                .switchIfEmpty(
                    Mono.error(new PnNotFoundException("Not found", "Get notification is not valid for - iun " + iun,
                            ERROR_CODE_DELIVERYPUSH_NOTIFICATIONFAILED))
                )
                .map(NotificationMapper::externalToInternal);
    }
}
