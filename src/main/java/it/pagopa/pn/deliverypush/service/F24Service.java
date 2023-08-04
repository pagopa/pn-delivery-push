package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.deliverypush.exceptions.PnValidationInvalidMetadataException;

import java.util.List;

public interface F24Service {

    /**
     * Genera tutti i PDF per un certo recipient
     * @param requestId la requestId da usare
     * @param iun IUN
     * @param recipientIndex recipient index
     * @param notificationCost eventuale costo della notifica
     *
     */
    void generateAllPDF(String requestId, String iun, Integer recipientIndex, Integer notificationCost);
}
