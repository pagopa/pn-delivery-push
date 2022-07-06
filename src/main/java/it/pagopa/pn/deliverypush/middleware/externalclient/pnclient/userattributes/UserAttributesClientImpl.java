package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.userattributes;

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
    public ResponseEntity<List<LegalDigitalAddress>> getLegalAddressBySender(String internalId, String senderId) {
        log.debug("Start getPlatformDigitalAddress for senderId {}", senderId);
        
        ResponseEntity<List<LegalDigitalAddress>> resp = legalApi.getLegalAddressBySenderWithHttpInfo(internalId, senderId);
        
        log.debug("Response to getPlatformDigitalAddress for senderId {}, have status code {}", senderId, resp.getStatusCode());
        
        return resp;
    }

    @Override
    public ResponseEntity<List<CourtesyDigitalAddress>> getCourtesyAddressBySender(String internalId, String senderId) {

        log.debug("Start getCourtesyAddress for senderId {}", senderId);

        ResponseEntity<List<CourtesyDigitalAddress>> resp = courtesyApi.getCourtesyAddressBySenderWithHttpInfo(internalId, senderId);

        log.debug("Response to getCourtesyAddress for senderId {}, have status code {}", senderId, resp.getStatusCode());

        return resp;
    }
}
