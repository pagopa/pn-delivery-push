package it.pagopa.pn.deliverypush.utils;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@AllArgsConstructor
@Builder
@Getter
@ToString
public class FontType {
    private String path;
    private String fontFamily;
}
