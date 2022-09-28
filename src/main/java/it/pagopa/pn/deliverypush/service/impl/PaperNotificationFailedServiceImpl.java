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

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
        paperNotificationFailedDao.deleteNotificationFailed(recipientId, iun);
    }

    @Override
    public List<ResponsePaperNotificationFailedDto> getPaperNotificationByRecipientId(String recipientId, Boolean getAAR) {
        log.info( "Retrieve paper notifications failed for recipientId={} getAAR={}", recipientId, getAAR);
        Set<PaperNotificationFailed> paperNotificationFailedSet = paperNotificationFailedDao.getPaperNotificationFailedByRecipientId(recipientId);

        if(! paperNotificationFailedSet.isEmpty() ){
            log.debug( "Get paperNotificationFailedSet OK for recipientId={}", recipientId);

            return paperNotificationFailedSet.stream().map(
                    elem -> {
                        log.debug( "Get paperNotificationFailed element for recipientId={} is with iun={}", recipientId, elem.getIun());
                        String iun = elem.getIun();

                        ResponsePaperNotificationFailedDto res = createResponse(elem, iun);

                        if(Boolean.TRUE.equals(getAAR)){
                            getAAR(recipientId, elem, res, iun);
                        }else {
                            log.debug("AAR is not required for this request - iun={} internalId={}", iun, recipientId);
                        }
                        
                        return res;
                    }
            ).collect(Collectors.toList());
            
        } else {
            String message = String.format("There isn't paperNotificationFailed for recipientId=%s ", recipientId);
            log.warn(message);
            throw new PnNotFoundException("Not found", message, ERROR_CODE_DELIVERYPUSH_NOTFOUND);
        }
    }

    @NotNull
    private ResponsePaperNotificationFailedDto createResponse(PaperNotificationFailed elem, String iun) {
        ResponsePaperNotificationFailedDto res = new ResponsePaperNotificationFailedDto();
        res.setIun(iun);
        res.setRecipientInternalId(elem.getRecipientId());
        return res;
    }

    private void getAAR(String recipientId, PaperNotificationFailed elem, ResponsePaperNotificationFailedDto res, String iun) {
        log.debug( "Start getAAR process - recipientId={} iun={}", recipientId, elem.getIun());

        NotificationInt notification = notificationService.getNotificationByIun(iun);
        int index = notificationUtils.getRecipientIndexFromInternalId(notification, elem.getRecipientId());
        log.debug( "getNotification and getIndex Ok - recipientId={} iun={}", recipientId, elem.getIun());

        String elementId = TimelineEventId.AAR_GENERATION.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(index)
                        .build());

        Optional<AarGenerationDetailsInt> detailsOpt = timelineService.getTimelineElementDetails(iun, elementId, AarGenerationDetailsInt.class);
        if( detailsOpt.isPresent()){
            AarGenerationDetailsInt details = detailsOpt.get();
            res.setAarUrl(details.getGeneratedAarUrl());
            log.debug( "Get AAR url Ok - recipientId={} iun={}", recipientId, elem.getIun());
        }else {
            log.error( "Get AAR url ERROR - recipientId={} iun={}", recipientId, elem.getIun());
        }
    }

}
