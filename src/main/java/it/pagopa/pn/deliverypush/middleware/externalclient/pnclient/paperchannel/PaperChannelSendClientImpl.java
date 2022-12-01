package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.paperchannel;

import it.pagopa.pn.commons.utils.LogUtils;
import it.pagopa.pn.delivery.generated.openapi.clients.paperchannel.ApiClient;
import it.pagopa.pn.delivery.generated.openapi.clients.paperchannel.api.PaperMessagesApi;
import it.pagopa.pn.delivery.generated.openapi.clients.paperchannel.model.AnalogAddress;
import it.pagopa.pn.delivery.generated.openapi.clients.paperchannel.model.PrepareRequest;
import it.pagopa.pn.delivery.generated.openapi.clients.paperchannel.model.SendRequest;
import it.pagopa.pn.delivery.generated.openapi.clients.paperchannel.model.SendResponse;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;

@Component
@Slf4j
public class PaperChannelSendClientImpl implements PaperChannelSendClient {


    public static final String PRINT_TYPE_BN_FRONTE_RETRO = "BN_FRONTE_RETRO";
    private final PnDeliveryPushConfigs cfg;
    private final RestTemplate restTemplate;
    private PaperMessagesApi paperMessagesApi;

    public PaperChannelSendClientImpl(@Qualifier("withOffsetDateTimeFormatter") RestTemplate restTemplate, PnDeliveryPushConfigs cfg) {
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
    public void prepare(PaperChannelPrepareRequest paperChannelPrepareRequest) {
        log.info("[enter] prepare iun={} address={} recipient={} requestId={} attachments={}", paperChannelPrepareRequest.getNotificationInt().getIun(), LogUtils.maskGeneric(paperChannelPrepareRequest.getPaAddress().getAddress()), LogUtils.maskGeneric(paperChannelPrepareRequest.getRecipientInt().getDenomination()), paperChannelPrepareRequest.getRequestId(), paperChannelPrepareRequest.getAttachments());

        PrepareRequest prepareRequest = new PrepareRequest();
        prepareRequest.setRequestId(paperChannelPrepareRequest.getRequestId());
        prepareRequest.setPrintType(PRINT_TYPE_BN_FRONTE_RETRO);
        prepareRequest.setPrintType(getProductType(paperChannelPrepareRequest.getAnalogType()));
        prepareRequest.setReceiverAddress(mapInternalToExternal(paperChannelPrepareRequest.getPaAddress()));
        prepareRequest.setAttachmentUrls(paperChannelPrepareRequest.getAttachments());
        prepareRequest.setReceiverFiscalCode(paperChannelPrepareRequest.getRecipientInt().getTaxId());
        prepareRequest.setReceiverType(paperChannelPrepareRequest.getRecipientInt().getRecipientType().getValue());

        prepareRequest.setRelatedRequestId(paperChannelPrepareRequest.getRelatedRequestId());
        prepareRequest.setDiscoveredAddress(mapInternalToExternal(paperChannelPrepareRequest.getDiscoveredAddress()));
        paperMessagesApi.sendPaperPrepareRequest(paperChannelPrepareRequest.getRequestId(), prepareRequest);

        log.info("[exit] prepare iun={}  address={} recipient={} requestId={} attachments={}", paperChannelPrepareRequest.getNotificationInt().getIun(), LogUtils.maskGeneric(paperChannelPrepareRequest.getPaAddress().getAddress()), LogUtils.maskGeneric(paperChannelPrepareRequest.getRecipientInt().getDenomination()), paperChannelPrepareRequest.getRequestId(), paperChannelPrepareRequest.getAttachments());
    }



    @Override
    public Integer send(PaperChannelSendRequest paperChannelSendRequest) {
        log.info("[enter] send iun={} address={} recipient={} requestId={} attachments={}", paperChannelSendRequest.getNotificationInt().getIun(), LogUtils.maskGeneric(paperChannelSendRequest.getReceiverAddress().getAddress()), LogUtils.maskGeneric(paperChannelSendRequest.getRecipientInt().getDenomination()), paperChannelSendRequest.getRequestId(), paperChannelSendRequest.getAttachments());

        SendRequest sendRequest = new SendRequest();
        sendRequest.setRequestId(paperChannelSendRequest.getRequestId());
        sendRequest.setPrintType(PRINT_TYPE_BN_FRONTE_RETRO);
        sendRequest.setPrintType(getProductType(paperChannelSendRequest.getAnalogType()));
        sendRequest.setReceiverAddress(mapInternalToExternal(paperChannelSendRequest.getReceiverAddress()));
        sendRequest.setAttachmentUrls(paperChannelSendRequest.getAttachments());
        sendRequest.setReceiverFiscalCode(paperChannelSendRequest.getRecipientInt().getTaxId());
        sendRequest.setReceiverType(paperChannelSendRequest.getRecipientInt().getRecipientType().getValue());
        sendRequest.setArAddress(mapInternalToExternal(paperChannelSendRequest.getArAddress()));
        sendRequest.setSenderAddress(mapInternalToExternal(paperChannelSendRequest.getSenderAddress()));
        sendRequest.setRequestPaId(paperChannelSendRequest.getNotificationInt().getSender().getPaTaxId());

        SendResponse response = paperMessagesApi.sendPaperSendRequest(paperChannelSendRequest.getRequestId(), sendRequest);

        log.info("[exit] send iun={} address={} recipient={} requestId={} attachments={} amount={}", paperChannelSendRequest.getNotificationInt().getIun(), LogUtils.maskGeneric(paperChannelSendRequest.getReceiverAddress().getAddress()), LogUtils.maskGeneric(paperChannelSendRequest.getRecipientInt().getDenomination()), paperChannelSendRequest.getRequestId(), paperChannelSendRequest.getAttachments(), response.getAmount());
        return response.getAmount();
    }

    private AnalogAddress mapInternalToExternal(PhysicalAddressInt physicalAddress){
        if (physicalAddress == null)
            return null;

        AnalogAddress analogAddress = new AnalogAddress();
        analogAddress.setFullname(physicalAddress.getFullname());
        analogAddress.setNameRow2(physicalAddress.getAt());
        analogAddress.setAddress(physicalAddress.getAddress());
        analogAddress.setAddressRow2(physicalAddress.getAddressDetails());
        analogAddress.setCap(physicalAddress.getZip());
        analogAddress.setCity(physicalAddress.getMunicipality());
        analogAddress.setCity2(physicalAddress.getMunicipalityDetails());
        analogAddress.setCountry(physicalAddress.getForeignState());
        analogAddress.setPr(physicalAddress.getProvince());
        return analogAddress;
    }

    private String getProductType(PhysicalAddressInt.ANALOG_TYPE serviceLevelType)
    {
        /*
          Tipo prodotto di cui viene chiesto il recapito:
          - AR: Raccomandata Andata e Ritorno,
          - 890: Recapito a norma della legge 890/1982,
          - RS: Raccomandata Semplice (per Avviso di mancato Recapito).
         */


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


