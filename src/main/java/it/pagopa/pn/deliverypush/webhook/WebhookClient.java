package it.pagopa.pn.deliverypush.webhook;

import java.util.List;

public interface WebhookClient {

    void sendInfo( String url, List<WebhookBufferRowDto> data );

}
