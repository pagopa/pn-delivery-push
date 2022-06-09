package it.pagopa.pn.deliverypush.externalclient.pnclient.mandate;

import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.mandate.generated.openapi.clients.mandate.ApiClient;
import it.pagopa.pn.mandate.generated.openapi.clients.mandate.api.MandatePrivateServiceApi;
import it.pagopa.pn.mandate.generated.openapi.clients.mandate.model.InternalMandateDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Component
public class PnMandateClientImpl implements PnMandateClient{

    private final MandatePrivateServiceApi mandatesApi;

    public PnMandateClientImpl(@Qualifier("withTracing") RestTemplate restTemplate, PnDeliveryPushConfigs cfg) {
        ApiClient newApiClient = new ApiClient(restTemplate);
        newApiClient.setBasePath(cfg.getMandateBaseUrl());
        this.mandatesApi = new MandatePrivateServiceApi( newApiClient );
    }

    public List<InternalMandateDto> listMandatesByDelegate(String delegated, String mandateId) {
        log.debug("Start get mandates - delegated={} and mandateId={}", delegated, mandateId);
        
        List<InternalMandateDto> listMandateDto =  mandatesApi.listMandatesByDelegate( delegated, mandateId );
        
        log.debug("Response get mandates - delegated={} and mandateId={}", delegated, mandateId);
        
        return listMandateDto;
    }

}
