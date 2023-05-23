package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.datavault;

import it.pagopa.pn.deliverypush.generated.openapi.msclient.datavault.model.BaseRecipientDto;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.datavault.model.NotificationRecipientAddressesDto;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface PnDataVaultClientReactive {
    String CLIENT_NAME = "PN-DATA-VAULT";
    String GET_RECIPIENT_DENOMINATION = "GET RECIPIENT DENOMINATION";
    String UPDATE_NOTIFICATION_ADDRESS = "UPDATE CONFIDENTIAL INFO, NOTIFICATION ADDRESS";

    Flux<BaseRecipientDto> getRecipientsDenominationByInternalId(List<String> listInternalId);

    Mono<Void> updateNotificationAddressesByIun(String iun, Boolean normalized, List<NotificationRecipientAddressesDto> list);
}
