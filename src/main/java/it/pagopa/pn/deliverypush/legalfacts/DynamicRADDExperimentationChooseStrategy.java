package it.pagopa.pn.deliverypush.legalfacts;

import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.utils.CheckRADDExperimentation;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class DynamicRADDExperimentationChooseStrategy implements AarTemplateChooseStrategy{
    private final CheckRADDExperimentation checkRADDExperimentation;
    @Override
    public AarTemplateType choose(PhysicalAddressInt address) {
        boolean isAddressInExperimentation = checkRADDExperimentation.checkAddress(address);
        
        if(isAddressInExperimentation){
            return AarTemplateType.AAR_NOTIFICATION_RADD_ALT;
        }else {
            return AarTemplateType.AAR_NOTIFICATION;
        }
    }
}
