package it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@ToString
public enum AarTemplateTypeEntity {
    AAR_NOTIFICATION("AAR_NOTIFICATION"),
    AAR_NOTIFICATION_RADD("AAR_NOTIFICATION_RADD"),
    AAR_NOTIFICATION_RADD_ALT("AAR_NOTIFICATION_RADD_ALT");

    private final String value;
}
