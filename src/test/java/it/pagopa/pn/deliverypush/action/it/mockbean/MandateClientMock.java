package it.pagopa.pn.deliverypush.action.it.mockbean;

import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.mandate.PnMandateClient;
import it.pagopa.pn.mandate.generated.openapi.clients.mandate.model.InternalMandateDto;

import java.util.List;

public class MandateClientMock implements PnMandateClient {
    @Override
    public List<InternalMandateDto> listMandatesByDelegate(String delegated, String mandateId) {
        throw new RuntimeException();
    }
}
