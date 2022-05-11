package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.generated.openapi.clients.delivery.model.SentNotification;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.pnclient.delivery.PnDeliveryClient;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.mapper.NotificationMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationServiceImpl implements NotificationService {
    private final PnDeliveryClient pnDeliveryClient;

    public NotificationServiceImpl(PnDeliveryClient pnDeliveryClient) {
        this.pnDeliveryClient = pnDeliveryClient;
    }

    @Override
    public NotificationInt getNotificationByIun(String iun) {
        ResponseEntity<SentNotification> resp = pnDeliveryClient.getSentNotification( iun );
        
        if (resp.getStatusCode().is2xxSuccessful()) {
            log.error("Get notification OK for - iun {}", iun);
            SentNotification sentNotification = resp.getBody();
            
            if(sentNotification != null){
                return NotificationMapper.externalToInternal(sentNotification);
            }else {
                log.error("Get notification is not valid for - iun {}", iun);
                throw new PnInternalException("Get notification is not valid for - iun " + iun);
            }
        } else {
            log.error("Get notification Failed for - iun {}", iun);
            throw new PnInternalException("Get notification Failed for - iun " + iun);
        }
    }

}
