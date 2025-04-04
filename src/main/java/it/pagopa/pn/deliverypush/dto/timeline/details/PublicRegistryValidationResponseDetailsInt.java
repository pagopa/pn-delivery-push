package it.pagopa.pn.deliverypush.dto.timeline.details;

import it.pagopa.pn.deliverypush.utils.AuditLogUtils;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class PublicRegistryValidationResponseDetailsInt extends PublicRegistryResponseDetailsInt implements PhysicalAddressRelatedTimelineElement {
    private String registry;
    private Instant addressResolutionStart;
    private Instant addressResolutionEnd;

    @Override
    public String toLog() {
        return String.format(
                "recIndex=%d digitalAddress=%s physicalAddress=%s requestTimelineId=%s registry=%s addressResolutionStart=%s addressResolutionEnd=%s",
                recIndex,
                AuditLogUtils.SENSITIVE,
                AuditLogUtils.SENSITIVE,
                requestTimelineId,
                registry,
                addressResolutionStart,
                addressResolutionEnd
        );
    }

}
