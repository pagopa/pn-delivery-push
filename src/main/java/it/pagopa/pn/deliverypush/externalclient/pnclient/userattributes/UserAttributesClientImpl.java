package it.pagopa.pn.deliverypush.externalclient.pnclient.userattributes;

import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.userattributes.generated.openapi.clients.userattributes.ApiClient;
import it.pagopa.pn.userattributes.generated.openapi.clients.userattributes.api.CourtesyApi;
import it.pagopa.pn.userattributes.generated.openapi.clients.userattributes.api.LegalApi;
import it.pagopa.pn.userattributes.generated.openapi.clients.userattributes.model.CourtesyDigitalAddress;
import it.pagopa.pn.userattributes.generated.openapi.clients.userattributes.model.LegalDigitalAddress;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Component
public class UserAttributesClientImpl implements UserAttributesClient {
    private final CourtesyApi courtesyApi;
    private final LegalApi legalApi;

    public UserAttributesClientImpl(@Qualifier("withTracing") RestTemplate restTemplate, PnDeliveryPushConfigs cfg) {
        ApiClient newApiClient = new ApiClient(restTemplate);
        newApiClient.setBasePath(cfg.getUserAttributesBaseUrl());
        this.courtesyApi = new CourtesyApi( newApiClient );
        this.legalApi = new LegalApi( newApiClient );
    }
    
    @Override
    public ResponseEntity<List<LegalDigitalAddress>> getLegalAddressBySender(String taxId, String senderId) {
        log.debug("Start getPlatformDigitalAddress for taxId={} senderId {}",taxId, senderId);
        
        ResponseEntity<List<LegalDigitalAddress>> resp = legalApi.getLegalAddressBySenderWithHttpInfo(taxId, senderId);
        
        log.debug("Response to getPlatformDigitalAddress for taxId={} senderId {}, have status code {}",taxId, senderId, resp.getStatusCode());
        
        return resp;
    }

    @Override
    public ResponseEntity<List<CourtesyDigitalAddress>> getCourtesyAddressBySender(String taxId, String senderId) {

        log.debug("Start getCourtesyAddress for taxId={} senderId {}",taxId, senderId);

        ResponseEntity<List<CourtesyDigitalAddress>> resp = courtesyApi.getCourtesyAddressBySenderWithHttpInfo(taxId, senderId);

        log.debug("Response to getCourtesyAddress for taxId={} senderId {}, have status code {}",taxId, senderId, resp.getStatusCode());

        return resp;
    }
}
