package it.pagopa.pn.deliverypush.actions;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.timeline.EndOfDigitalDeliveryWorkflowDetails;
import it.pagopa.pn.api.dto.notification.timeline.NotificationPathChooseDetails;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElementCategory;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons_delivery.middleware.TimelineDao;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionsPool;
import it.pagopa.pn.deliverypush.legalfacts.LegalFactDao;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class EndOfDigitalDeliveryWorkflowActionHandler extends AbstractActionHandler {

    private final ActionsPool actionsPool;
    private final LegalFactDao legalFactStore;

    public EndOfDigitalDeliveryWorkflowActionHandler(TimelineDao timelineDao, ActionsPool actionsPool,
                                                     LegalFactDao legalFactStore, PnDeliveryPushConfigs pnDeliveryPushConfigs) {
        super(timelineDao, actionsPool, pnDeliveryPushConfigs);
        this.actionsPool = actionsPool;
        this.legalFactStore = legalFactStore;
    }

    @Override
    public void handleAction(Action action, Notification notification) {
    	// - Retrieve addresses
    	Optional<NotificationPathChooseDetails> addressesOpt =
                getTimelineElement( action, ActionType.CHOOSE_DELIVERY_MODE, NotificationPathChooseDetails.class );

    	if( addressesOpt.isPresent() ) {
            NotificationPathChooseDetails addresses = addressesOpt.get();


            List<Action> receivePecActions = super.replicateForEachDigitalWorkflowAction(
                    Action.builder()
                            .type( ActionType.RECEIVE_PEC )
                            .iun( action.getIun() )
                            .recipientIndex( action.getRecipientIndex() )
                            .build()
                )
                .stream()
                .map( receivePecAction -> actionsPool.loadActionById( receivePecAction.getActionId() ))
                .filter( Optional::isPresent )
                .map( Optional::get )
                .collect(Collectors.toList());

            //FIXME recuperare se presente la timeline dell'azione di PEC_FAIL_RECEIVE_PAPER con iun e recipientId recuperati dall'action

            legalFactStore.savePecDeliveryWorkflowLegalFact( receivePecActions, notification, addresses );

            // - GENERATE NEXT ACTIONS
            Action nextAction = buildWaitRecipientTimeoutActionForDigital(action);
            scheduleAction(nextAction);

            // - WRITE TIMELINE
            NotificationRecipient recipient = notification.getRecipients().get(action.getRecipientIndex());
            addTimelineElement(action, TimelineElement.builder()
                    .category( TimelineElementCategory.END_OF_DIGITAL_DELIVERY_WORKFLOW )
                    .details( EndOfDigitalDeliveryWorkflowDetails.builder()
                            .taxId(recipient.getTaxId())
                            .build()
                    )
                    .build()
            );
        }
    	else {
            throw new PnInternalException( "Addresses list not found!!! Needed for action " + action );
        }
    }

    @Override
    public ActionType getActionType() {
        return ActionType.END_OF_DIGITAL_DELIVERY_WORKFLOW;
    }
}
