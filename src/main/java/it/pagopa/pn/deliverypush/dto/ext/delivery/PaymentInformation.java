package it.pagopa.pn.deliverypush.dto.ext.delivery;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class PaymentInformation {
    private String iun;
    private Integer recipientIdx;
}
