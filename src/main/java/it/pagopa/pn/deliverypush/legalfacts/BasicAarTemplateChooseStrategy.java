package it.pagopa.pn.deliverypush.legalfacts;

import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class BasicAarTemplateChooseStrategy implements AarTemplateChooseStrategy{
    private final AarTemplateType aarTemplateType;
    
    @Override
    public AarTemplateType choose(PhysicalAddressInt address) {
        return aarTemplateType;
    }
}
