package it.pagopa.pn.deliverypush.legalfacts;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Component
public class CustomInstantWriter {

    private static final DateTimeFormatter ITALIAN_DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter ITALIAN_DATE_TIME_FORMAT_NO_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter ITALIAN_DATE_TIME_FORMAT_TIME_ONLY = DateTimeFormatter.ofPattern("HH:mm");
    private static final Duration ONE_HOUR = Duration.ofHours(1);
    private static final ZoneId ROME_ZONE = ZoneId.of("Europe/Rome");

    public String instantToDate(Instant instant) {

        return instantToDate(instant, ITALIAN_DATE_TIME_FORMAT);
    }

    public String instantToTime(Instant instant) {
        return instantToDate(instant, ITALIAN_DATE_TIME_FORMAT_TIME_ONLY);
    }

    public String instantToDate(Instant instant, boolean withoutTime) {
        return instantToDate(instant, withoutTime?ITALIAN_DATE_TIME_FORMAT_NO_TIME:ITALIAN_DATE_TIME_FORMAT);
    }

    private String instantToDate(Instant instant, DateTimeFormatter formatter) {
        String suffix;
        Instant nextTransition = ROME_ZONE.getRules().nextTransition(instant).getInstant();
        boolean isAmbiguous = isNear(instant, nextTransition);

        if (!isAmbiguous) {
            Instant prevTransition = ROME_ZONE.getRules().previousTransition(instant).getInstant();
            isAmbiguous = isNear(instant, prevTransition);
            if (isAmbiguous) {
                suffix = " CET";
            } else {
                suffix = "";
            }
        } else {
            suffix = " CEST";
        }

        LocalDateTime localDate = LocalDateTime.ofInstant(instant, ROME_ZONE);

        if (formatter.equals(ITALIAN_DATE_TIME_FORMAT_NO_TIME)) {
            return localDate.format(formatter);
        }

        return localDate.format(formatter) + suffix;
    }

    private boolean isNear(Instant a, Instant b) {
        Instant min;
        Instant max;
        if (a.isBefore(b)) {
            min = a;
            max = b;
        } else {
            min = b;
            max = a;
        }
        Duration timeInterval = Duration.between(min, max);
        return ONE_HOUR.compareTo(timeInterval) >= 0;
    }
}
