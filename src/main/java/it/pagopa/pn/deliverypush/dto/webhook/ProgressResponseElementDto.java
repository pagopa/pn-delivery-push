package it.pagopa.pn.deliverypush.dto.webhook;

import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.ProgressResponseElement;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ProgressResponseElementDto {
    private List<ProgressResponseElement> progressResponseElementList;
    private int retryAfter;
}
