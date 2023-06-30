package it.pagopa.pn.deliverypush.action.analogworkflow;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.action.completionworkflow.CompletionWorkFlowHandler;
import it.pagopa.pn.deliverypush.action.utils.EndWorkflowStatus;
import it.pagopa.pn.deliverypush.action.utils.InstantNowSupplier;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.PaperChannelService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@Slf4j
public class AnalogWorkflowHandler {

    public static final int ATTEMPT_MADE_UNREACHABLE = 2; 

    private final NotificationService notificationService;
    private final PaperChannelService paperChannelService;
    private final CompletionWorkFlowHandler completionWorkFlow;
    private final InstantNowSupplier instantNowSupplier;
    public AnalogWorkflowHandler(NotificationService notificationService,
                                 PaperChannelService paperChannelService,
                                 CompletionWorkFlowHandler completionWorkFlow,
                                 InstantNowSupplier instantNowSupplier) {
        this.notificationService = notificationService;
        this.paperChannelService = paperChannelService;
        this.completionWorkFlow = completionWorkFlow;
        this.instantNowSupplier = instantNowSupplier;
    }

    public void startAnalogWorkflow(String iun, Integer recIndex) {
        NotificationInt notification = notificationService.getNotificationByIun(iun);
        nextWorkflowStep(notification, recIndex, 0, null);
    }

    /**
     * Handle analog notification Workflow based on already made attempt
     */
    public void nextWorkflowStep(NotificationInt notification, Integer recIndex, int sentAttemptMade, Instant lastAttemptTimestamp) {
        log.info("Start Analog next workflow action - iun={} id={}", notification.getIun(), recIndex);

        String iun = notification.getIun();
        log.debug("Sent attempt made is={} - iun={} id={}", sentAttemptMade, iun, recIndex);

        switch (sentAttemptMade) {
            case 0:
                log.info("Handle first send attempt - iun={} id={}", iun, recIndex);

                log.info("Start prepare analog notification with Pa address - iun={} id={} sentAttemptMade={}", iun, recIndex, sentAttemptMade);
                //send notification with paAddress
                paperChannelService.prepareAnalogNotification(notification, recIndex, sentAttemptMade);

                break;
            case 1:
                log.info("Handle second attempt, send prepare analog request  - iun={} id={} sentAttemptMade={}", iun, recIndex, sentAttemptMade);
                //Send attempt was already made, send prepare analog for second send attempt
                paperChannelService.prepareAnalogNotification(notification, recIndex, sentAttemptMade);
                break;
            case 2:
                // All sent attempts have been made. The user is not reachable
                log.info("User with iun={} and id={} is unreachable, all attempt was failed or no adress is available", iun, recIndex);
                completionWorkFlow.completionAnalogWorkflow(notification, recIndex, lastAttemptTimestamp!=null?lastAttemptTimestamp:instantNowSupplier.get(), null, EndWorkflowStatus.FAILURE);
                break;
            default:
                handleAttemptError(iun, recIndex, sentAttemptMade);
        }
    }



    private void handleAttemptError(String iun, Integer recIndex, int sentAttemptMade) {
        log.error("Specified attempt={} is not possibile  - iun={} id={}", sentAttemptMade, iun, recIndex);
        throw new PnInternalException("Specified attempt " + sentAttemptMade + " is not possibile", PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_INVALIDATTEMPT);
    }


}
