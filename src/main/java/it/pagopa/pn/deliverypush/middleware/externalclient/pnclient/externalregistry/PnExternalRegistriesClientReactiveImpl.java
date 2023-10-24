package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.externalregistry;

import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalregistry_reactive.api.UpdateNotificationCostApi;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalregistry_reactive.model.UpdateNotificationCostRequest;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalregistry_reactive.model.UpdateNotificationCostResponse;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@CustomLog
public class PnExternalRegistriesClientReactiveImpl extends CommonBaseClient implements PnExternalRegistriesClientReactive {
    private final UpdateNotificationCostApi updateNotificationCostApi;

    public Mono<UpdateNotificationCostResponse> updateNotificationCost(UpdateNotificationCostRequest updateNotificationCostRequest) {
        return updateNotificationCostApi.updateNotificationCost(updateNotificationCostRequest);
    }

}
