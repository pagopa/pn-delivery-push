package it.pagopa.pn.deliverypush.dto.ext.paperchannel;

import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class PaperEventInt {
    private String requestId;
    private String iun;
    private String statusCode;
    private Instant statusDateTime;
    private String statusDetail;
}
