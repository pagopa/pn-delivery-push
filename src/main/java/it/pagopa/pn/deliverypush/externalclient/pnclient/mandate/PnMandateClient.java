package it.pagopa.pn.deliverypush.externalclient.pnclient.mandate;

import it.pagopa.pn.mandate.generated.openapi.clients.mandate.model.InternalMandateDto;

import java.util.List;

public interface PnMandateClient {
    List<InternalMandateDto> listMandatesByDelegate(String delegated, String mandateId);
}
