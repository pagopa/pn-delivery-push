package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.userattributes;

import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.userattributes.ApiClient;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.userattributes.api.CourtesyApi;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.userattributes.api.LegalApi;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.userattributes.model.CourtesyDigitalAddress;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.userattributes.model.LegalDigitalAddress;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@CustomLog
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
    public List<LegalDigitalAddress> getLegalAddressBySender(String recipientId, String senderId) {
        log.logInvokingExternalService(CLIENT_NAME, GET_DIGITAL_PLATFORM_ADDRESS);

        ResponseEntity<List<LegalDigitalAddress>> resp = legalApi.getLegalAddressBySenderWithHttpInfo(recipientId, senderId);
        return resp.getBody();
    }

    @Override
    public List<CourtesyDigitalAddress> getCourtesyAddressBySender(String recipientId, String senderId) {
        log.logInvokingExternalService(CLIENT_NAME, GET_COURTESY_ADDRESS);
        
        ResponseEntity<List<CourtesyDigitalAddress>> resp = courtesyApi.getCourtesyAddressBySenderWithHttpInfo(recipientId, senderId);
        return resp.getBody();
    }
}
