package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.datavault;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.datavault.generated.openapi.clients.datavault.model.BaseRecipientDto;
import it.pagopa.pn.datavault.generated.openapi.clients.datavault_reactive.ApiClient;
import it.pagopa.pn.datavault.generated.openapi.clients.datavault_reactive.api.RecipientsApi;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.common.BaseClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@Slf4j
public class PnDataVaultClientReactiveImpl extends BaseClient implements PnDataVaultClientReactive {
    private final RecipientsApi recipientsApi;

    public PnDataVaultClientReactiveImpl(PnDeliveryPushConfigs cfg) {
        
        ApiClient newApiClient = new ApiClient( initWebClient(ApiClient.buildWebClientBuilder()).build() );
        newApiClient.setBasePath( cfg.getDataVaultBaseUrl() );

        this.recipientsApi = new RecipientsApi( newApiClient );
    }

    @Override
    @Retryable(
            value = {PnInternalException.class},
            maxAttempts = 3,
            backoff = @Backoff(random = true, delay = 500, maxDelay = 1000, multiplier = 2)
    )
    public Flux<BaseRecipientDto> getRecipientDenominationByInternalId(List<String> listInternalId) {
        log.debug("Start call getRecipientDenominationByInternalId - listInternalId={}", listInternalId);

        return recipientsApi.getRecipientDenominationByInternalId(listInternalId)
            .onErrorResume( err -> {
                log.error("Exception invoking getRecipientDenominationByInternalId with internalId list={} err ",listInternalId, err);
                return Mono.error(new PnInternalException("Exception invoking getRecipientDenominationByInternalId ", PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_UPDATEMETAFILEERROR, err));
            });
    }
}
