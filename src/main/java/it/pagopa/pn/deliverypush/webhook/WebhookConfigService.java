package it.pagopa.pn.deliverypush.webhook;

import it.pagopa.pn.api.dto.webhook.WebhookConfigDto;
import it.pagopa.pn.deliverypush.webhook.cassandra.CassandraWebhookConfigEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WebhookConfigService {

    private WebhookConfigsDao webhookConfigsDao;
    private DtoToEntityConfigMapper dtoToEntityConfigMapper;

    public WebhookConfigService(WebhookConfigsDao webhookConfigsDao, DtoToEntityConfigMapper dtoToEntityConfigMapper) {
        this.webhookConfigsDao = webhookConfigsDao;
        this.dtoToEntityConfigMapper = dtoToEntityConfigMapper;
    }

    public void putConfigurations(List<WebhookConfigDto> listConfigDto) {
        List<CassandraWebhookConfigEntity> listEntity = dtoToEntityConfigMapper.dto2Entity(listConfigDto);
        listEntity.forEach(entity -> webhookConfigsDao.put(entity));
    }

    public void putConfiguration(WebhookConfigDto configDto) {
        CassandraWebhookConfigEntity entity = dtoToEntityConfigMapper.dto2Entity(configDto);
        webhookConfigsDao.put(entity);
    }
}
