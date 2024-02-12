package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.papernotificationfailed.PaperNotificationFailed;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.dto.timeline.details.AarGenerationDetailsInt;
import it.pagopa.pn.deliverypush.exceptions.PnNotFoundException;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.ResponsePaperNotificationFailedDto;
import it.pagopa.pn.deliverypush.middleware.dao.failednotificationdao.PaperNotificationFailedDao;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.PaperNotificationFailedService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Optional;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_NOTFOUND;

@Slf4j
@Service
public class PaperNotificationFailedServiceImpl implements PaperNotificationFailedService {
    private final PaperNotificationFailedDao paperNotificationFailedDao;
    private final NotificationService notificationService;
    private final NotificationUtils notificationUtils;
    private final TimelineService timelineService;
    
    public PaperNotificationFailedServiceImpl(PaperNotificationFailedDao paperNotificationFailedDao,
                                              NotificationService notificationService,
                                              NotificationUtils notificationUtils, TimelineService timelineService) {
        this.paperNotificationFailedDao = paperNotificationFailedDao;
        this.notificationService = notificationService;
        this.notificationUtils = notificationUtils;
        this.timelineService = timelineService;
    }

    @Override
    public void addPaperNotificationFailed(PaperNotificationFailed paperNotificationFailed) {
        paperNotificationFailedDao.addPaperNotificationFailed(paperNotificationFailed);
    }

    @Override
    public void deleteNotificationFailed(String recipientId, String iun) {
        log.info("PaperNotificationFailed delete for recipientId={} iun={}", recipientId, iun);
        paperNotificationFailedDao.deleteNotificationFailed(recipientId, iun);
    }

    public Flux<ResponsePaperNotificationFailedDto> getPaperNotificationByRecipientId(String recipientId, Boolean getAAR) {
        log.info( "Retrieve paper notifications failed for recipientId={} getAAR={}", recipientId, getAAR);
        return Mono.fromCallable(() -> paperNotificationFailedDao.getPaperNotificationFailedByRecipientId(recipientId))
                .flatMapMany(paperNotificationFailedSet -> {
                    if(paperNotificationFailedSet.isEmpty()) {
                        String message = String.format("There isn't paperNotificationFailed for recipientId=%s ", recipientId);
                        log.warn(message);
                        throw new PnNotFoundException("Not found", message, ERROR_CODE_DELIVERYPUSH_NOTFOUND);
                    }

                    log.debug( "Get paperNotificationFailedSet OK for recipientId={}", recipientId);
                    return Flux.fromStream(paperNotificationFailedSet.stream());
                })
                .parallel()
                .runOn(Schedulers.boundedElastic())
                .flatMap(paperNotificationFailed -> {
                    String iun = paperNotificationFailed.getIun();
                    ResponsePaperNotificationFailedDto res = createResponse(paperNotificationFailed, iun);
                    if(Boolean.TRUE.equals(getAAR)){
                        return getAAR(recipientId, paperNotificationFailed, iun)
                                .map(optAar -> {
                                    optAar.ifPresent(res::setAarUrl);
                                    return res;
                                });
                    }else {
                        log.debug("AAR is not required for this request - iun={} internalId={}", iun, recipientId);
                        return Mono.just(res);
                    }
                })
                .sequential();
    }


    @NotNull
    private ResponsePaperNotificationFailedDto createResponse(PaperNotificationFailed elem, String iun) {
        ResponsePaperNotificationFailedDto res = new ResponsePaperNotificationFailedDto();
        res.setIun(iun);
        res.setRecipientInternalId(elem.getRecipientId());
        return res;
    }

    private Mono<Optional<String>> getAAR(String recipientId, PaperNotificationFailed elem, String iun) {
        log.debug( "Start getAAR process - recipientId={} iun={}", recipientId, elem.getIun());

        return Mono.fromCallable(() -> notificationService.getNotificationByIun(iun))
                .map(notificationInt -> buildAARGenerationElementId(notificationInt, iun, recipientId))
                .flatMap(elementId -> getAarGenerationDetailFromTimeline(iun, elementId))
                .map(aorDetailsOpt -> {
                    if(aorDetailsOpt.isPresent()){
                        log.debug( "Get AAR url Ok - recipientId={} iun={}", recipientId, iun);
                        AarGenerationDetailsInt details = aorDetailsOpt.get();
                        return Optional.of(details.getGeneratedAarUrl());
                    } else {
                        log.error( "Get AAR url ERROR - recipientId={} iun={}", recipientId, iun);
                        return Optional.empty();
                    }
                });
    }

    private String buildAARGenerationElementId(NotificationInt notificationInt, String iun, String recipientId) {
        int index = notificationUtils.getRecipientIndexFromInternalId(notificationInt, recipientId);
        log.debug( "getNotification and getIndex Ok - recipientId={} iun={}", recipientId, iun);

        return TimelineEventId.AAR_GENERATION.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(index)
                        .build());
    }

    private Mono<Optional<AarGenerationDetailsInt>> getAarGenerationDetailFromTimeline(String iun, String elementId) {
        return Mono.fromCallable(() -> timelineService.getTimelineElementDetails(iun, elementId, AarGenerationDetailsInt.class));
    }


}
