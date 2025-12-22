package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.externalregistry;

import it.pagopa.pn.deliverypush.exceptions.PnRootIdNonFoundException;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalregistry.api.RootSenderIdApi;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalregistry.model.RootSenderIdResponse;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;


@CustomLog
@RequiredArgsConstructor
@Component
public class PnExternalRegistryClientImpl implements PnExternalRegistryClient{
    private final RootSenderIdApi rootSenderIdApi;

    @Override
    @Cacheable("aooSenderIdCache")
    public String getRootSenderId(String senderId){
        try{
            RootSenderIdResponse rootSenderIdPrivate = rootSenderIdApi.getRootSenderIdPrivate(senderId);
            return rootSenderIdPrivate.getRootId();
        }catch (Exception exc) {
            String message = String.format("Error during map rootSenderID = %s [exception received = %s]", senderId, exc);
            log.error(message);
            throw new PnRootIdNonFoundException(message);
        }
    }
}
