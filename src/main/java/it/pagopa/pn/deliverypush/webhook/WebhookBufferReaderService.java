package it.pagopa.pn.deliverypush.webhook;

import it.pagopa.pn.api.dto.notification.status.NotificationStatus;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementCategory;
import it.pagopa.pn.api.dto.webhook.WebhookConfigDto;
import it.pagopa.pn.commons.utils.DateUtils;
import it.pagopa.pn.commons_delivery.utils.EncodingUtils;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.webhook.dto.WebhookBufferRowDto;
import it.pagopa.pn.deliverypush.webhook.dto.WebhookOutputDto;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

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

    protected void readWebhookBufferAndSend(WebhookConfigDto webhook) {
        log.debug("Scan webhook " + webhook.getPaId());

        Instant lastUpdate = webhook.getStartFrom();
        if (lastUpdate == null) {
            lastUpdate = Instant.EPOCH;
        }

        AtomicReference<List<WebhookBufferRowDto>> bufferChunkRef = new AtomicReference<>();

        webhookBufferDao.bySenderIdAndDate(webhook.getPaId(), lastUpdate)
                .map(bufferRow -> {
                    List<WebhookBufferRowDto> bufferChunk = bufferChunkRef.updateAndGet(b -> b != null ? b : new ArrayList<>());

                    bufferChunk.add(bufferRow);
                    if (bufferChunk.size() >= chunkSize) {
                        return bufferChunkRef.getAndSet(null);
                    } else {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .forEachOrdered(chunk -> sendOneChunk(webhook, chunk));

        if (bufferChunkRef.get() != null) {
            sendOneChunk(webhook, bufferChunkRef.get());
        }
    }

    private void sendOneChunk(WebhookConfigDto webhook, List<WebhookBufferRowDto> chunk) {
        try {
            log.info("Call webhook " + webhook.getPaId() + " url(" + webhook.getUrl() + ")" + " with chunk size " + chunk.size());
            List<WebhookOutputDto> webhookOutputDtoList = getListWebhookOutputDto(chunk);

            client.sendInfo(webhook.getUrl(), webhookOutputDtoList);
            chunk.stream().map(WebhookBufferRowDto::getStatusChangeTime)
                    .max(Comparator.naturalOrder())
                    .ifPresent(newLastUpdate ->
                            webhookConfigsDao.setWebhookStartFrom(webhook.getPaId(), newLastUpdate)
                    );
        } catch (RuntimeException exc) {
            log.error("Calling webhook " + webhook.getUrl(), exc);
        }
    }

    @NotNull
    private List<WebhookOutputDto> getListWebhookOutputDto(List<WebhookBufferRowDto> chunk) {
        return chunk.stream().map( this::bufferRow2output )
                .collect(Collectors.toList());
    }

    private WebhookOutputDto bufferRow2output( WebhookBufferRowDto row ) {
        boolean refusedCondition = NotificationStatus.REFUSED.toString().equals(row.getNotificationElement()) 
                || TimelineElementCategory.REQUEST_REFUSED.toString().equals(row.getNotificationElement());

        WebhookOutputDto out = WebhookOutputDto.builder()
                .notificationElement(row.getNotificationElement())
                .iun(refusedCondition ? null : row.getIun())
                .notificationId(EncodingUtils.base64Encoding(row.getIun()))
                .senderId(row.getSenderId())
                .statusChangeTime(DateUtils.formatInstantToString(row.getStatusChangeTime(), DateUtils.yyyyMMddHHmmssSSSZ))
                .build();
        log.debug("webhook Buffer row " + row + " mapped to " + out );
        return out;
    }

}
