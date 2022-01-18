package it.pagopa.pn.deliverypush.webhook;

import it.pagopa.pn.deliverypush.webhook.configuration.ClientCertificateCfg;
import it.pagopa.pn.deliverypush.webhook.dto.WebhookOutputDto;

import java.util.List;

public interface WebhookClient {

    void sendInfo(String url, List<WebhookOutputDto> data, ClientCertificateCfg certCfg);

}
