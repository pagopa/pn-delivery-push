package it.pagopa.pn.deliverypush.middleware.dao.notificationreworkdao;

import it.pagopa.pn.deliverypush.middleware.dao.notificationreworkdao.dynamo.entity.NotificationReworksEntity;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;

public interface NotificationReworkDao {
    Mono<NotificationReworksEntity> putIfAbsent(NotificationReworksEntity notificationReworksEntity);
    Mono<NotificationReworksEntity> findByIunAndReworkId(String iun, String reworkId);
    Mono<Page<NotificationReworksEntity>> findByIun(String iun, Map<String, AttributeValue> lastEvaluateKey, int limit);
    Mono<NotificationReworksEntity> findLatestByIun(String iun);
}

