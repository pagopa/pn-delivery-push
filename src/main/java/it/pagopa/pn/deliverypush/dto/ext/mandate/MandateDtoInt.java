package it.pagopa.pn.deliverypush.dto.ext.mandate;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MandateDtoInt {
    private String mandateId;
    private String delegator;
    private String delegate;
    private List<String> visibilityIds = null;
    private String dateFrom;
    private String dateTo;
}
