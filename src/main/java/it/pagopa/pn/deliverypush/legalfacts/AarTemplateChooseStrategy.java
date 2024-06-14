package it.pagopa.pn.deliverypush.legalfacts;

import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;

public interface AarTemplateChooseStrategy {
    AarTemplateType choose(PhysicalAddressInt address);
}
