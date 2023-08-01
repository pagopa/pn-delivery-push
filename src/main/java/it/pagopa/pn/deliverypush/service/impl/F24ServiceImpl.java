package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.dto.ext.mandate.MandateDtoInt;
import it.pagopa.pn.deliverypush.exceptions.PnValidationInvalidMetadataException;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.mandate.model.InternalMandateDto;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.CxTypeAuthFleet;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.f24.PnF24Client;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.mandate.PnMandateClient;
import it.pagopa.pn.deliverypush.service.F24Service;
import it.pagopa.pn.deliverypush.service.MandateService;
import it.pagopa.pn.deliverypush.service.mapper.MandateDtoMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class F24ServiceImpl implements F24Service {

    private final PnF24Client f24Client;

    public F24ServiceImpl(PnF24Client f24Client) {
        this.f24Client = f24Client;
    }


    private it.pagopa.pn.deliverypush.generated.openapi.msclient.mandate.model.CxTypeAuthFleet convertToMandateCxType(CxTypeAuthFleet cxType) {
        return it.pagopa.pn.deliverypush.generated.openapi.msclient.mandate.model.CxTypeAuthFleet.valueOf(cxType.getValue());
    }

    @Override
    public void validate(String iun) throws PnValidationInvalidMetadataException {
        // FIXME implementare l'invocazione al metodo del client e la generazione della exception
        // nel caso di metadati non validi. Altri tipi di errore invece vanno fatti "salire".
    }

    @Override
    public List<String> generateAllPDF(String requestId, String iun, Integer recipientIndex, Integer notificationCost) {
        return null;
    }
}
