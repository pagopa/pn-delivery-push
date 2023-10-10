package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.externalregistry;

import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalregistry_reactive.model.UpdateNotificationCostRequest;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalregistry_reactive.model.UpdateNotificationCostResponse;
import reactor.core.publisher.Mono;

public interface PnExternalRegistriesClientReactive {
    String CLIENT_NAME = PnLogger.EXTERNAL_SERVICES.PN_EXTERNAL_REGISTRIES;

    Mono<UpdateNotificationCostResponse> updateNotificationCost(UpdateNotificationCostRequest updateNotificationCostRequest);
}
