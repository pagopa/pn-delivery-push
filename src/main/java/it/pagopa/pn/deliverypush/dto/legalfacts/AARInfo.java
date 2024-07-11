package it.pagopa.pn.deliverypush.dto.legalfacts;

import it.pagopa.pn.deliverypush.legalfacts.AarTemplateType;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class AARInfo {
    private byte[] bytesArrayGeneratedAar;
    private AarTemplateType templateType;
}
