package it.pagopa.pn.deliverypush.action.it.mockbean;

import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalregistry_reactive.model.UpdateNotificationCostRequest;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalregistry_reactive.model.UpdateNotificationCostResponse;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.externalregistry.PnExternalRegistriesClientReactive;
import reactor.core.publisher.Mono;

public class PnExternalRegistriesClientReactiveMock implements PnExternalRegistriesClientReactive {
    @Override
    public Mono<UpdateNotificationCostResponse> updateNotificationCost(UpdateNotificationCostRequest updateNotificationCostRequest) {
        throw new UnsupportedOperationException();
    }
}
