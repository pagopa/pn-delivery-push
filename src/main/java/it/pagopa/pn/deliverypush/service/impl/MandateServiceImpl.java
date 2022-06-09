package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.dto.ext.mandate.MandateDtoInt;
import it.pagopa.pn.deliverypush.externalclient.pnclient.mandate.PnMandateClient;
import it.pagopa.pn.deliverypush.service.MandateService;
import it.pagopa.pn.mandate.generated.openapi.clients.mandate.model.InternalMandateDto;

import java.util.List;

public class MandateServiceImpl implements MandateService {
    private final PnMandateClient mandateClient;

    public MandateServiceImpl(PnMandateClient mandateClient) {
        this.mandateClient = mandateClient;
    }

    @Override
    public List<MandateDtoInt> listMandatesByDelegate(String delegated, String mandateId) {
        List<InternalMandateDto> listMandateDto = mandateClient.listMandatesByDelegate(delegated, mandateId);
        
    }
}
