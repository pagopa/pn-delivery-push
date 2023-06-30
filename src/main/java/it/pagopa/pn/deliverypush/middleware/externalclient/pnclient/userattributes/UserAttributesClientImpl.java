package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.userattributes;

import it.pagopa.pn.deliverypush.generated.openapi.msclient.userattributes.api.CourtesyApi;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.userattributes.api.LegalApi;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.userattributes.model.CourtesyDigitalAddress;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.userattributes.model.LegalDigitalAddress;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;

@CustomLog
@RequiredArgsConstructor
@Component
public class UserAttributesClientImpl implements UserAttributesClient {
    private final CourtesyApi courtesyApi;
    private final LegalApi legalApi;
    
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
