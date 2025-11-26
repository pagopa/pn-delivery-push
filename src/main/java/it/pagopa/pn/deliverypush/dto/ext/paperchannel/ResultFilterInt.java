package it.pagopa.pn.deliverypush.dto.ext.paperchannel;

import lombok.*;
import lombok.experimental.SuperBuilder;

@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode
@Getter
@Setter
@ToString
public class ResultFilterInt {

    private String fileKey;
    private ResultFilterEnum result;
    private String reasonCode;
    private String reasonDescription;
}
