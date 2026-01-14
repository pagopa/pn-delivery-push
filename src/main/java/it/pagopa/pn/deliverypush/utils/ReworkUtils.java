package it.pagopa.pn.deliverypush.utils;

import it.pagopa.pn.deliverypush.middleware.dao.notificationreworkdao.dynamo.entity.NotificationReworksEntity;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.List;

public final class ReworkUtils {

    private static final Comparator<NotificationReworksEntity> BY_REWORK_THEN_TRY =
            Comparator.comparingInt((NotificationReworksEntity e) ->
                            NotificationReworksEntity.ReworkIdBuilder.extractReworkIdx(e.getReworkId()))
                    .thenComparingInt(e ->
                            NotificationReworksEntity.ReworkIdBuilder.extractTryIdx(e.getReworkId()));

    public static Mono<NotificationReworksEntity> getLatestReworkRequest(List<NotificationReworksEntity> reworks) {
        return CollectionUtils.isEmpty(reworks) ? Mono.empty() : reworks.stream().max(BY_REWORK_THEN_TRY).map(Mono::just).orElse(Mono.empty());
    }
}