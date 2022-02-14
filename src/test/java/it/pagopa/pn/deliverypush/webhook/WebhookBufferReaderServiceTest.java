package it.pagopa.pn.deliverypush.webhook;

import it.pagopa.pn.api.dto.notification.timeline.TimelineElementCategory;
import it.pagopa.pn.api.dto.webhook.WebhookConfigDto;
import it.pagopa.pn.commons_delivery.utils.EncodingUtils;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.webhook.dto.WebhookBufferRowDto;
import it.pagopa.pn.deliverypush.webhook.dto.WebhookOutputDto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

class WebhookBufferReaderServiceTest {
    private WebhookBufferReaderService webhookBufferReaderService;

    @Mock
    private WebhookConfigsDao webhookConfigsDao;
    @Mock
    private WebhookBufferDao webhookBufferDao;
    @Mock
    private PnDeliveryPushConfigs cfg;
    @Mock
    private WebhookClient client;

    @BeforeEach
    public void setup() {
        PnDeliveryPushConfigs.Webhook webhookConf = new PnDeliveryPushConfigs.Webhook();
        webhookConf.setMaxLength(1000);
        Mockito.when(cfg.getWebhook()).thenReturn(webhookConf);

        webhookBufferReaderService = new WebhookBufferReaderService(webhookConfigsDao,webhookBufferDao, client, cfg);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void requestRefused() {

        List<WebhookBufferRowDto> listRow = new ArrayList<>();
        
        WebhookBufferRowDto row = WebhookBufferRowDto.builder()
                .senderId("paMilano1")
                .iun("iun")
                .statusChangeTime(Instant.now())
                .notificationElement(TimelineElementCategory.REQUEST_REFUSED.toString())
                .build();
        listRow.add(row);

        Mockito.when(webhookBufferDao.bySenderIdAndDate(Mockito.anyString(), Mockito.any(Instant.class))).thenReturn(listRow.stream());
                
        WebhookConfigDto webhook = WebhookConfigDto.builder()
                .paId("paMilano1")
                .url("testUrl")
                .startFrom(Instant.now())
                .active(true)
                .build();

        webhookBufferReaderService.readWebhookBufferAndSend(webhook);

        ArgumentCaptor<List<WebhookOutputDto>> listWebhookOutputDtoCaptor = ArgumentCaptor.forClass(List.class);

        Mockito.verify(client).sendInfo(Mockito.any(),listWebhookOutputDtoCaptor.capture());

        List<WebhookOutputDto> listWebhookOutputDtoValue = listWebhookOutputDtoCaptor.getValue();

        WebhookOutputDto elem = listWebhookOutputDtoValue.get(0);

        Assertions.assertNull(elem.getIun());
        Assertions.assertEquals(TimelineElementCategory.REQUEST_REFUSED.toString(),elem.getNotificationElement());
        Assertions.assertEquals(EncodingUtils.base64Encoding(row.getIun()),elem.getNotificationId());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void requestOk() {

        List<WebhookBufferRowDto> listRow = new ArrayList<>();

        WebhookBufferRowDto row = WebhookBufferRowDto.builder()
                .senderId("paMilano1")
                .iun("iun")
                .statusChangeTime(Instant.now())
                .notificationElement(TimelineElementCategory.NOTIFICATION_VIEWED.toString())
                .build();
        listRow.add(row);

        Mockito.when(webhookBufferDao.bySenderIdAndDate(Mockito.anyString(), Mockito.any(Instant.class))).thenReturn(listRow.stream());

        WebhookConfigDto webhook = WebhookConfigDto.builder()
                .paId("paMilano1")
                .url("testUrl")
                .startFrom(Instant.now())
                .active(true)
                .build();

        webhookBufferReaderService.readWebhookBufferAndSend(webhook);

        ArgumentCaptor<List<WebhookOutputDto>> listWebhookOutputDtoCaptor = ArgumentCaptor.forClass(List.class);

        Mockito.verify(client).sendInfo(Mockito.any(),listWebhookOutputDtoCaptor.capture());

        List<WebhookOutputDto> listWebhookOutputDtoValue = listWebhookOutputDtoCaptor.getValue();

        WebhookOutputDto elem = listWebhookOutputDtoValue.get(0);

        Assertions.assertNotNull(elem.getIun());
        Assertions.assertEquals(TimelineElementCategory.NOTIFICATION_VIEWED.toString(),elem.getNotificationElement());
        Assertions.assertEquals(EncodingUtils.base64Encoding(row.getIun()),elem.getNotificationId());
    }
}