package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.datavault;

import it.pagopa.pn.datavault.generated.openapi.clients.datavault.model.BaseRecipientDto;
import it.pagopa.pn.datavault.generated.openapi.clients.datavault.model.NotificationRecipientAddressesDto;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface PnDataVaultClientReactive {
    String CLIENT_NAME = "PN-DATA-VAULT";

    Flux<BaseRecipientDto> getRecipientsDenominationByInternalId(List<String> listInternalId);

    Mono<Void> updateNotificationAddressesByIun(String iun, Boolean normalized, List<NotificationRecipientAddressesDto> list);
}
