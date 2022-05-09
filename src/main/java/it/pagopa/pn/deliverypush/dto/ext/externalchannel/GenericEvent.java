package it.pagopa.pn.deliverypush.dto.ext.externalchannel;


public interface GenericEvent<H extends StandardEventHeader, P> {

    H getHeader();

    P getPayload();
}
