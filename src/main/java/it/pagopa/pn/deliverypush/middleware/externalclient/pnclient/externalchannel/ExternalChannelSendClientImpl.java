package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.externalchannel;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalchannel.ApiClient;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalchannel.api.DigitalCourtesyMessagesApi;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalchannel.api.DigitalLegalMessagesApi;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalchannel.model.DigitalCourtesyMailRequest;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalchannel.model.DigitalCourtesySmsRequest;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalchannel.model.DigitalNotificationRequest;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.address.CourtesyDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.legalfacts.LegalFactGenerator;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.*;

@Component
@CustomLog
public class ExternalChannelSendClientImpl implements ExternalChannelSendClient {

    private static final String EVENT_TYPE_LEGAL = "LEGAL";
    private static final String EVENT_TYPE_COURTESY = "COURTESY";


    public static final String PRINT_TYPE_BN_FRONTE_RETRO = "BN_FRONTE_RETRO";
    private final PnDeliveryPushConfigs cfg;
    private final RestTemplate restTemplate;
    private DigitalLegalMessagesApi digitalLegalMessagesApi;
    private DigitalCourtesyMessagesApi digitalCourtesyMessagesApi;
    private final LegalFactGenerator legalFactGenerator;

    public ExternalChannelSendClientImpl(@Qualifier("withOffsetDateTimeFormatter") RestTemplate restTemplate,
                                         PnDeliveryPushConfigs cfg,
                                         LegalFactGenerator legalFactGenerator) {
        this.legalFactGenerator = legalFactGenerator;
        this.cfg = cfg;
        this.restTemplate = restTemplate;
    }

    @PostConstruct
    public void init(){
        this.digitalLegalMessagesApi = new DigitalLegalMessagesApi(newApiClient());
        this.digitalCourtesyMessagesApi = new DigitalCourtesyMessagesApi(newApiClient());
    }

    private ApiClient newApiClient()
    {

        ApiClient apiClient = new ApiClient(restTemplate);
        apiClient.setBasePath(cfg.getExternalChannelBaseUrl());
        return apiClient;
    }


    @Override
    public void sendLegalNotification(NotificationInt notificationInt,
                                      NotificationRecipientInt recipientInt,
                                      LegalDigitalAddressInt digitalAddress,
                                      String timelineEventId,
                                      String aarKey,
                                      String quickAccessToken)
    {
        if (digitalAddress.getType() == LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC) {
            sendNotificationPEC(timelineEventId, notificationInt, recipientInt, digitalAddress,aarKey, quickAccessToken);
        } else {
            log.error("channel type not supported for iun={}", notificationInt.getIun());
            throw new PnInternalException("channel type not supported", ERROR_CODE_DELIVERYPUSH_CHANNELTYPENOTSUPPORTED);
        }
    }

    @Override
    public void sendCourtesyNotification(NotificationInt notificationInt,
                                         NotificationRecipientInt recipientInt,
                                         CourtesyDigitalAddressInt digitalAddress,
                                         String timelineEventId,
                                         String aarKey,
                                         String quickAccessToken)
    {
        if (digitalAddress.getType() == CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.EMAIL) {
            sendNotificationEMAIL(timelineEventId, notificationInt, recipientInt, digitalAddress, aarKey, quickAccessToken);
        } else if (digitalAddress.getType() == CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.SMS) {
            sendNotificationSMS(timelineEventId, notificationInt, digitalAddress);
        } else {
            log.error("channel type not supported for iun={}", notificationInt.getIun());
            throw new PnInternalException("channel type not supported", ERROR_CODE_DELIVERYPUSH_CHANNELTYPENOTSUPPORTED);
        }
    }


    private void sendNotificationPEC(String requestId,
                                     NotificationInt notificationInt,
                                     NotificationRecipientInt recipientInt,
                                     DigitalAddressInt digitalAddress,
                                     String aarKey,
                                     String quickAccessToken)
    {
        try {
            log.logInvokingAsyncExternalService(CLIENT_NAME, LEGAL_NOTIFICATION_REQUEST, requestId);
            
            String mailBody = legalFactGenerator.generateNotificationAARPECBody(notificationInt, recipientInt, quickAccessToken);
            String mailSubj = legalFactGenerator.generateNotificationAARSubject(notificationInt);

            DigitalNotificationRequest digitalNotificationRequestDto = new DigitalNotificationRequest();
            digitalNotificationRequestDto.setChannel(DigitalNotificationRequest.ChannelEnum.PEC);
            digitalNotificationRequestDto.setRequestId(requestId);
            digitalNotificationRequestDto.setCorrelationId(requestId);
            digitalNotificationRequestDto.setEventType(EVENT_TYPE_LEGAL);
            digitalNotificationRequestDto.setMessageContentType(DigitalNotificationRequest.MessageContentTypeEnum.HTML);
            digitalNotificationRequestDto.setQos(DigitalNotificationRequest.QosEnum.BATCH);
            digitalNotificationRequestDto.setReceiverDigitalAddress(digitalAddress.getAddress());
            digitalNotificationRequestDto.setClientRequestTimeStamp(OffsetDateTime.now(ZoneOffset.UTC));
            digitalNotificationRequestDto.setMessageText(mailBody);
            digitalNotificationRequestDto.setSubjectText(mailSubj);
            digitalNotificationRequestDto.setAttachmentUrls(List.of(aarKey));

            if (StringUtils.hasText(cfg.getExternalchannelSenderPec()))
                digitalNotificationRequestDto.setSenderDigitalAddress(cfg.getExternalchannelSenderPec());


            digitalLegalMessagesApi.sendDigitalLegalMessage(requestId, cfg.getExternalchannelCxId(), digitalNotificationRequestDto);
                    
        } catch (Exception e) {
            log.error("error sending PEC notification for iun={}", notificationInt.getIun());
            throw new PnInternalException("error sending PEC notification", ERROR_CODE_DELIVERYPUSH_SENDPECNOTIFICATIONFAILED, e);
        }
    }

    private void sendNotificationEMAIL(String requestId,
                                       NotificationInt notificationInt,
                                       NotificationRecipientInt recipientInt,
                                       DigitalAddressInt digitalAddress,
                                       String aarKey,
                                       String quickAccessToken)
    {
        try {
            log.logInvokingAsyncExternalService(CLIENT_NAME, COURTESY_NOTIFICATION_REQUEST + "[EMAIL]", requestId);

            String mailbody = legalFactGenerator.generateNotificationAARBody(notificationInt, recipientInt, quickAccessToken);
            String mailsubj = legalFactGenerator.generateNotificationAARSubject(notificationInt);

            DigitalCourtesyMailRequest digitalNotificationRequestDto = new DigitalCourtesyMailRequest();
            digitalNotificationRequestDto.setChannel(DigitalCourtesyMailRequest.ChannelEnum.EMAIL);
            digitalNotificationRequestDto.setRequestId(requestId);
            digitalNotificationRequestDto.setCorrelationId(requestId);
            digitalNotificationRequestDto.setEventType(EVENT_TYPE_COURTESY);
            digitalNotificationRequestDto.setQos(DigitalCourtesyMailRequest.QosEnum.BATCH);
            digitalNotificationRequestDto.setReceiverDigitalAddress(digitalAddress.getAddress());
            digitalNotificationRequestDto.setClientRequestTimeStamp(OffsetDateTime.now(ZoneOffset.UTC));
            digitalNotificationRequestDto.setMessageContentType(DigitalCourtesyMailRequest.MessageContentTypeEnum.HTML);
            digitalNotificationRequestDto.setMessageText(mailbody);
            digitalNotificationRequestDto.setSubjectText(mailsubj);
            digitalNotificationRequestDto.setAttachmentUrls(List.of(aarKey));
            if (StringUtils.hasText(cfg.getExternalchannelSenderEmail()))
                digitalNotificationRequestDto.setSenderDigitalAddress(cfg.getExternalchannelSenderEmail());

            digitalCourtesyMessagesApi.sendDigitalCourtesyMessage(requestId, cfg.getExternalchannelCxId(), digitalNotificationRequestDto);

        } catch (Exception e) {
            throw new PnInternalException("error sending EMAIL notification", ERROR_CODE_DELIVERYPUSH_SENDEMAILNOTIFICATIONFAILED);
        }
    }

    private void sendNotificationSMS(String requestId, NotificationInt notificationInt, DigitalAddressInt digitalAddress)
    {
        try {
            log.logInvokingAsyncExternalService(CLIENT_NAME, COURTESY_NOTIFICATION_REQUEST + "[PEC]", requestId);

            String smsbody = legalFactGenerator.generateNotificationAARForSMS(notificationInt);

            DigitalCourtesySmsRequest digitalNotificationRequestDto = new DigitalCourtesySmsRequest();
            digitalNotificationRequestDto.setChannel(DigitalCourtesySmsRequest.ChannelEnum.SMS);
            digitalNotificationRequestDto.setRequestId(requestId);
            digitalNotificationRequestDto.setCorrelationId(requestId);
            digitalNotificationRequestDto.setEventType(EVENT_TYPE_COURTESY);
            digitalNotificationRequestDto.setQos(DigitalCourtesySmsRequest.QosEnum.BATCH);
            digitalNotificationRequestDto.setReceiverDigitalAddress(digitalAddress.getAddress());
            digitalNotificationRequestDto.setClientRequestTimeStamp(OffsetDateTime.now(ZoneOffset.UTC));
            digitalNotificationRequestDto.setMessageText(smsbody);
            if (StringUtils.hasText(cfg.getExternalchannelSenderSms()))
                digitalNotificationRequestDto.setSenderDigitalAddress(cfg.getExternalchannelSenderSms());

            digitalCourtesyMessagesApi.sendCourtesyShortMessage(requestId, cfg.getExternalchannelCxId(), digitalNotificationRequestDto);

        } catch (Exception e) {
            throw new PnInternalException("error sending SMS notification", ERROR_CODE_DELIVERYPUSH_SENDSMSNOTIFICATIONFAILED, e);
        }
    }

}


