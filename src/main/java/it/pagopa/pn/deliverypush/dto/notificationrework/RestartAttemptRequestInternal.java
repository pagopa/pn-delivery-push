package it.pagopa.pn.deliverypush.dto.notificationrework;

import lombok.Data;

@Data
public class RestartAttemptRequestInternal {
    private String iun;
    private String attemptId;
    private String recIndex;
    private String reason;
    private String productType;
}
