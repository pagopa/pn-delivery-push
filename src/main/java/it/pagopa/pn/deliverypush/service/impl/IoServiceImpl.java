package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.externalregistry.PnExternalRegistryClient;
import it.pagopa.pn.deliverypush.service.IoService;
import it.pagopa.pn.externalregistry.generated.openapi.clients.externalregistry.model.SendMessageRequest;
import it.pagopa.pn.externalregistry.generated.openapi.clients.externalregistry.model.SendMessageResponse;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;


@Slf4j
@Service
public class IoServiceImpl implements IoService {
    private final PnExternalRegistryClient pnExternalRegistryClient;
    private final NotificationUtils notificationUtils;
    
    public IoServiceImpl(PnExternalRegistryClient pnExternalRegistryClient,
                         NotificationUtils notificationUtils) {
        this.pnExternalRegistryClient = pnExternalRegistryClient;
        this.notificationUtils = notificationUtils;
    }

    @Override
    public void sendIOMessage(NotificationInt notification, int recIndex) {
        log.info("Start send message to App IO - iun={} id={}", notification.getIun(), recIndex);

        NotificationRecipientInt recipientInt = notificationUtils.getRecipientFromIndex(notification, recIndex);

        SendMessageRequest sendMessageRequest = getSendMessageRequest(notification, recipientInt);

        ResponseEntity<SendMessageResponse> resp = pnExternalRegistryClient.sendIOMessage(sendMessageRequest);

        if (resp.getStatusCode().is2xxSuccessful()) {
            
            SendMessageResponse sendIoMessageResponse = resp.getBody();
            log.info("sendIOMessage OK - iun={} id={} responseId={}", notification.getIun(), recIndex, 
                    sendIoMessageResponse != null ? sendIoMessageResponse.getId() : null);

        } else {
            log.error("sendIOMessage Failed - iun={} id={}", notification.getIun(), recIndex);
            throw new PnInternalException("sendIOMessage Failed - iun "+ notification.getIun() +" id "+ recIndex);
        }
    }

    @NotNull
    private SendMessageRequest getSendMessageRequest(NotificationInt notification, NotificationRecipientInt recipientInt) {
        SendMessageRequest sendMessageRequest = new SendMessageRequest();
        sendMessageRequest.setAmount(notification.getAmount());
        sendMessageRequest.setDueDate(notification.getPaymentExpirationDate());
        sendMessageRequest.setRecipientTaxID(recipientInt.getTaxId());
        sendMessageRequest.setRequestAcceptedDate(notification.getSentAt());
        sendMessageRequest.setSenderDenomination(notification.getSender().getPaDenomination());
        sendMessageRequest.setIun(notification.getIun());
        
        String subject = notification.getSender().getPaDenomination() +"-"+ notification.getSubject();
        sendMessageRequest.setSubject(subject);

        return sendMessageRequest;
    }
}
