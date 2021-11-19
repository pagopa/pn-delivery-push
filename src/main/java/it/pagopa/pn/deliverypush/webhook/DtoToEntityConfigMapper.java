package it.pagopa.pn.deliverypush.webhook;

import it.pagopa.pn.api.dto.webhook.WebhookConfigDto;
import it.pagopa.pn.deliverypush.webhook.cassandra.CassandraWebhookConfigEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DtoToEntityConfigMapper {

    public List<CassandraWebhookConfigEntity> dto2Entity(List<WebhookConfigDto> listDto) {
        List<CassandraWebhookConfigEntity> listEntity = new ArrayList<>();

        listDto.forEach(dto -> listEntity.add(CassandraWebhookConfigEntity.builder()
                .paId(dto.getPaId())
                .url(dto.getUrl())
                .since(dto.getStartFrom())
                .active(dto.isActive())
                .allNotifications(dto.isAllNotifications())
                .notificationsElement(dto.getNotificationsElement())
                .type(dto.getType())
                .build()));

        return listEntity;
    }

    public CassandraWebhookConfigEntity dto2Entity(WebhookConfigDto dto) {
        return CassandraWebhookConfigEntity.builder()
                .paId(dto.getPaId())
                .url(dto.getUrl())
                .since(dto.getStartFrom())
                .active(dto.isActive())
                .allNotifications(dto.isAllNotifications())
                .notificationsElement(dto.getNotificationsElement())
                .type(dto.getType())
                .build();
    }
}
