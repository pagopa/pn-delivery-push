package it.pagopa.pn.deliverypush.webhook;

import it.pagopa.pn.commons.abstractions.KeyValueStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class WebhookBufferReader {

    private final WebhookConfigs configs;
    private final KeyValueStore<WebhookBufferRowEntityId, WebhookBufferRowEntity> webhookBufferDao;

    public WebhookBufferReader(WebhookConfigs configs, KeyValueStore<WebhookBufferRowEntityId, WebhookBufferRowEntity> webhookBufferDao) {
        this.configs = configs;
        this.webhookBufferDao = webhookBufferDao;
    }

    //MANCA MEMORIZZAZIONE ULTIMA ESECUZIONE

    public void readWebhookBufferAndSend() {
        for( WebhookConfigs.WebhookCfgEntry webhook: configs.listActiveWebhook() ) {

        }
    }

}
