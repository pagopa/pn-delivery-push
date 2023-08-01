package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.exceptions.PnValidationInvalidMetadataException;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.f24.model.GenerateF24Request;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.CxTypeAuthFleet;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.f24.PnF24Client;
import it.pagopa.pn.deliverypush.service.F24Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
    public void generateAllPDF(String requestId, String iun, Integer recipientIndex, Integer notificationCost) {
        log.info("generating all F24 PDF iun={} recIndex={} cost={} requestId={}", iun, recipientIndex, notificationCost, requestId);
        GenerateF24Request generateF24Request = new GenerateF24Request();
        generateF24Request.setRequestId(requestId);
        generateF24Request.setIun(iun);
        generateF24Request.setNotificationCost(notificationCost);
        generateF24Request.setRecipientIndex(recipientIndex);
        f24Client.generateAllPDF(requestId, generateF24Request);
    }
}
