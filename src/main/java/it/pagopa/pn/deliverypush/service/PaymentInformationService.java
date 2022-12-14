package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.deliverypush.dto.ext.delivery.PaymentInformation;

public interface PaymentInformationService {
    PaymentInformation getIunFromPaTaxIdAndNoticeCode(String paTaxId, String noticeCode);
}
