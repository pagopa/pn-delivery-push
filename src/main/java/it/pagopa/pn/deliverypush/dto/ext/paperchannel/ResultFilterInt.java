package it.pagopa.pn.deliverypush.dto.ext.paperchannel;

import it.pagopa.pn.deliverypush.generated.openapi.msclient.paperchannel.model.ResultFilterEnum;
import lombok.*;
import lombok.experimental.SuperBuilder;

@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode
@Getter
@ToString
public class ResultFilterInt {

    private String fileKey;
    private ResultFilterEnum result;
    private String reasonCode;
    private String reasonDescription;
}
