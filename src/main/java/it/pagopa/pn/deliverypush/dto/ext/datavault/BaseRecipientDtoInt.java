package it.pagopa.pn.deliverypush.dto.ext.datavault;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class BaseRecipientDtoInt {
    private String internalId;
    private String taxId;
    private RecipientTypeInt recipientType;
    private String denomination;
}
