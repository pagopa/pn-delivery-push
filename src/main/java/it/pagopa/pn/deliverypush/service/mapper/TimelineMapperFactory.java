package it.pagopa.pn.deliverypush.service.mapper;

import java.time.Instant;

public class TimelineMapperFactory {
    public static TimelineMapper getTimelineMapper(Instant notificationSentAt){
        Instant fixReleaseDate = getFixReleaseDate();
        
        if(notificationSentAt.isAfter(fixReleaseDate) || notificationSentAt.equals(fixReleaseDate)){
            return new TimelineMapperAfterFix();
        }else {
            return new TimelineMapperBeforeFix();
        }
    }

    private static Instant getFixReleaseDate() {
        //recupero fixReleaseDate da config
        return null;
    }
}
