package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.paperchannel;

import it.pagopa.pn.deliverypush.generated.openapi.msclient.paperchannel.ApiClient;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.paperchannel.api.PaperMessagesApi;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.paperchannel.model.*;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.time.OffsetDateTime;

@Component
@CustomLog
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
        log.logInvokingAsyncExternalService(CLIENT_NAME, PREPARE_ANALOG_NOTIFICATION, paperChannelPrepareRequest.getRequestId());
        
        PrepareRequest prepareRequest = new PrepareRequest();
        prepareRequest.setRequestId(paperChannelPrepareRequest.getRequestId());
        prepareRequest.setIun(paperChannelPrepareRequest.getNotificationInt().getIun());
        prepareRequest.setPrintType(PRINT_TYPE_BN_FRONTE_RETRO);
        prepareRequest.setProposalProductType(getProductType(paperChannelPrepareRequest.getAnalogType()));
        prepareRequest.setReceiverAddress(mapInternalToExternal(paperChannelPrepareRequest.getPaAddress()));
        prepareRequest.setAttachmentUrls(paperChannelPrepareRequest.getAttachments());
        prepareRequest.setReceiverFiscalCode(paperChannelPrepareRequest.getRecipientInt().getTaxId());
        prepareRequest.setReceiverType(paperChannelPrepareRequest.getRecipientInt().getRecipientType().getValue());

        prepareRequest.setRelatedRequestId(paperChannelPrepareRequest.getRelatedRequestId());
        prepareRequest.setDiscoveredAddress(mapInternalToExternal(paperChannelPrepareRequest.getDiscoveredAddress()));

        paperMessagesApi.sendPaperPrepareRequest(paperChannelPrepareRequest.getRequestId(), prepareRequest);
    }

    @Override
    public SendResponse send(PaperChannelSendRequest paperChannelSendRequest) {
        log.logInvokingAsyncExternalService(CLIENT_NAME, SEND_ANALOG_NOTIFICATION, paperChannelSendRequest.getRequestId());

        SendRequest sendRequest = new SendRequest();
        sendRequest.setIun(paperChannelSendRequest.getNotificationInt().getIun());
        sendRequest.setRequestId(paperChannelSendRequest.getRequestId());
        sendRequest.setPrintType(PRINT_TYPE_BN_FRONTE_RETRO);
        sendRequest.setProductType(ProductTypeEnum.fromValue(paperChannelSendRequest.getProductType()));
        sendRequest.setReceiverAddress(mapInternalToExternal(paperChannelSendRequest.getReceiverAddress()));
        sendRequest.setAttachmentUrls(paperChannelSendRequest.getAttachments());
        sendRequest.setReceiverFiscalCode(paperChannelSendRequest.getRecipientInt().getTaxId());
        sendRequest.setReceiverType(paperChannelSendRequest.getRecipientInt().getRecipientType().getValue());
        sendRequest.setArAddress(mapInternalToExternal(paperChannelSendRequest.getArAddress()));
        sendRequest.setSenderAddress(mapInternalToExternal(paperChannelSendRequest.getSenderAddress()));
        sendRequest.setRequestPaId(paperChannelSendRequest.getNotificationInt().getSender().getPaTaxId());
        sendRequest.setClientRequestTimeStamp(OffsetDateTime.now());


        return paperMessagesApi.sendPaperSendRequest(paperChannelSendRequest.getRequestId(), sendRequest);

        
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

    private ProposalTypeEnum getProductType(PhysicalAddressInt.ANALOG_TYPE serviceLevelType)
    {
        /*
          Tipo prodotto di cui viene chiesto il recapito:
          - AR: Raccomandata Andata e Ritorno,
          - 890: Recapito a norma della legge 890/1982,
          - RS: Raccomandata Semplice (per Avviso di mancato Recapito).
         */


        return switch (serviceLevelType) {
            case REGISTERED_LETTER_890 -> ProposalTypeEnum._890;
            case AR_REGISTERED_LETTER -> ProposalTypeEnum.AR;
            case SIMPLE_REGISTERED_LETTER -> ProposalTypeEnum.RS;
        };

    }

}


