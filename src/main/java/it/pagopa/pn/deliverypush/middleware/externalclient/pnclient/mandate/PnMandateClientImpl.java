package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.mandate;

import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.mandate.generated.openapi.clients.mandate.ApiClient;
import it.pagopa.pn.mandate.generated.openapi.clients.mandate.api.MandatePrivateServiceApi;
import it.pagopa.pn.mandate.generated.openapi.clients.mandate.model.CxTypeAuthFleet;
import it.pagopa.pn.mandate.generated.openapi.clients.mandate.model.InternalMandateDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
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
        log.debug("Start get mandates - delegated={}, mandateId={}, cxType={}, cxGroups={}", delegated, mandateId, cxType, cxGroups);

        List<InternalMandateDto> listMandateDto = mandatesApi.listMandatesByDelegate(delegated, cxType, mandateId, cxGroups);

        if (listMandateDto != null && !listMandateDto.isEmpty()) {
            log.debug("Response get mandates - delegated={}, mandateId={}, cxType={}, cxGroups={} - response size={}",
                    delegated, mandateId, cxType, cxGroups, listMandateDto.size());
        } else {
            log.debug("Response get mandates is empty - delegated={}, mandateId={}, cxType={}, cxGroups={}",
                    delegated, mandateId, cxType, cxGroups);
        }

        return listMandateDto;
    }

}
