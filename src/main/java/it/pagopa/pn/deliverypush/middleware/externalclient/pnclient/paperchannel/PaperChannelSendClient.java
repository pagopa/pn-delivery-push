package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.paperchannel;


public interface PaperChannelSendClient {

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
    Integer send(PaperChannelSendRequest paperChannelSendRequest);

}
