package it.pagopa.pn.deliverypush.dto.ext.externalchannel;

import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.PhysicalAddress;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.ResponseStatus;
import lombok.*;

import java.time.Instant;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class ExtChannelResponse {
    private String iun;
    private String eventId;
    private Instant notificationDate;
    private ResponseStatus responseStatus;
    private List<String> errorList;
    private List<String> attachmentKeys;
    private PhysicalAddress analogNewAddressFromInvestigation;
}
