package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.generated.openapi.clients.delivery.model.NotificationCostResponse;
import it.pagopa.pn.deliverypush.dto.ext.delivery.PaymentInformation;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.delivery.PnDeliveryClient;
import it.pagopa.pn.deliverypush.service.PaymentInformationService;
import it.pagopa.pn.deliverypush.service.mapper.NotificationCostResponseMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_TAXIDNOTICECODEFAILED;

@Service
@Slf4j
public class PaymentInformationServiceImpl implements PaymentInformationService {
    private final PnDeliveryClient pnDeliveryClient;

    public PaymentInformationServiceImpl(PnDeliveryClient pnDeliveryClient) {
        this.pnDeliveryClient = pnDeliveryClient;
    }

    @Override
    public PaymentInformation getIunFromPaTaxIdAndNoticeCode(String paTaxId, String noticeCode) {
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
