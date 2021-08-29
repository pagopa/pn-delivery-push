package it.pagopa.pn.deliverypush.abstractions.actionspool.impl;

import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;

import java.util.List;
import java.util.Optional;

public interface ActionDao {

    void addAction( Action action, String timeSlot );

    Optional<Action> getActionById( String actionId );

    List<Action> findActionsByTimeSlot( String timeSlot );

    void unSchedule( Action action, String timeSlot );
}
