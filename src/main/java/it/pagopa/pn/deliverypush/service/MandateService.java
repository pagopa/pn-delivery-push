package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.deliverypush.dto.ext.mandate.MandateDtoInt;

import java.util.List;

public interface MandateService {
    List<MandateDtoInt> listMandatesByDelegate(String delegated, String mandateId);
}
