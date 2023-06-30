package it.pagopa.pn.deliverypush.dto.ext.externalchannel;

import lombok.*;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class AttachmentDetailsInt {
    private String id;
    private String documentType;
    private String url;
    private Instant date;
}
