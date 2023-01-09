package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.generated.openapi.clients.delivery.model.NotificationCostResponse;
import it.pagopa.pn.deliverypush.dto.ext.delivery.NotificationCostResponseInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.delivery.PnDeliveryClient;
import it.pagopa.pn.deliverypush.service.NotificationCostService;
import it.pagopa.pn.deliverypush.service.mapper.NotificationCostResponseMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_TAXIDNOTICECODEFAILED;

@Service
@Slf4j
public class NotificationCostServiceImpl implements NotificationCostService {
    private final PnDeliveryClient pnDeliveryClient;

    public NotificationCostServiceImpl(PnDeliveryClient pnDeliveryClient) {
        this.pnDeliveryClient = pnDeliveryClient;
    }

    @Override
    public Mono<Integer> getNotificationCost(NotificationInt notificationInt, int recIndex) {
        return Mono.just(100);
    }

    @Override
    public NotificationCostResponseInt getIunFromPaTaxIdAndNoticeCode(String paTaxId, String noticeCode) {
        NotificationCostResponse notificationCostResponse = pnDeliveryClient.getNotificationCostPrivate(paTaxId, noticeCode);

        log.debug("Get getIunFromPaTaxIdAndNoticeCode OK - paTaxId={} noticeCode={}", paTaxId, noticeCode);

        if (notificationCostResponse != null) {
            return NotificationCostResponseMapper.externalToInternal(notificationCostResponse);
        } else {
            log.error("getIunFromPaTaxIdAndNoticeCode is not valid - paTaxId={} noticeCode={}", paTaxId, noticeCode);
            throw new PnInternalException("getIunFromPaTaxIdAndNoticeCode - paTaxId= " + paTaxId + " noticeCode=" + noticeCode, ERROR_CODE_DELIVERYPUSH_TAXIDNOTICECODEFAILED);
        }

    }
}
