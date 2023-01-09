package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.datavault;

import it.pagopa.pn.datavault.generated.openapi.clients.datavault.model.BaseRecipientDto;
import it.pagopa.pn.datavault.generated.openapi.clients.datavault_reactive.ApiClient;
import it.pagopa.pn.datavault.generated.openapi.clients.datavault_reactive.api.RecipientsApi;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.common.BaseClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

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
    public Flux<BaseRecipientDto> getRecipientDenominationByInternalId(List<String> listInternalId) {
        log.debug("Start call getRecipientDenominationByInternalId - listInternalId={}", listInternalId);

        return recipientsApi.getRecipientDenominationByInternalId(listInternalId);
    }
}
