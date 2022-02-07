package it.pagopa.pn.deliverypush.legalfacts;

import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.address.PhysicalAddress;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons_delivery.middleware.TimelineDao;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Slf4j
abstract class AbstractLegalFactPdfGenerator {


    private final TimelineDao timelineDao;

    protected AbstractLegalFactPdfGenerator(TimelineDao timelineDao) {
        this.timelineDao = timelineDao;
    }





    protected TimelineElement timelineElement(Action action) {
        Optional<TimelineElement> row;
        row = this.timelineDao.getTimelineElement( action.getIun(), action.getActionId() );
        if ( !row.isPresent() ) {
            String msg = "Error while retrieving timeline for IUN %s and action %s";
            msg = String.format( msg, action.getIun(), action.getActionId() );
            log.debug( msg );
            throw new PnInternalException( msg );
        }
        return row.get();
    }



}
