package it.pagopa.pn.deliverypush.dto.nationalregistries;

import lombok.*;

@Getter
@Setter
@Builder
@ToString
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class CheckTaxIdOKInt {
    private String taxId;
    private Boolean isValid;
    private String errorCode;
}
