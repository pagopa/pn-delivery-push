package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.mandate;

import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.mandate.ApiClient;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.mandate.api.MandatePrivateServiceApi;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.mandate.model.CxTypeAuthFleet;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.mandate.model.InternalMandateDto;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@CustomLog
@Component
public class PnMandateClientImpl implements PnMandateClient {

    private final MandatePrivateServiceApi mandatesApi;

    public PnMandateClientImpl(@Qualifier("withTracing") RestTemplate restTemplate, PnDeliveryPushConfigs cfg) {
        ApiClient newApiClient = new ApiClient(restTemplate);
        newApiClient.setBasePath(cfg.getMandateBaseUrl());
        this.mandatesApi = new MandatePrivateServiceApi(newApiClient);
    }

    public List<InternalMandateDto> listMandatesByDelegate(String delegated,
                                                           String mandateId,
                                                           CxTypeAuthFleet cxType,
                                                           List<String> cxGroups) {
        log.logInvokingExternalService(CLIENT_NAME, GET_MANDATES_BY_DELEGATE);
        return mandatesApi.listMandatesByDelegate(delegated, cxType, mandateId, cxGroups);
    }

}
