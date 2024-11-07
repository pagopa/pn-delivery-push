package it.pagopa.pn.deliverypush.dto.webhook;

import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.ProgressResponseElementV25;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ProgressResponseElementDto {
    private List<ProgressResponseElementV25> progressResponseElementList;
    private int retryAfter;
}
