package it.pagopa.pn.deliverypush.dto.legalFacts;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class PdfInfo {
    private String key;
    private int numberOfPages;
}