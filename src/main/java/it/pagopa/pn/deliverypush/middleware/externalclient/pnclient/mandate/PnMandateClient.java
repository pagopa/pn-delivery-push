package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.mandate;

import it.pagopa.pn.mandate.generated.openapi.clients.mandate.model.CxTypeAuthFleet;
import it.pagopa.pn.mandate.generated.openapi.clients.mandate.model.InternalMandateDto;

import java.util.List;

public interface PnMandateClient {
    String CLIENT_NAME = "PN-MANDATE";
    String GET_MANDATES_BY_DELEGATE = "GET MANDATES BY DELEGATE";
    
    List<InternalMandateDto> listMandatesByDelegate(String delegated,
                                                    String mandateId,
                                                    CxTypeAuthFleet cxType,
                                                    List<String> cxGroups);
}
