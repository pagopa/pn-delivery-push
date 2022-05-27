package it.pagopa.pn.deliverypush.externalclient.pnclient.externalchannel;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.generated.openapi.clients.externalchannel.ApiClient;
import it.pagopa.pn.delivery.generated.openapi.clients.externalchannel.api.DigitalCourtesyMessagesApi;
import it.pagopa.pn.delivery.generated.openapi.clients.externalchannel.api.DigitalLegalMessagesApi;
import it.pagopa.pn.delivery.generated.openapi.clients.externalchannel.api.PaperMessagesApi;
import it.pagopa.pn.delivery.generated.openapi.clients.externalchannel.model.DigitalCourtesyMailRequest;
import it.pagopa.pn.delivery.generated.openapi.clients.externalchannel.model.DigitalCourtesySmsRequest;
import it.pagopa.pn.delivery.generated.openapi.clients.externalchannel.model.DigitalNotificationRequest;
import it.pagopa.pn.delivery.generated.openapi.clients.externalchannel.model.PaperEngageRequest;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.*;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.PhysicalAddress;
import it.pagopa.pn.deliverypush.legalfacts.LegalFactGenerator;
import it.pagopa.pn.deliverypush.util.LogUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;

@Component
@Slf4j
public class ExternalChannelSendClientImpl implements ExternalChannelSendClient {

    private static final String EVENT_TYPE_LEGAL = "LEGAL";
    private static final String EVENT_TYPE_COURTESY = "COURTESY";


    public static final String PRINT_TYPE_BN_FRONTE_RETRO = "BN_FRONTE_RETRO";
    private final PnDeliveryPushConfigs cfg;
    private final DigitalLegalMessagesApi digitalLegalMessagesApi;
    private final DigitalCourtesyMessagesApi digitalCourtesyMessagesApi;
    private final PaperMessagesApi paperMessagesApi;
    private final LegalFactGenerator legalFactGenerator;

    public ExternalChannelSendClientImpl(@Qualifier("withTracing") RestTemplate restTemplate, PnDeliveryPushConfigs cfg, LegalFactGenerator legalFactGenerator) {
        this.legalFactGenerator = legalFactGenerator;

        ApiClient apiClient = new ApiClient(restTemplate);
        apiClient.setBasePath(cfg.getExternalChannelBaseUrl());

        this.digitalLegalMessagesApi = new DigitalLegalMessagesApi(apiClient);

        apiClient = new ApiClient(restTemplate);
        apiClient.setBasePath(cfg.getExternalChannelBaseUrl());

        this.digitalCourtesyMessagesApi = new DigitalCourtesyMessagesApi(apiClient);

        apiClient = new ApiClient(restTemplate);
        apiClient.setBasePath(cfg.getExternalChannelBaseUrl());

        this.paperMessagesApi = new PaperMessagesApi(apiClient);


        this.cfg = cfg;
    }

    @Override
    public void sendAnalogNotification(NotificationInt notificationInt, NotificationRecipientInt recipientInt, PhysicalAddress physicalAddress, String timelineEventId, ANALOG_TYPE analogType, String aarKey) {

        PaperEngageRequest paperEngageRequest = new PaperEngageRequest();
        paperEngageRequest.setRequestId(timelineEventId);
        paperEngageRequest.setIun(notificationInt.getIun());
        paperEngageRequest.setProductType(getProductType(analogType));
        paperEngageRequest.setRequestPaId(notificationInt.getSender().getPaId());
        paperEngageRequest.setClientRequestTimeStamp(OffsetDateTime.now(ZoneOffset.UTC).toInstant());
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
        paperEngageRequest.setAttachmentUri("safestorage://" + aarKey);

        paperMessagesApi.sendPaperEngageRequest(timelineEventId, cfg.getExternalchannelCxId(), paperEngageRequest);
    }

    @Override
    public void sendLegalNotification(NotificationInt notificationInt, LegalDigitalAddressInt digitalAddress, String timelineEventId)
    {
        if (digitalAddress.getType() == LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
            sendNotificationPEC(timelineEventId, notificationInt, digitalAddress);
        else
            throw new PnInternalException("channel type not supported");
    }

    @Override
    public void sendCourtesyNotification(NotificationInt notificationInt, CourtesyDigitalAddressInt digitalAddress, String timelineEventId)
    {
        if (digitalAddress.getType() == CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE.EMAIL)
            sendNotificationEMAIL(timelineEventId, notificationInt, digitalAddress);
        else if (digitalAddress.getType() == CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE.SMS)
            sendNotificationSMS(timelineEventId, notificationInt, digitalAddress);
        else
            throw new PnInternalException("channel type not supported");
    }


    private void sendNotificationPEC(String requestId, NotificationInt notificationInt, DigitalAddressInt digitalAddress)
    {
        try {
            log.info("sendNotificationPEC address:{} requestId:{}", LogUtils.maskEmailAddress(digitalAddress.getAddress()), requestId);

            String mailbody = legalFactGenerator.generateNotificationAARBody(notificationInt);
            String mailsubj = legalFactGenerator.generateNotificationAARSubject(notificationInt);

            DigitalNotificationRequest digitalNotificationRequestDto = new DigitalNotificationRequest();
            digitalNotificationRequestDto.setChannel(DigitalNotificationRequest.ChannelEnum.PEC);
            digitalNotificationRequestDto.setRequestId(requestId);
            digitalNotificationRequestDto.setEventType(EVENT_TYPE_LEGAL);
            digitalNotificationRequestDto.setMessageContentType(DigitalNotificationRequest.MessageContentTypeEnum.HTML);
            digitalNotificationRequestDto.setQos(DigitalNotificationRequest.QosEnum.BATCH);
            digitalNotificationRequestDto.setReceiverDigitalAddress(digitalAddress.getAddress());
            digitalNotificationRequestDto.setClientRequestTimeStamp(OffsetDateTime.now(ZoneOffset.UTC).toInstant());
            digitalNotificationRequestDto.setMessageText(mailbody);
            digitalNotificationRequestDto.setSubjectText(mailsubj);
            digitalNotificationRequestDto.setAttachmentUrls(new ArrayList<>());

            digitalLegalMessagesApi.sendDigitalLegalMessage(requestId, cfg.getExternalchannelCxId(), digitalNotificationRequestDto);
        } catch (Exception e) {
            throw new PnInternalException("error sending PEC notification", e);
        }
    }

    private void sendNotificationEMAIL(String requestId, NotificationInt notificationInt, DigitalAddressInt digitalAddress)
    {
        try {
            log.info("sendNotificationEMAIL address:{} requestId:{}", LogUtils.maskEmailAddress(digitalAddress.getAddress()), requestId);

            String mailbody = legalFactGenerator.generateNotificationAARBody(notificationInt);
            String mailsubj = legalFactGenerator.generateNotificationAARSubject(notificationInt);

            DigitalCourtesyMailRequest digitalNotificationRequestDto = new DigitalCourtesyMailRequest();
            digitalNotificationRequestDto.setChannel(DigitalCourtesyMailRequest.ChannelEnum.EMAIL);
            digitalNotificationRequestDto.setRequestId(requestId);
            digitalNotificationRequestDto.setEventType(EVENT_TYPE_COURTESY);
            digitalNotificationRequestDto.setQos(DigitalCourtesyMailRequest.QosEnum.BATCH);
            digitalNotificationRequestDto.setReceiverDigitalAddress(digitalAddress.getAddress());
            digitalNotificationRequestDto.setClientRequestTimeStamp(OffsetDateTime.now(ZoneOffset.UTC).toInstant());
            digitalNotificationRequestDto.setMessageContentType(DigitalCourtesyMailRequest.MessageContentTypeEnum.HTML);
            digitalNotificationRequestDto.setMessageText(mailbody);
            digitalNotificationRequestDto.setSubjectText(mailsubj);
            digitalNotificationRequestDto.setAttachmentUrls(new ArrayList<>());

            digitalCourtesyMessagesApi.sendDigitalCourtesyMessage(requestId, cfg.getExternalchannelCxId(), digitalNotificationRequestDto);
        } catch (Exception e) {
            throw new PnInternalException("error sending EMAIL notification", e);
        }
    }

    private void sendNotificationSMS(String requestId, NotificationInt notificationInt, DigitalAddressInt digitalAddress)
    {
        try {
            log.info("sendNotificationSMS address:{} requestId:{}", LogUtils.maskNumber(digitalAddress.getAddress()), requestId);

            String smsbody = legalFactGenerator.generateNotificationAARForSMS(notificationInt);

            DigitalCourtesySmsRequest digitalNotificationRequestDto = new DigitalCourtesySmsRequest();
            digitalNotificationRequestDto.setChannel(DigitalCourtesySmsRequest.ChannelEnum.SMS);
            digitalNotificationRequestDto.setRequestId(requestId);
            digitalNotificationRequestDto.setEventType(EVENT_TYPE_COURTESY);
            digitalNotificationRequestDto.setQos(DigitalCourtesySmsRequest.QosEnum.BATCH);
            digitalNotificationRequestDto.setReceiverDigitalAddress(digitalAddress.getAddress());
            digitalNotificationRequestDto.setClientRequestTimeStamp(OffsetDateTime.now(ZoneOffset.UTC).toInstant());
            digitalNotificationRequestDto.setMessageText(smsbody);
            digitalCourtesyMessagesApi.sendCourtesyShortMessage(requestId, cfg.getExternalchannelCxId(), digitalNotificationRequestDto);
        } catch (Exception e) {
            throw new PnInternalException("error sending SMS notification", e);
        }
    }


    private String getProductType(ANALOG_TYPE serviceLevelType)
    {
        /*
          Tipo prodotto di cui viene chiesto il recapito:
          - AR: Raccomandata Andata e Ritorno,
          - 890: Recapito a norma della legge 890/1982,
          - RI: Raccomandata Internazionale,
          - RS: Raccomandata Semplice (per Avviso di mancato Recapito).
         */
        switch (serviceLevelType){
            case REGISTERED_LETTER_890:
                return "890";
            case AR_REGISTERED_LETTER:
                return "AR";
            case SIMPLE_REGISTERED_LETTER:
                return "RS";
            // FIXME: gestire il caso di RI
        }

        return  null;
    }

}


