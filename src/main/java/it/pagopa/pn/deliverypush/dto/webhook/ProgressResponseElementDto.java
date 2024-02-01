package it.pagopa.pn.deliverypush.dto.webhook;

import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.ProgressResponseElementV23;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProgressResponseElementDto {
    private List<ProgressResponseElementV23> progressResponseElementList;
    private int retryAfter;
}
