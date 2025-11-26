package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.actionmanager;

import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.actionmanager.model.NewAction;

public interface ActionManagerClient {
    String CLIENT_NAME = PnLogger.EXTERNAL_SERVICES.PN_ACTION_MANAGER;
    String ADD_ONLY_ACTION_IF_ABSENT_PROCESS_NAME = "ADD ONLY ACTION IF ABSENT";

    void addOnlyActionIfAbsent(NewAction action);
}
