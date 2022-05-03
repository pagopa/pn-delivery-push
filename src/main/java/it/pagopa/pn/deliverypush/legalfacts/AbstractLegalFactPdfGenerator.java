package it.pagopa.pn.deliverypush.legalfacts;

import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.address.PhysicalAddress;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElement;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.middleware.timelinedao.TimelineDao;
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

    private static final DateTimeFormatter ITALIAN_DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Duration ONE_HOUR = Duration.ofHours(1);
    private static final ZoneId ROME_ZONE = ZoneId.of("Europe/Rome");

    private final TimelineDao timelineDao;

    protected AbstractLegalFactPdfGenerator(TimelineDao timelineDao) {
        this.timelineDao = timelineDao;
    }

    protected String instantToDate(Instant instant) {
        String suffix;
        Instant nextTransition = ROME_ZONE.getRules().nextTransition( instant ).getInstant();
        boolean isAmbiguous = isNear( instant, nextTransition );

        if( ! isAmbiguous ) {
            Instant prevTransition = ROME_ZONE.getRules().previousTransition( instant ).getInstant();
            isAmbiguous = isNear( instant, prevTransition );
            if( isAmbiguous ) {
                suffix = " CET";
            }
            else {
                suffix = "";
            }
        }
        else {
            suffix = " CEST";
        }

        LocalDateTime localDate = LocalDateTime.ofInstant(instant, ROME_ZONE);
        String date = localDate.format( ITALIAN_DATE_TIME_FORMAT );

        return date + suffix;
    }

    private boolean isNear( Instant a, Instant b) {
        Instant min;
        Instant max;
        if( a.isBefore(b) ) {
            min = a;
            max = b;
        }
        else {
            min = b;
            max = a;
        }
        Duration timeInterval = Duration.between(min, max);
        return ONE_HOUR.compareTo(timeInterval) >= 0;
    }

    protected String nullSafePhysicalAddressToString( NotificationRecipient recipient, String separator ) {
        String result = null;

        if ( recipient != null ) {
            PhysicalAddress physicalAddress = recipient.getPhysicalAddress();
            if ( physicalAddress != null ) {
                List<String> standardAddressString = physicalAddress.toStandardAddressString( recipient.getDenomination() );
                if ( standardAddressString != null ) {
                    result = String.join( separator, standardAddressString );
                }
            }
        }

        return result;
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
