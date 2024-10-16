package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.externalchannel;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.utils.LogUtils;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.address.CourtesyDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalchannel.api.DigitalCourtesyMessagesApi;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalchannel.api.DigitalLegalMessagesApi;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalchannel.model.DigitalCourtesyMailRequest;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalchannel.model.DigitalCourtesySmsRequest;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalchannel.model.DigitalNotificationRequest;
import it.pagopa.pn.deliverypush.legalfacts.LegalFactGenerator;
import it.pagopa.pn.deliverypush.service.utils.FileUtils;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.*;

@Component
@RequiredArgsConstructor
@CustomLog
public class ExternalChannelSendClientImpl implements ExternalChannelSendClient {

    private static final String EVENT_TYPE_LEGAL = "LEGAL";
    private static final String EVENT_TYPE_COURTESY = "COURTESY";

    private final PnDeliveryPushConfigs cfg;
    private final DigitalLegalMessagesApi digitalLegalMessagesApi;
    private final DigitalCourtesyMessagesApi digitalCourtesyMessagesApi;
    private final LegalFactGenerator legalFactGenerator;

    @Override
    public void sendLegalNotification(NotificationInt notificationInt,
                                      NotificationRecipientInt recipientInt,
                                      LegalDigitalAddressInt digitalAddress,
                                      String timelineEventId,
                                      List<String> fileKeys,
                                      String quickAccessToken)
    {
        if (digitalAddress.getType() == LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC || digitalAddress.getType() == LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.SERCQ) {
            sendNotificationPECOrSERCQ(timelineEventId, notificationInt, recipientInt, digitalAddress,fileKeys, quickAccessToken);
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


    private void sendNotificationPECOrSERCQ(String requestId,
                                            NotificationInt notificationInt,
                                            NotificationRecipientInt recipientInt,
                                            LegalDigitalAddressInt digitalAddress,
                                            List<String> fileKeys,
                                            String quickAccessToken)
    {
        boolean isPec = digitalAddress.getType().equals(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC);
        try {
            log.logInvokingAsyncExternalService(CLIENT_NAME, LEGAL_NOTIFICATION_REQUEST, requestId);

            String opLog = isPec ? "sendNotificationPEC" : "sendNotificationSERCQ";
            String maskedAddress = isPec ? LogUtils.maskEmailAddress(digitalAddress.getAddress()) : LogUtils.maskGeneric(digitalAddress.getAddress());
            log.debug("[enter] {} address={} requestId={} recipient={}", opLog, maskedAddress, requestId, LogUtils.maskGeneric(recipientInt.getDenomination()));

            String mailBody = legalFactGenerator.generateNotificationAARPECBody(notificationInt, recipientInt, quickAccessToken);
            String mailSubj = legalFactGenerator.generateNotificationAARSubject(notificationInt);
            List<String> fileKeysWithStoragePrefix = fileKeys.stream().map(FileUtils::getKeyWithStoragePrefix).toList();

            DigitalNotificationRequest digitalNotificationRequestDto = new DigitalNotificationRequest();
            digitalNotificationRequestDto.setChannel(digitalAddress.getType() == LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC ? DigitalNotificationRequest.ChannelEnum.PEC : DigitalNotificationRequest.ChannelEnum.SERCQ);
            digitalNotificationRequestDto.setRequestId(requestId);
            digitalNotificationRequestDto.setCorrelationId(requestId);
            digitalNotificationRequestDto.setEventType(EVENT_TYPE_LEGAL);
            digitalNotificationRequestDto.setMessageContentType(DigitalNotificationRequest.MessageContentTypeEnum.HTML);
            digitalNotificationRequestDto.setQos(DigitalNotificationRequest.QosEnum.BATCH);
            digitalNotificationRequestDto.setReceiverDigitalAddress(digitalAddress.getAddress());
            digitalNotificationRequestDto.setClientRequestTimeStamp(OffsetDateTime.now(ZoneOffset.UTC));
            digitalNotificationRequestDto.setMessageText(mailBody);
            digitalNotificationRequestDto.setSubjectText(mailSubj);
            digitalNotificationRequestDto.setAttachmentUrls(fileKeysWithStoragePrefix);

            if (StringUtils.hasText(cfg.getExternalchannelSenderPec()))
                digitalNotificationRequestDto.setSenderDigitalAddress(cfg.getExternalchannelSenderPec());


            digitalLegalMessagesApi.sendDigitalLegalMessage(requestId, cfg.getExternalchannelCxId(), digitalNotificationRequestDto);

            log.debug("[exit] {} address={} requestId={} recipient={}", opLog, maskedAddress, requestId, LogUtils.maskGeneric(recipientInt.getDenomination()));
        } catch (Exception e) {
            String channelName = isPec ? "PEC" : "SERCQ";
            log.error("error sending {} notification for iun={}", channelName, notificationInt.getIun());
            throw new PnInternalException(String.format("error sending %s notification", channelName), ERROR_CODE_DELIVERYPUSH_SENDPECNOTIFICATIONFAILED, e);
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
            log.debug("[enter] sendNotificationSMS address={} requestId={} recipient={}", LogUtils.maskNumber(digitalAddress.getAddress()), requestId, LogUtils.maskGeneric(recipientInt.getDenomination()));

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
            digitalNotificationRequestDto.setAttachmentUrls(List.of(FileUtils.getKeyWithStoragePrefix(aarKey)));
            if (StringUtils.hasText(cfg.getExternalchannelSenderEmail()))
                digitalNotificationRequestDto.setSenderDigitalAddress(cfg.getExternalchannelSenderEmail());

            digitalCourtesyMessagesApi.sendDigitalCourtesyMessage(requestId, cfg.getExternalchannelCxId(), digitalNotificationRequestDto);

            log.debug("[exit] sendNotificationEMAIL address={} requestId={} recipient={}", LogUtils.maskEmailAddress(digitalAddress.getAddress()), requestId, LogUtils.maskGeneric(recipientInt.getDenomination()));
        } catch (Exception e) {
            throw new PnInternalException("error sending EMAIL notification", ERROR_CODE_DELIVERYPUSH_SENDEMAILNOTIFICATIONFAILED);
        }
    }

    private void sendNotificationSMS(String requestId, NotificationInt notificationInt, DigitalAddressInt digitalAddress)
    {
        try {
            log.logInvokingAsyncExternalService(CLIENT_NAME, COURTESY_NOTIFICATION_REQUEST + "[SMS]", requestId);

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


