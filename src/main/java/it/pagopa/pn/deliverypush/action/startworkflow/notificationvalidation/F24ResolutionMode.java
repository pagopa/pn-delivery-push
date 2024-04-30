package it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation;


import lombok.Getter;

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