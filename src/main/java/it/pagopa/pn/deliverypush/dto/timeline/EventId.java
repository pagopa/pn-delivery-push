package it.pagopa.pn.deliverypush.dto.timeline;

import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DigitalAddressSource;
import lombok.*;


@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class EventId {//NEW
    private String iun;
    private Integer recIndex;
    private DigitalAddressSource source;
    private int index;
}
