package it.pagopa.pn.deliverypush.webhook;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class WebhookConfigs {

    private final Map<String, WebhookCfgEntry> cfgs = new HashMap<>();

    public WebhookConfigs() {
        addCfg("PA_withWebhook1", "http://provawebhook.it/");
    }

    protected void addCfg( String paId, String url ) {
        this.cfgs.put( paId, new WebhookCfgEntry( paId, url, true));
    }

    public List<WebhookCfgEntry> listActiveWebhook() {
        return Collections.emptyList();
    }

    @Data
    public static class WebhookCfgEntry {
        private final String paId;
        private final String url;
        private final boolean active;
    }

}
