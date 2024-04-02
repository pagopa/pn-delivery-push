package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.paperchannel;

import it.pagopa.pn.commons.exceptions.PnHttpResponseException;
import it.pagopa.pn.commons.utils.LogUtils;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.exceptions.PnPaperChannelChangedCostException;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.paperchannel.api.PaperMessagesApi;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.paperchannel.model.*;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@CustomLog
public class PaperChannelSendClientImpl implements PaperChannelSendClient {
    public static final String PRINT_TYPE_BN_FRONTE_RETRO = "BN_FRONTE_RETRO";
    private final PaperMessagesApi paperMessagesApi;
    
    @Override
    public void prepare(PaperChannelPrepareRequest paperChannelPrepareRequest) {
        log.logInvokingAsyncExternalService(CLIENT_NAME, PREPARE_ANALOG_NOTIFICATION, paperChannelPrepareRequest.getRequestId());
        log.debug("[enter] prepare iun={} address={} recipient={} requestId={} attachments={} relatedRequestId={}", paperChannelPrepareRequest.getNotificationInt().getIun(), LogUtils.maskGeneric(paperChannelPrepareRequest.getPaAddress()==null?"null":paperChannelPrepareRequest.getPaAddress().getAddress()), LogUtils.maskGeneric(paperChannelPrepareRequest.getRecipientInt().getDenomination()), paperChannelPrepareRequest.getRequestId(), paperChannelPrepareRequest.getAttachments(), paperChannelPrepareRequest.getRelatedRequestId());

        PrepareRequest prepareRequest = new PrepareRequest();
        prepareRequest.setRequestId(paperChannelPrepareRequest.getRequestId());
        prepareRequest.setIun(paperChannelPrepareRequest.getNotificationInt().getIun());
        prepareRequest.setPrintType(PRINT_TYPE_BN_FRONTE_RETRO);
        prepareRequest.setProposalProductType(getProductType(paperChannelPrepareRequest.getAnalogType()));
        prepareRequest.setReceiverAddress(mapInternalToExternal(paperChannelPrepareRequest.getPaAddress()));
        prepareRequest.setAttachmentUrls(paperChannelPrepareRequest.getAttachments());
        prepareRequest.setReceiverFiscalCode(paperChannelPrepareRequest.getRecipientInt().getTaxId());
        prepareRequest.setReceiverType(paperChannelPrepareRequest.getRecipientInt().getRecipientType().getValue());
        prepareRequest.setNotificationSentAt(paperChannelPrepareRequest.getNotificationInt().getSentAt());

        prepareRequest.setRelatedRequestId(paperChannelPrepareRequest.getRelatedRequestId());
        prepareRequest.setDiscoveredAddress(mapInternalToExternal(paperChannelPrepareRequest.getDiscoveredAddress()));

        paperMessagesApi.sendPaperPrepareRequest(paperChannelPrepareRequest.getRequestId(), prepareRequest);

        log.debug("[exit] prepare iun={}  address={} recipient={} requestId={} attachments={} relatedRequestId={}", paperChannelPrepareRequest.getNotificationInt().getIun(), LogUtils.maskGeneric(paperChannelPrepareRequest.getPaAddress()==null?"null":paperChannelPrepareRequest.getPaAddress().getAddress()), LogUtils.maskGeneric(paperChannelPrepareRequest.getRecipientInt().getDenomination()), paperChannelPrepareRequest.getRequestId(), paperChannelPrepareRequest.getAttachments(), paperChannelPrepareRequest.getRelatedRequestId());
    }

    @Override
    public SendResponse send(PaperChannelSendRequest paperChannelSendRequest) {
        try {
            log.logInvokingAsyncExternalService(CLIENT_NAME, SEND_ANALOG_NOTIFICATION, paperChannelSendRequest.getRequestId());
            log.debug("[enter] send iun={} address={} recipient={} requestId={} attachments={}", paperChannelSendRequest.getNotificationInt().getIun(), LogUtils.maskGeneric(paperChannelSendRequest.getReceiverAddress().getAddress()), LogUtils.maskGeneric(paperChannelSendRequest.getRecipientInt().getDenomination()), paperChannelSendRequest.getRequestId(), paperChannelSendRequest.getAttachments());

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
            sendRequest.setClientRequestTimeStamp(Instant.now());

            SendResponse response = paperMessagesApi.sendPaperSendRequest(paperChannelSendRequest.getRequestId(), sendRequest);
            log.debug("[exit] send iun={} address={} recipient={} requestId={} attachments={} amount={}", paperChannelSendRequest.getNotificationInt().getIun(), LogUtils.maskGeneric(paperChannelSendRequest.getReceiverAddress().getAddress()), LogUtils.maskGeneric(paperChannelSendRequest.getRecipientInt().getDenomination()), paperChannelSendRequest.getRequestId(), paperChannelSendRequest.getAttachments(), response.getAmount());
            return response;
        } catch (PnHttpResponseException e) {
            if (e.getStatusCode() == HttpStatus.UNPROCESSABLE_ENTITY.value()) {
                log.error("received unprocessable from paper-channel, it means that send cost is different from prepare, and need to recompute prepare", e);
                throw new PnPaperChannelChangedCostException(e);
            } else {
              throw e;
            }
        }
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


