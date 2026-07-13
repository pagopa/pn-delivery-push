package it.pagopa.pn.deliverypush.action.details;

import it.pagopa.pn.deliverypush.middleware.dao.notificationreworkdao.dynamo.entity.ReworkRequestType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationReworkValidationDetails {
    private String reworkId;
    private String reworkAttempt;
    private String reworkPcRetry;
    private String reworkRecIndex;
    private String reworkExpectedFinalStatus;
    private String reason;
    private ReworkRequestType reworkRequestType;
    private String task;
    private List<String> elementsToInvalidate;
}
