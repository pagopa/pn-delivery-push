package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.datavault;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.datavault.generated.openapi.clients.datavault.model.BaseRecipientDto;
import it.pagopa.pn.datavault.generated.openapi.clients.datavault.model.NotificationRecipientAddressesDto;
import it.pagopa.pn.datavault.generated.openapi.clients.datavault_reactive.ApiClient;
import it.pagopa.pn.datavault.generated.openapi.clients.datavault_reactive.api.NotificationsApi;
import it.pagopa.pn.datavault.generated.openapi.clients.datavault_reactive.api.RecipientsApi;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@Slf4j
public class PnDataVaultClientReactiveImpl extends CommonBaseClient implements PnDataVaultClientReactive {
    private final RecipientsApi recipientsApi;
    private final NotificationsApi notificationApi;

    public PnDataVaultClientReactiveImpl(PnDeliveryPushConfigs cfg) {
        
        ApiClient newApiClient = new ApiClient( initWebClient(ApiClient.buildWebClientBuilder()) );
        newApiClient.setBasePath( cfg.getDataVaultBaseUrl() );

        this.recipientsApi = new RecipientsApi( newApiClient );
        this.notificationApi = new NotificationsApi(newApiClient);
    }

    @Override
    @Retryable(
            value = {PnInternalException.class},
            maxAttempts = 3,
            backoff = @Backoff(random = true, delay = 500, maxDelay = 1000, multiplier = 2)
    )
    public Flux<BaseRecipientDto> getRecipientsDenominationByInternalId(List<String> listInternalId) {
        log.debug("Start call getRecipientDenominationByInternalId - listInternalId={}", listInternalId);

        return recipientsApi.getRecipientDenominationByInternalId(listInternalId)
            .onErrorResume( err -> {
                log.error("Exception invoking getRecipientDenominationByInternalId with internalId list={} err ",listInternalId, err);
                return Mono.error(new PnInternalException("Exception invoking getRecipientDenominationByInternalId ", PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_UPDATEMETAFILEERROR, err));
            });
    }

    @Override
    public Mono<Void> updateNotificationAddressesByIun(String iun, Boolean normalized, List<NotificationRecipientAddressesDto> list) {
        log.debug("Start call getNotificationTimelineByIunWithHttpInfo - iun={}", iun);

        return notificationApi.updateNotificationAddressesByIun(iun, normalized, list)
                .doOnSuccess( res -> log.debug("Response updateNotificationAddressesByIun - iun={}", iun));

    }
}
