package it.pagopa.pn.deliverypush.webhook;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.webhook.dto.WebhookOutputDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Component
public class WebhookClientCertImpl implements WebhookClient{


    private final RestTemplate restTemplate;

    public WebhookClientCertImpl(@Qualifier(WebhookRestTemplateFactory.REST_TEMPLATE_WITH_CLIENT_CERTIFICATE) RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public void sendInfo(String url, List<WebhookOutputDto> data) {
        log.info("Send info webhook with url: " + url);
        HttpEntity<List<WebhookOutputDto>> entity = new HttpEntity<>(data, null);
        ResponseEntity<Void> resp = restTemplate.exchange(url, HttpMethod.POST, entity, Void.class);
        log.info("Response: " + resp);
        if (!resp.getStatusCode().is2xxSuccessful()) {
            throw new PnInternalException("Http error " + resp.getStatusCodeValue() + " calling webhook " + url);
        }
    }
}
