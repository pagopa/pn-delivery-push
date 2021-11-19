package it.pagopa.pn.deliverypush.webhook.configuration;

import it.pagopa.pn.deliverypush.webhook.WebhookConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

class InitConfWebhookFromFileTest {

    private InitConfWebhookFromFile initConfWebhookFromFile;
    @Mock
    private WebhookConfigService webhookInitConfigService;

    @BeforeEach
    public void setup() {
        String jsonPropertyFilePath = "/webhookconfig/webhookinit.json";
        initConfWebhookFromFile = new InitConfWebhookFromFile(webhookInitConfigService, jsonPropertyFilePath);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void initWebhookConfiguration() {
        initConfWebhookFromFile.initWebhookConfiguration();
        Mockito.verify(webhookInitConfigService).putConfigurations(ArgumentMatchers.anyList());
    }
}