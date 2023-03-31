package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.deliverypush.dto.ext.mandate.MandateDtoInt;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.CxTypeAuthFleet;

import java.util.List;

public interface MandateService {

    List<MandateDtoInt> listMandatesByDelegate(String delegated, String mandateId, CxTypeAuthFleet cxType, List<String> cxGroups);

}
