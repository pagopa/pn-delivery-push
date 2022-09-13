package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.generated.openapi.clients.delivery.model.NotificationCostResponse;
import it.pagopa.pn.deliverypush.dto.ext.delivery.NotificationCostResponseInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.delivery.PnDeliveryClient;
import it.pagopa.pn.deliverypush.service.NotificationCostService;
import it.pagopa.pn.deliverypush.service.mapper.NotificationCostResponseMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationCostServiceImpl implements NotificationCostService {
    private final PnDeliveryClient pnDeliveryClient;

    public NotificationCostServiceImpl(PnDeliveryClient pnDeliveryClient) {
        this.pnDeliveryClient = pnDeliveryClient;
    }

    @Override
    public Integer getNotificationCost(NotificationInt notificationInt, int recIndex) {
        return 100;
    }
    
    @Override
    public NotificationCostResponseInt getIunFromPaTaxIdAndNoticeCode(String paTaxId, String noticeCode){
        ResponseEntity<NotificationCostResponse> resp = pnDeliveryClient.getNotificationCostPrivate(paTaxId, noticeCode);

        if (resp.getStatusCode().is2xxSuccessful()) {
            log.debug("Get getIunFromPaTaxIdAndNoticeCode OK - paTaxId={} noticeCode={}", paTaxId, noticeCode);

            NotificationCostResponse notificationCostResponse = resp.getBody();

            if(notificationCostResponse != null){
                return NotificationCostResponseMapper.externalToInternal(notificationCostResponse);
            }else {
                log.error("getIunFromPaTaxIdAndNoticeCode is not valid - paTaxId={} noticeCode={}", paTaxId, noticeCode);
                throw new PnInternalException("getIunFromPaTaxIdAndNoticeCode - paTaxId= " + paTaxId + " noticeCode="+noticeCode);
            }
        } else {
            log.error("getIunFromPaTaxIdAndNoticeCode Failed - paTaxId={} noticeCode={}", paTaxId, noticeCode);
            throw new PnInternalException("getIunFromPaTaxIdAndNoticeCode Failed - paTaxId= " + paTaxId + " noticeCode="+noticeCode);
        }
        
    }
}
