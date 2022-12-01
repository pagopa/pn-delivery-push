package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.externalchannel.paperchannel;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.utils.LogUtils;
import it.pagopa.pn.delivery.generated.openapi.clients.paperchannel.ApiClient;
import it.pagopa.pn.delivery.generated.openapi.clients.paperchannel.api.PaperMessagesApi;
import it.pagopa.pn.delivery.generated.openapi.clients.paperchannel.model.PrepareRequest;
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
public class PaperChannelSendClientImpl implements PaperChannelSendClient {


    public static final String PRINT_TYPE_BN_FRONTE_RETRO = "BN_FRONTE_RETRO";
    private final PnDeliveryPushConfigs cfg;
    private final RestTemplate restTemplate;
    private PaperMessagesApi paperMessagesApi;
    private final LegalFactGenerator legalFactGenerator;

    public PaperChannelSendClientImpl(@Qualifier("withOffsetDateTimeFormatter") RestTemplate restTemplate, PnDeliveryPushConfigs cfg, LegalFactGenerator legalFactGenerator) {
        this.legalFactGenerator = legalFactGenerator;
        this.cfg = cfg;
        this.restTemplate = restTemplate;
    }

    @PostConstruct
    public void init(){
        this.paperMessagesApi = new PaperMessagesApi(newApiClient());
    }

    private ApiClient newApiClient()
    {

        ApiClient apiClient = new ApiClient(restTemplate);
        apiClient.setBasePath(cfg.getPaperChannelBaseUrl());
        return apiClient;
    }


    @Override
    public void sendPrepare(NotificationInt notificationInt, NotificationRecipientInt recipientInt, PhysicalAddressInt physicalAddress, String timelineEventId, PhysicalAddressInt.ANALOG_TYPE analogType, String aarKey) {
        log.info("[enter] sendAnalogNotification address={} recipient={} requestId={} aarkey={}", LogUtils.maskGeneric(physicalAddress.getAddress()), LogUtils.maskGeneric(recipientInt.getDenomination()), timelineEventId, aarKey);

        PrepareRequest prepareRequest = new PrepareRequest();
        prepareRequest.setRequestId(timelineEventId);
        prepareRequest.setPrintType(PRINT_TYPE_BN_FRONTE_RETRO);
        prepareRequest.setPrintType(getProductType(analogType, physicalAddress.getForeignState()));
        prepareRequest.setReceiverAddress();

        PaperEngageRequest paperEngageRequest = new PaperEngageRequest();
        paperEngageRequest.setRequestId(timelineEventId);
        paperEngageRequest.setIun(notificationInt.getIun());
        paperEngageRequest.setProductType();
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


