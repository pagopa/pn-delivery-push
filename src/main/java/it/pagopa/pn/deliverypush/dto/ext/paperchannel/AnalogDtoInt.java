package it.pagopa.pn.deliverypush.dto.ext.paperchannel;

import it.pagopa.pn.deliverypush.generated.openapi.msclient.paperchannel.model.SendResponse;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class AnalogDtoInt {
    private SendResponse sendResponse;
    private String productType;
    private String prepareRequestId;
    private String relatedRequestId;
    private int sentAttemptMade;
}
