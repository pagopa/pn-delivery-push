package it.pagopa.pn.deliverypush.dto.radd;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class RaddInfo {
    private String type;
    private String transactionId;
}
