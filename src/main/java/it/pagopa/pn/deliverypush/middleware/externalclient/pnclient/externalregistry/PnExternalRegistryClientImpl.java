package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.externalregistry;

import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalregistry.api.RootSenderIdApi;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalregistry.api.SendIoMessageApi;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalregistry.model.RootSenderIdResponse;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalregistry.model.SendMessageRequest;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalregistry.model.SendMessageResponse;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
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


    public String getRootSenderId(String senderId){
        try{
            RootSenderIdResponse rootSenderIdPrivate = rootSenderIdApi.getRootSenderIdPrivate(senderId);
            return rootSenderIdPrivate.getRootId();
        }catch (Exception exc) {
            log.error("Error during map rootSenderID", exc);
            return "";
        }
    }
}
