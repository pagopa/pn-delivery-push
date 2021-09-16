package it.pagopa.pn.deliverypush.abstractions.actionspool.impl;



import it.pagopa.pn.deliverypush.abstractions.actionspool.LastPollForFutureActions;

import java.util.Optional;

public interface LastPollForFutureActionsDao {

    void updateLastPollForFutureActions(LastPollForFutureActions lastPollForFutureActions);

    Optional<LastPollForFutureActions> getLastPollForFutureActionsById();



}
