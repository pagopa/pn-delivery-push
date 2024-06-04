package it.pagopa.pn.deliverypush.legalfacts;

import lombok.Getter;

@Getter
public enum AarTemplateType {
    AAR_NOTIFICATION(false, DocumentComposition.TemplateType.AAR_NOTIFICATION),
    AAR_NOTIFICATION_RADD(true, DocumentComposition.TemplateType.AAR_NOTIFICATION_RADD),
    AAR_NOTIFICATION_RADD_ALT(true, DocumentComposition.TemplateType.AAR_NOTIFICATION_RADD_ALT);

    private final boolean aarRADD;
    private final DocumentComposition.TemplateType templateType;
    
    AarTemplateType (boolean aarRADD, DocumentComposition.TemplateType templateType){
        this.aarRADD = aarRADD;
        this.templateType = templateType;
        if(! this.name().equals(templateType.name())){
            String errorMsg = String.format(
                    "Template name %s is not a valid value",
                    this.name()
            );
            throw new IllegalArgumentException(errorMsg);
        }
    }
}