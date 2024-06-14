package it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity;

public enum AarTemplateTypeEntity {
    AAR_NOTIFICATION("AAR_NOTIFICATION"),
    AAR_NOTIFICATION_RADD("AAR_NOTIFICATION_RADD"),
    AAR_NOTIFICATION_RADD_ALT("AAR_NOTIFICATION_RADD_ALT");

    private final String value;

    AarTemplateTypeEntity(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
