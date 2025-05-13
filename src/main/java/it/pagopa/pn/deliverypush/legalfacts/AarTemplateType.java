package it.pagopa.pn.deliverypush.legalfacts;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public enum AarTemplateType {
    AAR_NOTIFICATION(false),
    AAR_NOTIFICATION_RADD(true),
    AAR_NOTIFICATION_RADD_ALT(true);

    private final boolean aarRADD;
    
    AarTemplateType (boolean aarRADD){
        this.aarRADD = aarRADD;
    }
}
