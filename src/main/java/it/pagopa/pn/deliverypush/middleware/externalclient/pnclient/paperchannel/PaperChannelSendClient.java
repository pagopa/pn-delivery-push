package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.paperchannel;


import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.paperchannel.model.SendResponse;

public interface PaperChannelSendClient {
    String CLIENT_NAME = PnLogger.EXTERNAL_SERVICES.PN_PAPER_CHANNEL;
    String PREPARE_ANALOG_NOTIFICATION = "PREPARE ANALOG NOTIFICATION";
    String SEND_ANALOG_NOTIFICATION = "SEND ANALOG NOTIFICATION";


    /**
     * Esegue la prepare dell'invio di una notifica cartacea. La prepare è sempre asincrona.
     *
     * @param paperChannelPrepareRequest
     */
    void prepare(PaperChannelPrepareRequest paperChannelPrepareRequest);

    /**
     * Esegue l'invio della notifica cartacea, in base ad un requestId precedentemente preparato.
     * Richiede di ripassare tutti i parametri
     *
     *
     * @param paperChannelSendRequest@return ritorna il costo dell'invio in euro-cent
     */
    SendResponse send(PaperChannelSendRequest paperChannelSendRequest);

}
