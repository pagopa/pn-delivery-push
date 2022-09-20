package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.externalchannel;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.utils.LogUtils;
import it.pagopa.pn.delivery.generated.openapi.clients.externalchannel.ApiClient;
import it.pagopa.pn.delivery.generated.openapi.clients.externalchannel.api.DigitalCourtesyMessagesApi;
import it.pagopa.pn.delivery.generated.openapi.clients.externalchannel.api.DigitalLegalMessagesApi;
import it.pagopa.pn.delivery.generated.openapi.clients.externalchannel.api.PaperMessagesApi;
import it.pagopa.pn.delivery.generated.openapi.clients.externalchannel.model.DigitalCourtesyMailRequest;
import it.pagopa.pn.delivery.generated.openapi.clients.externalchannel.model.DigitalCourtesySmsRequest;
import it.pagopa.pn.delivery.generated.openapi.clients.externalchannel.model.DigitalNotificationRequest;
import it.pagopa.pn.delivery.generated.openapi.clients.externalchannel.model.PaperEngageRequest;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.address.CourtesyDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.legalfacts.LegalFactGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.*;

@Component
@Slf4j
public class ExternalChannelSendClientImpl implements ExternalChannelSendClient {

    private static final String EVENT_TYPE_LEGAL = "LEGAL";
    private static final String EVENT_TYPE_COURTESY = "COURTESY";


    public static final String PRINT_TYPE_BN_FRONTE_RETRO = "BN_FRONTE_RETRO";
    private final PnDeliveryPushConfigs cfg;
    private final RestTemplate restTemplate;
    private DigitalLegalMessagesApi digitalLegalMessagesApi;
    private DigitalCourtesyMessagesApi digitalCourtesyMessagesApi;
    private PaperMessagesApi paperMessagesApi;
    private final LegalFactGenerator legalFactGenerator;

    public ExternalChannelSendClientImpl(@Qualifier("withOffsetDateTimeFormatter") RestTemplate restTemplate, PnDeliveryPushConfigs cfg, LegalFactGenerator legalFactGenerator) {
        this.legalFactGenerator = legalFactGenerator;
        this.cfg = cfg;
        this.restTemplate = restTemplate;
    }

    @PostConstruct
    public void init(){
        this.digitalLegalMessagesApi = new DigitalLegalMessagesApi(newApiClient());
        this.digitalCourtesyMessagesApi = new DigitalCourtesyMessagesApi(newApiClient());
        this.paperMessagesApi = new PaperMessagesApi(newApiClient());
    }

    private ApiClient newApiClient()
    {

        ApiClient apiClient = new ApiClient(restTemplate);
        apiClient.setBasePath(cfg.getExternalChannelBaseUrl());
        return apiClient;
    }


    @Override
    public void sendAnalogNotification(NotificationInt notificationInt, NotificationRecipientInt recipientInt, PhysicalAddressInt physicalAddress, String timelineEventId, PhysicalAddressInt.ANALOG_TYPE analogType, String aarKey) {
        log.info("[enter] sendAnalogNotification address={} recipient={} requestId={} aarkey={}", LogUtils.maskGeneric(physicalAddress.getAddress()), LogUtils.maskGeneric(recipientInt.getDenomination()), timelineEventId, aarKey);

        PaperEngageRequest paperEngageRequest = new PaperEngageRequest();
        paperEngageRequest.setRequestId(timelineEventId);
        paperEngageRequest.setIun(notificationInt.getIun());
        paperEngageRequest.setProductType(getProductType(analogType, physicalAddress.getForeignState()));
        paperEngageRequest.setRequestPaId(notificationInt.getSender().getPaId());
        paperEngageRequest.setClientRequestTimeStamp(OffsetDateTime.now(ZoneOffset.UTC));
        paperEngageRequest.setPrintType(PRINT_TYPE_BN_FRONTE_RETRO);

        // nome e indirizzo destinatario
        paperEngageRequest.setReceiverName(recipientInt.getDenomination());

        paperEngageRequest.setReceiverAddress(physicalAddress.getAddress());
        paperEngageRequest.setReceiverAddressRow2(physicalAddress.getAddressDetails());
        paperEngageRequest.setReceiverCap(physicalAddress.getZip());
        paperEngageRequest.setReceiverCity(physicalAddress.getMunicipality());
        paperEngageRequest.setReceiverCity2(physicalAddress.getMunicipalityDetails());
        paperEngageRequest.setReceiverCountry(physicalAddress.getForeignState());
        paperEngageRequest.setReceiverPr(physicalAddress.getProvince());

        // uso la key recuperata dalla timeline la key riferita all'avviso AAR da spedire tramite raccomandata
        paperEngageRequest.setAttachmentUri(aarKey);

        paperMessagesApi.sendPaperEngageRequest(timelineEventId, cfg.getExternalchannelCxId(), paperEngageRequest);

        log.info("[exit] sendAnalogNotification address={} recipient={} requestId={} aarkey={}", LogUtils.maskGeneric(physicalAddress.getAddress()), LogUtils.maskGeneric(recipientInt.getDenomination()), timelineEventId, aarKey);
    }

    @Override
    public void sendLegalNotification(NotificationInt notificationInt, NotificationRecipientInt recipientInt, LegalDigitalAddressInt digitalAddress, String timelineEventId)
    {
        if (digitalAddress.getType() == LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
            sendNotificationPEC(timelineEventId, notificationInt, recipientInt, digitalAddress);
        else
            throw new PnInternalException("channel type not supported", ERROR_CODE_CHANNEL_TYPE_NOT_SUPPORTED);
    }

    @Override
    public void sendCourtesyNotification(NotificationInt notificationInt, NotificationRecipientInt recipientInt, CourtesyDigitalAddressInt digitalAddress, String timelineEventId)
    {
        if (digitalAddress.getType() == CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.EMAIL)
            sendNotificationEMAIL(timelineEventId, notificationInt, recipientInt, digitalAddress);
        else if (digitalAddress.getType() == CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.SMS)
            sendNotificationSMS(timelineEventId, notificationInt, recipientInt, digitalAddress);
        else
            throw new PnInternalException("channel type not supported", ERROR_CODE_CHANNEL_TYPE_NOT_SUPPORTED);
    }


    private void sendNotificationPEC(String requestId, NotificationInt notificationInt,  NotificationRecipientInt recipientInt, DigitalAddressInt digitalAddress)
    {
        try {
            log.info("[enter] sendNotificationPEC address={} requestId={} recipient={}", LogUtils.maskEmailAddress(digitalAddress.getAddress()), requestId, LogUtils.maskGeneric(recipientInt.getDenomination()));

            String mailbody = legalFactGenerator.generateNotificationAARPECBody(notificationInt, recipientInt);
            String mailsubj = legalFactGenerator.generateNotificationAARSubject(notificationInt);

            DigitalNotificationRequest digitalNotificationRequestDto = new DigitalNotificationRequest();
            digitalNotificationRequestDto.setChannel(DigitalNotificationRequest.ChannelEnum.PEC);
            digitalNotificationRequestDto.setRequestId(requestId);
            digitalNotificationRequestDto.setCorrelationId(requestId);
            digitalNotificationRequestDto.setEventType(EVENT_TYPE_LEGAL);
            digitalNotificationRequestDto.setMessageContentType(DigitalNotificationRequest.MessageContentTypeEnum.HTML);
            digitalNotificationRequestDto.setQos(DigitalNotificationRequest.QosEnum.BATCH);
            digitalNotificationRequestDto.setReceiverDigitalAddress(digitalAddress.getAddress());
            digitalNotificationRequestDto.setClientRequestTimeStamp(OffsetDateTime.now(ZoneOffset.UTC));
            digitalNotificationRequestDto.setMessageText(mailbody);
            digitalNotificationRequestDto.setSubjectText(mailsubj);
            digitalNotificationRequestDto.setAttachmentUrls(new ArrayList<>());
            if (StringUtils.hasText(cfg.getExternalchannelSenderPec()))
                digitalNotificationRequestDto.setSenderDigitalAddress(cfg.getExternalchannelSenderPec());

            digitalLegalMessagesApi.sendDigitalLegalMessage(requestId, cfg.getExternalchannelCxId(), digitalNotificationRequestDto);

            log.info("[exit] sendNotificationPEC address={} requestId={} recipient={}", LogUtils.maskEmailAddress(digitalAddress.getAddress()), requestId, LogUtils.maskGeneric(recipientInt.getDenomination()));
        } catch (Exception e) {
            throw new PnInternalException("error sending PEC notification", ERROR_CODE_SEND_PEC_NOTIFICATION_FAILED);
        }
    }

    private void sendNotificationEMAIL(String requestId, NotificationInt notificationInt, NotificationRecipientInt recipientInt, DigitalAddressInt digitalAddress)
    {
        try {
            log.info("[enter] sendNotificationEMAIL address={} requestId={} recipient={}", LogUtils.maskEmailAddress(digitalAddress.getAddress()), requestId, LogUtils.maskGeneric(recipientInt.getDenomination()));

            String mailbody = legalFactGenerator.generateNotificationAARBody(notificationInt, recipientInt);
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
            digitalNotificationRequestDto.setAttachmentUrls(new ArrayList<>());
            if (StringUtils.hasText(cfg.getExternalchannelSenderEmail()))
                digitalNotificationRequestDto.setSenderDigitalAddress(cfg.getExternalchannelSenderEmail());

            digitalCourtesyMessagesApi.sendDigitalCourtesyMessage(requestId, cfg.getExternalchannelCxId(), digitalNotificationRequestDto);

            log.info("[exit] sendNotificationEMAIL address={} requestId={} recipient={}", LogUtils.maskEmailAddress(digitalAddress.getAddress()), requestId, LogUtils.maskGeneric(recipientInt.getDenomination()));
        } catch (Exception e) {
            throw new PnInternalException("error sending EMAIL notification", ERROR_CODE_SEND_EMAIL_NOTIFICATION_FAILED);
        }
    }

    private void sendNotificationSMS(String requestId, NotificationInt notificationInt, NotificationRecipientInt recipientInt, DigitalAddressInt digitalAddress)
    {
        try {
            log.info("[enter] sendNotificationSMS address={} requestId={} recipient={}", LogUtils.maskNumber(digitalAddress.getAddress()), requestId, LogUtils.maskGeneric(recipientInt.getDenomination()));

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

            log.info("[exit] sendNotificationSMS address={} requestId={} recipient={}", LogUtils.maskNumber(digitalAddress.getAddress()), requestId, LogUtils.maskGeneric(recipientInt.getDenomination()));
        } catch (Exception e) {
            throw new PnInternalException("error sending SMS notification", ERROR_CODE_SEND_SMS_NOTIFICATION_FAILED);
        }
    }


    private String getProductType(PhysicalAddressInt.ANALOG_TYPE serviceLevelType, String country)
    {
        /*
          Tipo prodotto di cui viene chiesto il recapito:
          - AR: Raccomandata Andata e Ritorno,
          - 890: Recapito a norma della legge 890/1982,
          - RI: Raccomandata Internazionale,
          - RS: Raccomandata Semplice (per Avviso di mancato Recapito).
         */

        // la RI se il country è non vuoto e diverso da it
        if (StringUtils.hasText(country) && !country.trim().equalsIgnoreCase("it"))
        {
            return "RI";
        }

        switch (serviceLevelType){
            case REGISTERED_LETTER_890:
                return "890";
            case AR_REGISTERED_LETTER:
                return "AR";
            case SIMPLE_REGISTERED_LETTER:
                return "RS";
        }

        return  null;
    }

}


