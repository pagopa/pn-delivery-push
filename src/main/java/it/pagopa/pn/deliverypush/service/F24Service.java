package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.deliverypush.exceptions.PnValidationInvalidMetadataException;

import java.util.List;

public interface F24Service {

    /**
     * Valida i metadati di una notifica
     * @param iun lo IUN della notifica
     * @throws PnValidationInvalidMetadataException nell caso di errori di validazione
     */
    void validate(String iun) throws PnValidationInvalidMetadataException;

    /**
     * Genera tutti i PDF per un certo recipient
     * @param requestId la requestId da usare
     * @param iun IUN
     * @param recipientIndex recipient index
     * @param notificationCost eventuale costo della notifica
     * @return lista di filekey
     */
    List<String>  generateAllPDF(String requestId, String iun, Integer recipientIndex, Integer notificationCost);
}
