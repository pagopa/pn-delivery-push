package it.pagopa.pn.deliverypush.actions;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons_delivery.middleware.TimelineDao;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionHandler;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionsPool;
import it.pagopa.pn.deliverypush.abstractions.actionspool.DigitalAddressSource;

import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;

public abstract class AbstractActionHandler implements ActionHandler {

    private final TimelineDao timelineDao;
    private final ActionsPool actionsPool;
    private final PnDeliveryPushConfigs pnDeliveryPushConfigs;

    protected AbstractActionHandler(TimelineDao timelineDao, ActionsPool actionsPool, PnDeliveryPushConfigs pnDeliveryPushConfigs) {
        this.timelineDao = timelineDao;
        this.actionsPool = actionsPool;
        this.pnDeliveryPushConfigs = pnDeliveryPushConfigs;
    }

    protected void scheduleAction(Action action) {
        this.actionsPool.scheduleFutureAction( action.toBuilder()
                .actionId( action.getType().buildActionId( action ))
                .build()
            );
    }
    protected void addTimelineElement(Action action, TimelineElement row) {
        this.timelineDao.addTimelineElement( row.toBuilder()
                .iun( action.getIun() )
                .timestamp( Instant.now() )
                .elementId( action.getActionId() )
                .build()
            );
    }
    protected  <T> Optional<T> getTimelineElement(Action action, ActionType actionType, Class<T> timelineDetailsClass) {
        Optional<TimelineElement> row;
        row = this.timelineDao.getTimelineElement( action.getIun(), actionType.buildActionId( action ) );

        return row.map( el -> timelineDetailsClass.cast( el.getDetails() ) );
    }
    protected  boolean isPresentTimeLineElement(Action action, ActionType actionType) {
        return this.timelineDao.getTimelineElement( action.getIun(), actionType.buildActionId( action ) ).isPresent();
    }
    protected Action buildNextSendAction(Action action ) {
        boolean nextIsInFirstRound = FIRST_ROUND.equals( action.getRetryNumber() )
                && ! DigitalAddressSource.GENERAL.equals( action.getDigitalAddressSource() );

        boolean actionIsLastOfFirstRound = FIRST_ROUND.equals( action.getRetryNumber() )
                && DigitalAddressSource.GENERAL.equals( action.getDigitalAddressSource() );

        boolean nextIsInSecondRound = actionIsLastOfFirstRound
                || (
                        SECOND_ROUND.equals( action.getRetryNumber() )
                    &&
                        ! DigitalAddressSource.GENERAL.equals( action.getDigitalAddressSource() )
                );

        Action nextAction;
        if( nextIsInFirstRound ) {
            nextAction = buildNextSendPecActionWithRound( action, FIRST_ROUND);
        }
        else if ( nextIsInSecondRound ) {
            nextAction = buildNextSendPecActionWithRound( action, SECOND_ROUND);
        }
        // If neither first nor second round: we have done with send attempt and can proceed with paper delivery request
        else {
            nextAction = buildSendPaperAfterPecAction( action );
        }

        return nextAction;
    }

    protected Action buildSendPaperAfterPecAction(Action action ) {
    	return Action.builder()
                .iun( action.getIun() )
                .recipientIndex( action.getRecipientIndex() )
                .notBefore( Instant.now() )
                .type( ActionType.PEC_FAIL_SEND_PAPER )
                .build();
    }

    protected Action buildWaitRecipientTimeoutActionForDigital(Action action ) {
        Duration recipientViewMaxTime = pnDeliveryPushConfigs.getTimeParams().getRecipientViewMaxTimeForDigital();
        return buildWaitRecipientTimeoutAction(action,recipientViewMaxTime);
    }

    protected Action buildWaitRecipientTimeoutActionForAnalog(Action action ) {
        Duration recipientViewMaxTime = pnDeliveryPushConfigs.getTimeParams().getRecipientViewMaxTimeForAnalog();
        return buildWaitRecipientTimeoutAction(action,recipientViewMaxTime);
    }
    
    protected Action buildWaitRecipientTimeoutAction(Action action, Duration delay ) {
        return Action.builder()
                .iun(action.getIun())
                .recipientIndex(action.getRecipientIndex())
                .notBefore(Instant.now().plus(delay) )
                .type(ActionType.WAIT_FOR_RECIPIENT_TIMEOUT)
                .build();
    }

    protected Action buildEndofDigitalWorkflowAction(Action action ) {
        return Action.builder()
                .iun(action.getIun())
                .recipientIndex(action.getRecipientIndex())
                .notBefore(Instant.now())
                .type(ActionType.END_OF_DIGITAL_DELIVERY_WORKFLOW)
                .build();
    }

    protected Action buildEndofAnalogWorkflowAction(Action action ) {
        return Action.builder()
                .iun(action.getIun())
                .recipientIndex(action.getRecipientIndex())
                .notBefore(Instant.now())
                .type(ActionType.END_OF_ANALOG_DELIVERY_WORKFLOW)
                .build();
    }

    protected Action buildCompletelyUnreachableAction(Action action ) {
        return Action.builder()
                .iun(action.getIun())
                .recipientIndex(action.getRecipientIndex())
                .notBefore(Instant.now())
                .type(ActionType.COMPLETELY_UNREACHABLE)
                .build();
    }

    private Instant loadFirstAttemptTime(Action action) {
        String firstAttemptResultActionId = ActionType.RECEIVE_PEC.buildActionId( action.toBuilder()
                .retryNumber( 1 )
                .build()
            );

        // FIXME: se non c'è il risultato verificare che manchi anche la richiesta di invio: se c'è è un anomalia.

        Set<TimelineElement> timeline = timelineDao.getTimeline(action.getIun());

        Optional<TimelineElement> firstAttemptResult = timeline.stream()
            .filter( timelineElement -> firstAttemptResultActionId.equals( timelineElement.getElementId() ))
            .findFirst();

        if( firstAttemptResult.isEmpty() ) {
            String firstAttemptActionId = ActionType.SEND_PEC.buildActionId( action.toBuilder()
                    .retryNumber( 1 )
                    .build()
                );

            firstAttemptResult = timeline.stream()
                    .filter( timelineElement -> firstAttemptActionId.equals( timelineElement.getElementId() ))
                    .findFirst();

        }

        return firstAttemptResult
                .map( TimelineElement::getTimestamp )
                .orElse( Instant.now() ); // - If first attempt is absent can retry immediately
    }

    protected Action buildNextSendPecActionWithRound(Action action, Integer roundNumber){
        Instant actionTime;
        if ( FIRST_ROUND.equals( roundNumber )) {
            Duration actionDelay = pnDeliveryPushConfigs.getTimeParams().getWaitingResponseFromFirstAddress();
            actionTime = Instant.now().plus( actionDelay );
        } else if( SECOND_ROUND.equals( roundNumber )) {
            Duration actionDelay = pnDeliveryPushConfigs.getTimeParams().getSecondAttemptWaitingTime();
            actionTime = loadFirstAttemptTime( action ).plus( actionDelay );
        }
        else {
            throw new PnInternalException("Pec workflow: not supported round " + roundNumber);
        }

        Instant now = Instant.now();
        if( now.isAfter( actionTime )) {
            actionTime = now;
        }

        return Action.builder()
                .iun( action.getIun() )
                .recipientIndex( action.getRecipientIndex() )
                .notBefore(actionTime )
                .type( ActionType.SEND_PEC )
                .digitalAddressSource( action.getDigitalAddressSource().next() )
                .retryNumber( roundNumber )
                .build();
    }

    protected static final Integer FIRST_ROUND = 1;
    protected static final Integer SECOND_ROUND = 2;
    protected static final Integer LAST_ROUND = SECOND_ROUND;


    protected List<Action> replicateForEachDigitalWorkflowAction(Action templateAction) {
        List<Action> result = new ArrayList<>();

        Action.ActionBuilder builder =  templateAction.toBuilder();

        for( DigitalAddressSource das: DigitalAddressSource.values() ) {
            for( int retryNum = FIRST_ROUND; retryNum <= LAST_ROUND; retryNum++ ) {
                Action actionWithoutId = builder
                        .digitalAddressSource( das )
                        .retryNumber( retryNum )
                        .build();
                Action actionWithId = actionWithoutId.toBuilder()
                        .actionId( actionWithoutId.getType().buildActionId( actionWithoutId ))
                        .build();
                result.add( actionWithId );
            }
        }
        return result;
    }
}
