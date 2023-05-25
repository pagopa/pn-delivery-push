package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.mandate;

import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.mandate.model.CxTypeAuthFleet;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.mandate.model.InternalMandateDto;

import java.util.List;

public interface PnMandateClient {
    String CLIENT_NAME = PnLogger.EXTERNAL_SERVICES.PN_MANDATE;
    String GET_MANDATES_BY_DELEGATE = "GET MANDATES BY DELEGATE";
    
    List<InternalMandateDto> listMandatesByDelegate(String delegated,
                                                    String mandateId,
                                                    CxTypeAuthFleet cxType,
                                                    List<String> cxGroups);
}
