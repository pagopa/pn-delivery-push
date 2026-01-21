package it.pagopa.pn.deliverypush.middleware.dao.notificationreworkdao;

import it.pagopa.pn.deliverypush.middleware.dao.notificationreworkdao.dynamo.entity.NotificationReworksEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface NotificationReworkDao {
    Mono<NotificationReworksEntity> putIfAbsent(NotificationReworksEntity notificationReworksEntity);
    Mono<NotificationReworksEntity> findByIunAndReworkId(String iun, String reworkId);
    Flux<NotificationReworksEntity> findByIun(String iun);
    Mono<NotificationReworksEntity> findLatestByIun(String iun);
    Mono<Void> updateStatusError(String iun, String reworkId, String message);
    Mono<NotificationReworksEntity> updateStatusToPending(String iun, String reworkId);
}

