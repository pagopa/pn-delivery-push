package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.datavault;

import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.datavault.model.BaseRecipientDto;
import reactor.core.publisher.Flux;

import java.util.List;

public interface PnDataVaultClientReactive {
    String CLIENT_NAME = PnLogger.EXTERNAL_SERVICES.PN_DATA_VAULT;
    String GET_RECIPIENT_DENOMINATION = "GET RECIPIENT DENOMINATION";

    Flux<BaseRecipientDto> getRecipientsDenominationByInternalId(List<String> listInternalId);

}
