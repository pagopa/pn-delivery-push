package it.pagopa.pn.deliverypush.dto.timeline.details;


import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class DownstreamIdInt {
    private String systemId;
    private String messageId;
}
