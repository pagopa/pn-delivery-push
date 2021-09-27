package it.pagopa.pn.deliverypush.webhook;

import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@Service
@Slf4j
public class WebhookBufferReaderService {

    private final WebhookConfigsDao webhookConfigsDao;
    private final WebhookBufferDao webhookBufferDao;
    private final WebhookClient client;
    private final int chunkSize;

    public WebhookBufferReaderService(WebhookConfigsDao webhookConfigsDao, WebhookBufferDao webhookBufferDao, WebhookClient client, PnDeliveryPushConfigs cfg) {
        this.webhookConfigsDao = webhookConfigsDao;
        this.webhookBufferDao = webhookBufferDao;
        this.client = client;
        this.chunkSize = cfg.getWebhook().getMaxLength();
    }

    @Scheduled(fixedDelayString = "${pn.delivery-push.webhook.schedule-interval}")
    public void readWebhookBufferAndSend() {
        webhookConfigsDao.activeWebhooks().forEach( this::readWebhookBufferAndSend );
    }

    protected void readWebhookBufferAndSend(WebhookInfoDto webhook ) {
        log.info("Scan webhook " + webhook);

        Instant lastUpdate = webhook.getStartFrom();
        if( lastUpdate == null ) {
            lastUpdate = Instant.EPOCH;
        }

        AtomicReference<List<WebhookBufferRowDto>> bufferChunkRef = new AtomicReference<>();

        webhookBufferDao.bySenderIdAndDate( webhook.getPaId(), lastUpdate )
            .map( bufferRow -> {
                    List<WebhookBufferRowDto> bufferChunk = bufferChunkRef.updateAndGet( b -> b != null ? b : new ArrayList<>());
                    bufferChunk.add( bufferRow );
                    if( bufferChunk.size() >= chunkSize) {
                        return bufferChunkRef.getAndSet( null );
                    }
                    else {
                        return null;
                    }
                })
            .filter( Objects::nonNull )
            .forEachOrdered( chunk -> sendOneChunk(webhook, chunk) );

        if( bufferChunkRef.get() != null ) {
            sendOneChunk( webhook, bufferChunkRef.get() );
        }
    }

    private void sendOneChunk(WebhookInfoDto webhook, List<WebhookBufferRowDto> chunk) {
        try {
            log.info("Call webhook " + webhook + " with chunk size " + chunk.size() );
            client.sendInfo( webhook.getUrl(), chunk);
            chunk.stream().map( WebhookBufferRowDto::getStatusChangeTime )
                    .max( Comparator.naturalOrder() )
                    .ifPresent( newLastUpdate ->
                        webhookConfigsDao.setWebhookStartFrom(webhook.getPaId(), newLastUpdate )
                    );
        }
        catch (RuntimeException exc) {
            log.error("Calling webhook " + webhook.getUrl(), exc);
        }
    }

}
