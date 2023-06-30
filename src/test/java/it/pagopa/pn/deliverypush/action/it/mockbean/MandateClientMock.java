package it.pagopa.pn.deliverypush.action.it.mockbean;

import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.mandate.PnMandateClient;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.mandate.model.CxTypeAuthFleet;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.mandate.model.InternalMandateDto;

import java.util.List;

public class MandateClientMock implements PnMandateClient {
    @Override
    public List<InternalMandateDto> listMandatesByDelegate(String delegated, String mandateId, CxTypeAuthFleet cxType, List<String> cxGroups) {
        throw new RuntimeException();
    }
}
