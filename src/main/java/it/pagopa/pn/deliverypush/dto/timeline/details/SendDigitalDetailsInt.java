package it.pagopa.pn.deliverypush.dto.timeline.details;

import it.pagopa.pn.deliverypush.dto.address.DigitalAddressSourceInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.utils.AuditLogUtils;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class SendDigitalDetailsInt implements DigitalSendTimelineElementDetails {
    private int recIndex;
    private LegalDigitalAddressInt digitalAddress;
    private DigitalAddressSourceInt digitalAddressSource;
    private Integer retryNumber;
    private DownstreamIdInt downstreamId;

    public String toLog() {
        return String.format(
                "recIndex=%d source=%s retryNumber=%s digitalAddress=%s",
                recIndex,
                digitalAddressSource,
                retryNumber,
                AuditLogUtils.SENSITIVE
        );
    }
}
