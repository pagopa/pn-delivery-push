package it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.impl;

import it.pagopa.pn.api.dto.events.MomProducer;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionsPool;
import it.pagopa.pn.deliverypush.service.ActionService;
import it.pagopa.pn.deliverypush.utils.FeatureEnabledUtils;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@CustomLog
public class ActionsPoolImpl implements ActionsPool {

    public static final String TIMESLOT_PATTERN = "yyyy-MM-dd'T'HH:mm";
    private final MomProducer<ActionEvent> actionsQueue;
    private final ActionService actionService;
    private final Clock clock;
    private final PnDeliveryPushConfigs configs;
    private final FeatureEnabledUtils featureEnabledUtils;

    private Duration lockAtMostFor;
    private Duration timeToBreak;
    
    public ActionsPoolImpl(MomProducer<ActionEvent> actionsQueue, ActionService actionService,
                           Clock clock, PnDeliveryPushConfigs configs,
                           FeatureEnabledUtils featureEnabledUtils, @Value("${lockAtMostFor}") Duration lockAtMostFor, @Value("${timeToBreak}") Duration timeToBreak) {
        this.actionsQueue = actionsQueue;
        this.actionService = actionService;
        this.clock = clock;
        this.configs = configs;
        this.featureEnabledUtils = featureEnabledUtils;
        this.lockAtMostFor = lockAtMostFor;
        this.timeToBreak = timeToBreak;
    }

    @Override
    public void addOnlyAction(Action action){
        final String timeSlot = computeTimeSlot( action.getNotBefore() );
        action = action.toBuilder()
                .timeslot( timeSlot)
                .build();
        actionService.addOnlyActionIfAbsent(action);
    }

    protected List<String> computeTimeSlots(Instant from, Instant to) {
        List<String> timeSlots = new ArrayList<>();
        Instant timeSlotStart = from.truncatedTo( ChronoUnit.MINUTES );
        //I timeslot restituiti arrivano al minuto appena prima a now
        while( timeSlotStart.isBefore( to )) {
            String timeSlot = computeTimeSlot( timeSlotStart );
            timeSlots.add( timeSlot );

            timeSlotStart = timeSlotStart.plus( 1, ChronoUnit.MINUTES );
        }
        return timeSlots;
    }

    private String computeTimeSlot(Instant instant) {
        OffsetDateTime nowUtc = instant.atOffset( ZoneOffset.UTC );
        int year = nowUtc.get( ChronoField.YEAR_OF_ERA );
        int month = nowUtc.get( ChronoField.MONTH_OF_YEAR );
        int day = nowUtc.get( ChronoField.DAY_OF_MONTH );
        int hour = nowUtc.get( ChronoField.HOUR_OF_DAY );
        int minute = nowUtc.get( ChronoField.MINUTE_OF_HOUR );
        return String.format("%04d-%02d-%02dT%02d:%02d", year, month, day, hour, minute);
    }
}
