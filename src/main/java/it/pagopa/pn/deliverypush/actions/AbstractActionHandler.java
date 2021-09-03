package it.pagopa.pn.deliverypush.actions;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.commons_delivery.middleware.TimelineDao;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionHandler;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionsPool;
import it.pagopa.pn.deliverypush.abstractions.actionspool.DigitalAddressSource;

public abstract class AbstractActionHandler implements ActionHandler {

    private final TimelineDao timelineDao;
    private final ActionsPool actionsPool;

    protected AbstractActionHandler(TimelineDao timelineDao, ActionsPool actionsPool) {
        this.timelineDao = timelineDao;
        this.actionsPool = actionsPool;
    }

    protected void scheduleAction( Action action ) {
        this.actionsPool.scheduleFutureAction( action.toBuilder()
                .actionId( action.getType().buildActionId( action ))
                .build()
            );
    }

    protected void addTimelineElement( Action action, TimelineElement row ) {
        this.timelineDao.addTimelineElement( row.toBuilder()
                .iun( action.getIun() )
                .timestamp( Instant.now() )
                .elementId( action.getActionId() )
                .build()
            );
    }

    protected <T> Optional<T> getTimelineElement(Action action, ActionType actionType, Class<T> timelineDetailsClass ) {
        Optional<TimelineElement> row;
        row = this.timelineDao.getTimelineElement( action.getIun(), actionType.buildActionId( action ) );

        return row.map( el -> timelineDetailsClass.cast( el.getDetails() ) );
    }

    protected Optional<Action> buildNextSendAction(Action action ) {
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

        Action.ActionBuilder nextActionBuilder = Action.builder()
                .iun( action.getIun() )
                .recipientIndex( action.getRecipientIndex() );

        Action nextAction;
        if( nextIsInFirstRound ) {
            nextAction = nextActionBuilder
                    .notBefore( Instant.now() )
                    .type( ActionType.SEND_PEC )
                    .digitalAddressSource( action.getDigitalAddressSource().next() )
                    .retryNumber( 1 )
                    .build();

        }
        else if ( nextIsInSecondRound ) {
            nextAction = nextActionBuilder
                    .notBefore( loadFirstAttemptTime( action ).plus( 70, ChronoUnit.SECONDS ) )
                    .type( ActionType.SEND_PEC )
                    .digitalAddressSource( action.getDigitalAddressSource().next() )
                    .retryNumber( 2 )
                    .build();
        }
        // If neither first nor second round: we have done with send attempt and can wait for recipient
        else {
            nextAction = null;
        }

        return Optional.ofNullable( nextAction );
    }

    protected Action buildWaitRecipientTimeoutAction(Action action ) {
        return Action.builder()
                .iun(action.getIun())
                .recipientIndex(action.getRecipientIndex())
                .notBefore(Instant.now().plus(5, ChronoUnit.MINUTES))
                .type(ActionType.WAIT_FOR_RECIPIENT_TIMEOUT)
                .build();
    }

    protected Action buildSendCourtesyAction(Action action ) {
        return Action.builder()
                .iun(action.getIun())
                .recipientIndex(action.getRecipientIndex())
                .notBefore(Instant.now())
                .type(ActionType.SEND_COURTESY_MESSAGES)
                .build();
    }

    private Instant loadFirstAttemptTime(Action action) {
        String firstAttemptResultActionId = ActionType.RECEIVE_PEC.buildActionId( action.toBuilder()
                .retryNumber( 1 )
                .build()
            );

        // FIXME: se non c'è il risultato verificare che manchi anche la richiesta di invio: se c'è è un anomalia.

        Optional<TimelineElement> firstAttemptResult = timelineDao.getTimeline( action.getIun() )
            .stream()
            .filter( timelineElement -> firstAttemptResultActionId.equals( timelineElement.getElementId() ))
            .findFirst();

        return firstAttemptResult
                .map( TimelineElement::getTimestamp )
                .orElse( Instant.now() ); // - If first attempt is absent can retry immediately
    }

    private static final Integer FIRST_ROUND = 1;
    private static final Integer SECOND_ROUND = 2;


}
