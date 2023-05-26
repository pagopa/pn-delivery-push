package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.externalregistry.PnExternalRegistryClient;
import it.pagopa.pn.deliverypush.service.IoService;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalregistry.model.SendMessageRequest;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalregistry.model.SendMessageResponse;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.time.Instant;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_ERRORCOURTESYIO;
import static it.pagopa.pn.deliverypush.generated.openapi.msclient.externalregistry.model.SendMessageResponse.ResultEnum.*;


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
    public SendMessageResponse.ResultEnum sendIOMessage(NotificationInt notification, int recIndex, Instant schedulingAnalogDate) {
        log.info("Start send message to App IO - iun={} id={}", notification.getIun(), recIndex);

        NotificationRecipientInt recipientInt = notificationUtils.getRecipientFromIndex(notification, recIndex);

        SendMessageRequest sendMessageRequest = getSendMessageRequest(notification, recipientInt, recIndex, schedulingAnalogDate);

        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEvent logEvent = auditLogBuilder.before(PnAuditLogEventType.AUD_DA_SEND_IO, "sendIOMessage - iun={} id={}", notification.getIun(), recIndex)
                .iun(sendMessageRequest.getIun())
                .build();
        logEvent.log();
        
        try {
          SendMessageResponse sendIoMessageResponse = pnExternalRegistryClient.sendIOMessage(sendMessageRequest);

          if(sendIoMessageResponse != null){
              if( isErrorStatus( sendIoMessageResponse.getResult() ) ){
                  logEvent.generateFailure("Error in sendIoMessage, with errorStatus={} - iun={} id={} ", sendIoMessageResponse.getResult(), notification.getIun(), recIndex).log();
                  throw new PnInternalException("Error in sendIoMessage, with errorStatus="+ sendIoMessageResponse.getResult() +" - iun="+ notification.getIun() +" id="+ recIndex, ERROR_CODE_DELIVERYPUSH_ERRORCOURTESYIO);
              } else {
                  logEvent.generateSuccess("Send io message success, with result={}", sendIoMessageResponse.getResult()).log();
                  return sendIoMessageResponse.getResult();
              }
          }else {
              logEvent.generateFailure("endIOMessage return not valid response response - iun={} id={} ", notification.getIun(), recIndex).log();
              throw new PnInternalException("sendIOMessage return not valid response response - iun="+ notification.getIun() +" id="+ recIndex, ERROR_CODE_DELIVERYPUSH_ERRORCOURTESYIO);
          }

        } catch (Exception ex){
            logEvent.generateFailure("Error in sendIoMessage", ex).log();
            throw ex;
        }
    }

    private boolean isErrorStatus(SendMessageResponse.ResultEnum result) {
        return ERROR_USER_STATUS.equals(result) || ERROR_COURTESY.equals(result) || ERROR_OPTIN.equals(result);
    }

    @NotNull
    private SendMessageRequest getSendMessageRequest(NotificationInt notification, NotificationRecipientInt recipientInt, int recIndex, Instant schedulingAnalogDate) {
        SendMessageRequest sendMessageRequest = new SendMessageRequest();
        sendMessageRequest.setAmount(notification.getAmount());
        sendMessageRequest.setDueDate(notification.getPaymentExpirationDate());
        sendMessageRequest.setRecipientTaxID(recipientInt.getTaxId());
        sendMessageRequest.setRequestAcceptedDate(notification.getSentAt());
        sendMessageRequest.setSenderDenomination(notification.getSender().getPaDenomination());
        sendMessageRequest.setIun(notification.getIun());
        sendMessageRequest.setRecipientIndex(recIndex);
        sendMessageRequest.setRecipientInternalID(recipientInt.getInternalId());
        sendMessageRequest.setSubject(notification.getSubject());
        sendMessageRequest.setSchedulingAnalogDate(schedulingAnalogDate);

        return sendMessageRequest;
    }
}
