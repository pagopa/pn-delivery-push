package it.pagopa.pn.deliverypush.webhook;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@Service
@Slf4j
public class WebhookBufferReaderService {

    private final WebhookConfigsDao configs;
    private final WebhookBufferDao webhookBufferDao;
    private final WebhookClient client;

    public WebhookBufferReaderService(WebhookConfigsDao configs, WebhookBufferDao webhookBufferDao, WebhookClient client) {
        this.configs = configs;
        this.webhookBufferDao = webhookBufferDao;
        this.client = client;
    }

    @Scheduled(fixedDelay = 60 * 1000)
    public void readWebhookBufferAndSend() {
        configs.activeWebhooks().forEach( this::readWebhookBufferAndSend );
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
                    if( bufferChunk.size() >= CHUNK_SIZE ) {
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
                        configs.setWebhookStartFrom(webhook.getPaId(), newLastUpdate )
                    );
        }
        catch (RuntimeException exc) {
            log.error("Calling webhook " + webhook.getUrl(), exc);
        }
    }

    private static final int CHUNK_SIZE = 10;

}
