package it.pagopa.pn.deliverypush.dto.ext.externalchannel;

import com.fasterxml.jackson.annotation.JsonValue;

public enum EventCodeInt {
    C000("C000"), // COMUNICAZIONE CON SERVER PEC AVVENUTA
    C001("C001"), // StatusPec.ACCETTAZIONE
    C002("C002"), // StatusPec.NON_ACCETTAZIONE
    C003("C003"), // StatusPec.AVVENUTA_CONSEGNA
    C004("C004"), // StatusPec.ERRORE_CONSEGNA
    C005("C005"), // StatusPec.PRESA_IN_CARICO
    C006("C006"), // StatusPec.RILEVAZIONE_VIRUS
    C007("C007"); // StatusPec.PREAVVISO_ERRORE_CONSEGNA
    
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
