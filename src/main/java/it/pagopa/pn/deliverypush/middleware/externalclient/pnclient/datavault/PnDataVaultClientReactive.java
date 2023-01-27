package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.datavault;

import it.pagopa.pn.datavault.generated.openapi.clients.datavault.model.BaseRecipientDto;
import reactor.core.publisher.Flux;

import java.util.List;

public interface PnDataVaultClientReactive {
    Flux<BaseRecipientDto> getRecipientsDenominationByInternalId(List<String> listInternalId);
}
