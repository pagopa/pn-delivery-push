package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.externalregistry;

import it.pagopa.pn.deliverypush.exceptions.PnRootIdNonFountException;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalregistry.api.RootSenderIdApi;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalregistry.api.SendIoMessageApi;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalregistry.model.RootSenderIdResponse;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalregistry.model.SendMessageRequest;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalregistry.model.SendMessageResponse;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;


@CustomLog
@RequiredArgsConstructor
@Component
public class PnExternalRegistryClientImpl implements PnExternalRegistryClient{

    private final SendIoMessageApi sendIoMessageApi;
    private final RootSenderIdApi rootSenderIdApi;
    
    @Override
    public SendMessageResponse sendIOMessage(SendMessageRequest sendMessageRequest) {
        log.logInvokingExternalService(CLIENT_NAME, SEND_IO_MESSAGE);
        
        ResponseEntity<SendMessageResponse> resp;
        resp = sendIoMessageApi.sendIOMessageWithHttpInfo(sendMessageRequest);

        return resp.getBody();
    }


    @Cacheable("aooSenderIdCache")
    public String getRootSenderId(String senderId){
        try{
            RootSenderIdResponse rootSenderIdPrivate = rootSenderIdApi.getRootSenderIdPrivate(senderId);
            return rootSenderIdPrivate.getRootId();
        }catch (Exception exc) {
            String message = String.format("Error during map rootSenderID = %s [exception received = %s]", senderId, exc);
            log.error(message);
            throw new PnRootIdNonFountException(message);
        }
    }
}
