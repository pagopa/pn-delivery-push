package it.pagopa.pn.deliverypush.legalfacts;

import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import org.springframework.stereotype.Component;

@Component
public class RADDExperimentationChooseStrategy implements AarTemplateChooseStrategy{
    @Override
    public AarTemplateType choose(PhysicalAddressInt address) {
        return null;
    }
}
