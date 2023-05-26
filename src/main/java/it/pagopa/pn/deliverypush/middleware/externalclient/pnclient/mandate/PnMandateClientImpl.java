package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.mandate;

import it.pagopa.pn.deliverypush.generated.openapi.msclient.mandate.api.MandatePrivateServiceApi;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.mandate.model.CxTypeAuthFleet;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.mandate.model.InternalMandateDto;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@CustomLog
@RequiredArgsConstructor
@Component
public class PnMandateClientImpl implements PnMandateClient {
    private final MandatePrivateServiceApi mandatesApi;
    
    public List<InternalMandateDto> listMandatesByDelegate(String delegated,
                                                           String mandateId,
                                                           CxTypeAuthFleet cxType,
                                                           List<String> cxGroups) {
        log.logInvokingExternalService(CLIENT_NAME, GET_MANDATES_BY_DELEGATE);
        return mandatesApi.listMandatesByDelegate(delegated, cxType, mandateId, cxGroups);
    }

}
