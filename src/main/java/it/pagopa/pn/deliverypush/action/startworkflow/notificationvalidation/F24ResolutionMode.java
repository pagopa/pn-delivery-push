package it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation;


import lombok.Getter;

/**
 * Modalità di risoluzione degli F24:
 * - URL: non risolve direttamente gli url f24, ma ne crea uno di tipo f24set://
 * - RESOLVE_WITH_TIMELINE: risolve gli url f24 da inviare cercandoli nell'evento GENERATED_F24
 * - RESOLVE_WITH_REPLACED_LIST: risolve gli url f24 da inviare utilizzando la lista "replacedF24AttachmentsUrls"
 */
@Getter
public enum F24ResolutionMode {
    URL("URL"),
    RESOLVE_WITH_TIMELINE("RESOLVE_WITH_TIMELINE"),
    RESOLVE_WITH_REPLACED_LIST("RESOLVE_WITH_REPLACED_LIST");

    private final String value;

    F24ResolutionMode(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}