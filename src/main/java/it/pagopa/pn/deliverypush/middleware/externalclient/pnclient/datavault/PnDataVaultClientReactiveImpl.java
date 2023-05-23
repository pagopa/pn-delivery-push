package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.datavault;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.datavault.model.BaseRecipientDto;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.datavault.model.NotificationRecipientAddressesDto;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.datavault_reactive.ApiClient;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.datavault_reactive.api.NotificationsApi;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.datavault_reactive.api.RecipientsApi;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes;
import lombok.CustomLog;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@CustomLog
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
        log.logInvokingExternalService(CLIENT_NAME, GET_RECIPIENT_DENOMINATION);

        return recipientsApi.getRecipientDenominationByInternalId(listInternalId)
                .onErrorResume( err -> {
                    log.error("Exception invoking getRecipientDenominationByInternalId with internalId list={} err ",listInternalId, err);
                    return Mono.error(new PnInternalException("Exception invoking getRecipientDenominationByInternalId ", PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_UPDATEMETAFILEERROR, err));
                });
    }

    @Override
    public Mono<Void> updateNotificationAddressesByIun(String iun, Boolean normalized, List<NotificationRecipientAddressesDto> list) {
        log.logInvokingExternalService(CLIENT_NAME, UPDATE_NOTIFICATION_ADDRESS);
        
        return notificationApi.updateNotificationAddressesByIun(iun, normalized, list)
                .doOnSuccess( res -> log.debug("Received sync response from {} for {}", CLIENT_NAME, UPDATE_NOTIFICATION_ADDRESS));
    }
}
