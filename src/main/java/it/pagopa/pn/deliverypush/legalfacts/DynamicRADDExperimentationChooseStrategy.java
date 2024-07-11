package it.pagopa.pn.deliverypush.legalfacts;

import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.utils.CheckRADDExperimentation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public class DynamicRADDExperimentationChooseStrategy implements AarTemplateChooseStrategy{
    private final CheckRADDExperimentation checkRADDExperimentation;
    @Override
    public AarTemplateType choose(PhysicalAddressInt address) {
        log.debug("Choosing Dynamic AAR type for zip={}", address.getZip());
        boolean isAddressInExperimentation = checkRADDExperimentation.checkAddress(address);
        log.debug("zip={} isAddressInExperimentation={}", address.getZip(), isAddressInExperimentation);

        if(isAddressInExperimentation){
            return AarTemplateType.AAR_NOTIFICATION_RADD_ALT;
        }else {
            return AarTemplateType.AAR_NOTIFICATION;
        }
    }
}
