package it.pagopa.pn.deliverypush.dto.legalfacts;

import it.pagopa.pn.deliverypush.legalfacts.AarTemplateType;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class PdfInfo {
    private String key;
    private int numberOfPages;
    private AarTemplateType aarTemplateType;
}
