package it.pagopa.pn.deliverypush.abstractions.actionspool;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.Notification;

public interface ActionHandler {

    void handleAction(Action action, Notification notification);

    ActionType getActionType();
}
