package it.pagopa.pn.deliverypush.webhook;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
public class WebhookClientImpl implements WebhookClient {

    private final RestTemplate rest = new RestTemplate();

    public void sendInfo( String url, List<WebhookBufferRowDto> data ) {
        ResponseEntity<Map> resp = rest.postForEntity( url, data, Map.class);
        if( ! resp.getStatusCode().is2xxSuccessful() ) {
            throw new PnInternalException("Http error " + resp.getStatusCodeValue() + " calling webhook " + url );
        }
    }

}
