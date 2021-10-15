package it.pagopa.pn.deliverypush.actions;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.timeline.*;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons_delivery.middleware.TimelineDao;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionsPool;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class EndOfAnalogDeliveryWorkflowActionHandler extends AbstractActionHandler {

    private final ActionsPool actionsPool;
    private final LegalFactUtils legalFactStore;

    public EndOfAnalogDeliveryWorkflowActionHandler(TimelineDao timelineDao, ActionsPool actionsPool,
                                                    LegalFactUtils legalFactStore, PnDeliveryPushConfigs pnDeliveryPushConfigs) {
        super(timelineDao, actionsPool, pnDeliveryPushConfigs);
        this.actionsPool = actionsPool;
        this.legalFactStore = legalFactStore;
    }

    @Override
    public void handleAction(Action action, Notification notification) {

    	List<SendPaperFeedbackDetails> paperTrail = loadPaperTrail(action);

        legalFactStore.savePaperDeliveryWorkflowLegalFact( paperTrail, notification );

        // - GENERATE NEXT ACTIONS
        Action nextAction = buildWaitRecipientTimeoutActionForAnalog(action);
        scheduleAction(nextAction);

        // - WRITE TIMELINE
        NotificationRecipient recipient = notification.getRecipients().get(action.getRecipientIndex());
        addTimelineElement(action, TimelineElement.builder()
                .category( TimelineElementCategory.END_OF_ANALOG_DELIVERY_WORKFLOW )
                .details(EndOfAnalogDeliveryWorkflowDetails.builder()
                        .taxId(recipient.getTaxId())
                        .build()
                )
                .build()
        );

    }

    private List<SendPaperFeedbackDetails> loadPaperTrail(Action action) {

        List<SendPaperFeedbackDetails> paperTrail = new ArrayList<>();

        boolean exists = true;
        for (int i = 1; exists ; i++){
            Action paperRetrieve = action.toBuilder()
                    .type(ActionType.RECEIVE_PAPER)
                    .retryNumber( i )
                    .build();
            Optional<SendPaperFeedbackDetails> paperRetrieveDetail = getTimelineElement(
                    paperRetrieve,
                    ActionType.RECEIVE_PAPER,
                    SendPaperFeedbackDetails.class);
            if(paperRetrieveDetail.isPresent()){
                paperTrail.add(paperRetrieveDetail.get());
            } else {
                exists = false;
            }
        }
        return paperTrail;
    }

    @Override
    public ActionType getActionType() {
        return ActionType.END_OF_ANALOG_DELIVERY_WORKFLOW;
    }
}
