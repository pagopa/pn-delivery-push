package it.pagopa.pn.deliverypush.dto.ext.externalchannel;

import com.fasterxml.jackson.annotation.JsonValue;

public enum EventCodeInt {

    // codici interni Delivery-Push (DP)
    DP00("DP00"), // Tentativo reinvio richiesto: codice interno a delivery push che indica una richiesta di ritentativo

    // codici in arrivo da ext-Channel (C) con/senza busta indica se lo stato contiene allegati
    C000("C000"), // COMUNICAZIONE CON SERVER PEC AVVENUTA  (senza busta)
    C001("C001"), // StatusPec.ACCETTAZIONE  (con busta)
    C002("C002"), // StatusPec.NON_ACCETTAZIONE  (con busta)
    C003("C003"), // StatusPec.AVVENUTA_CONSEGNA  (con busta)
    C004("C004"), // StatusPec.ERRORE_CONSEGNA (con busta)
    C005("C005"), // StatusPec.PRESA_IN_CARICO  (senza busta)
    C006("C006"), // StatusPec.RILEVAZIONE_VIRUS (con busta)
    C007("C007"), // StatusPec.PREAVVISO_ERRORE_CONSEGNA  (senza busta)
    C008("C008"), // StatusPec.ERRORE_COMUNICAZIONE_SERVER_PEC  - con retry da parte di PN (senza busta)
    C009("C009"), // StatusPec.ERRORE_DOMINIO_PEC_NON_VALIDO - senza retry:  indica un dominio pec non valido; (senza busta)
    C010("C010"); // StatusPec.ERROR_INVIO_PEC - con retry da parte di PN: indica un errore generico di invio pec (senza busta)

    private final String value;

    EventCodeInt(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

}
