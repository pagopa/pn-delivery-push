package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.externalregistry;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.externalregistry.generated.openapi.clients.externalregistry.ApiClient;
import it.pagopa.pn.externalregistry.generated.openapi.clients.externalregistry.api.SendIoMessageApi;
import it.pagopa.pn.externalregistry.generated.openapi.clients.externalregistry.model.SendMessageRequest;
import it.pagopa.pn.externalregistry.generated.openapi.clients.externalregistry.model.SendMessageResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;


@Slf4j
@Component
public class PnExternalRegistryClientImpl implements PnExternalRegistryClient{
    private final SendIoMessageApi sendIoMessageApi;

    public PnExternalRegistryClientImpl(@Qualifier("withTracing") RestTemplate restTemplate, PnDeliveryPushConfigs cfg) {
        it.pagopa.pn.externalregistry.generated.openapi.clients.externalregistry.ApiClient newApiClient = new ApiClient(restTemplate);
        newApiClient.setBasePath(cfg.getExternalRegistryBaseUrl());
        this.sendIoMessageApi = new SendIoMessageApi( newApiClient );
    }
    
    @Override
    public ResponseEntity<SendMessageResponse> sendIOMessage(SendMessageRequest sendMessageRequest) {
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();

        PnAuditLogEvent logEvent = auditLogBuilder.before(PnAuditLogEventType.AUD_AD_SEND_IO, "sendIOMessage")
                .iun(sendMessageRequest.getIun())
                .build();

        logEvent.log();
        ResponseEntity<SendMessageResponse> resp;
        try {
            log.info("Start sendIOMessage - iun={}", sendMessageRequest.getIun());
            resp = sendIoMessageApi.sendIOMessageWithHttpInfo(sendMessageRequest);
            log.info("Response sendIOMessage - iun={}", sendMessageRequest.getIun());
        } catch (Exception ex) {
            logEvent.generateFailure(ex.getMessage()).log();
            throw ex;
        }
        logEvent.generateSuccess().log();
        return resp;
    }
}
