package it.pagopa.pn.deliverypush.dto.legalfacts;

import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@SuperBuilder(toBuilder = true)
@Getter
@ToString
public class LegalFactsIdIntWithRecIndex extends LegalFactsIdInt {
    private Integer recIndex;
}
