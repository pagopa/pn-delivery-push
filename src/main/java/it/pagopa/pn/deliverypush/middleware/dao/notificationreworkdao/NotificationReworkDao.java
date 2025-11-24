package it.pagopa.pn.deliverypush.middleware.dao.notificationreworkdao;

import it.pagopa.pn.deliverypush.middleware.dao.notificationreworkdao.dynamo.entity.NotificationReworksEntity;
import reactor.core.publisher.Mono;

import java.util.List;

public interface NotificationReworkDao {
    Mono<NotificationReworksEntity> putIfAbsent(NotificationReworksEntity notificationReworksEntity);
    Mono<NotificationReworksEntity> findByIunAndReworkId(String iun, String reworkId);
    Mono<List<NotificationReworksEntity>> findByIun(String iun);
    Mono<NotificationReworksEntity> findLatestByIun(String iun);
}

