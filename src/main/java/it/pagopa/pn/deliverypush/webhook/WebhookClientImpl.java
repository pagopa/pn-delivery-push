package it.pagopa.pn.deliverypush.webhook;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Component
public class WebhookClientImpl implements WebhookClient {

    private final RestTemplate rest = new RestTemplate();

    public void sendInfo(String url, List<WebhookBufferRowDto> data) {
        log.info("Send info webhook with url: " + url);
        HttpEntity<List<WebhookBufferRowDto>> entity = new HttpEntity<>(data, null);
        ResponseEntity<Void> resp = rest.exchange(url, HttpMethod.POST, entity, Void.class);
        log.info("Response: " + resp);
        if (!resp.getStatusCode().is2xxSuccessful()) {
            throw new PnInternalException("Http error " + resp.getStatusCodeValue() + " calling webhook " + url);
        }
    }

}
