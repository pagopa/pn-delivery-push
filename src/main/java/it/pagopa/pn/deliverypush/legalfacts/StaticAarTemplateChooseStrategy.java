package it.pagopa.pn.deliverypush.legalfacts;

import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public class StaticAarTemplateChooseStrategy implements AarTemplateChooseStrategy{
    private final AarTemplateType aarTemplateType;
    
    @Override
    public AarTemplateType choose(PhysicalAddressInt address) {
        log.debug("Choosing Static AAR type for zip={}", address.getZip());
        return aarTemplateType;
    }
}
