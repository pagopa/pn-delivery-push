package it.pagopa.pn.deliverypush.webhook;

import it.pagopa.pn.api.dto.webhook.WebhookConfigDto;
import it.pagopa.pn.deliverypush.webhook.cassandra.CassandraWebhookConfigEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

class WebhookConfigServiceTest {
    private WebhookConfigService webhookConfigService;
    private DtoToEntityConfigMapper dtoToEntityConfigMapper;
    @Mock
    private WebhookConfigsDao webhookConfigsDao;

    @BeforeEach
    public void setup() {
        dtoToEntityConfigMapper = new DtoToEntityConfigMapper();
        webhookConfigService = new WebhookConfigService(webhookConfigsDao, dtoToEntityConfigMapper);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void putConfigurations() {
        List<WebhookConfigDto> listConfigDto = new ArrayList<>();
        listConfigDto.add(WebhookConfigDto.builder()
                .paId("paMilano1")
                .url("testUrl")
                .startFrom(Instant.now())
                .active(true)
                .build());
        webhookConfigService.putConfigurations(listConfigDto);
        Mockito.verify(webhookConfigsDao).put(Mockito.any(CassandraWebhookConfigEntity.class));
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void putConfiguration() {
        WebhookConfigDto dto = WebhookConfigDto.builder()
                .paId("paMilano1")
                .url("testUrl")
                .startFrom(Instant.now())
                .active(true)
                .build();
        webhookConfigService.putConfiguration(dto);
        Mockito.verify(webhookConfigsDao).put(Mockito.any(CassandraWebhookConfigEntity.class));
    }
}