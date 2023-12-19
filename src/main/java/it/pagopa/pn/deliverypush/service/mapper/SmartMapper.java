package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.*;
import it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementDetailsV20;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.modelmapper.convention.MatchingStrategies;

@Slf4j
public class SmartMapper {
    private static ModelMapper modelMapper;


    private static BiFunction postMappingTransformer;

    private SmartMapper (){}


    static PropertyMap<NormalizedAddressDetailsInt, TimelineElementDetailsV20> addressDetailPropertyMap = new PropertyMap<>() {
        @Override
        protected void configure() {
            skip(destination.getNewAddress());
            skip(destination.getPhysicalAddress());
        }
    };


    static PropertyMap<NotificationViewedDetailsInt, TimelineElementDetailsV20> notificationViewedDetailPropertyMap = new PropertyMap<>() {
        @Override
        protected void configure() {
            skip(destination.getEventTimestamp());
        }
    };

    static PropertyMap<SendDigitalProgressDetailsInt, TimelineElementDetailsV20> sendDigitalProgressDetailPropertyMap = new PropertyMap<>() {
        @Override
        protected void configure() {
            skip(destination.getEventTimestamp());
        }
    };

    static PropertyMap<NotificationPaidDetailsInt, TimelineElementDetailsV20> notificationPaidDetailPropertyMap = new PropertyMap<>() {
        @Override
        protected void configure() {
            skip(destination.getEventTimestamp());
        }
    };

    static PropertyMap<PrepareAnalogDomicileFailureDetailsInt, TimelineElementDetailsV20> prepareAnalogDomicileFailureDetailsInt = new PropertyMap<>() {
        @Override
        protected void configure() {
            skip(destination.getPhysicalAddress());
        }
    };
    static Converter<TimelineElementInternal, TimelineElementInternal>
            timelineElementInternalTimestampConverter =
            ctx -> {
                // se il detail estende l'interfaccia e l'elementTimestamp non è nullo, lo sovrascrivo nel source originale
                if (ctx.getSource().getDetails() instanceof ElementTimestampTimelineElementDetails elementTimestampTimelineElementDetails
                    && elementTimestampTimelineElementDetails.getElementTimestamp() != null)
                {
                    return ctx.getSource().toBuilder()
                            .timestamp(elementTimestampTimelineElementDetails.getElementTimestamp())
                            .build();
                }

                return ctx.getSource();
            };

    static{
        modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        modelMapper.addMappings(addressDetailPropertyMap);
        modelMapper.addMappings(prepareAnalogDomicileFailureDetailsInt);
        modelMapper.addMappings(notificationViewedDetailPropertyMap);
        modelMapper.addMappings(sendDigitalProgressDetailPropertyMap);
        modelMapper.addMappings(notificationPaidDetailPropertyMap);

        modelMapper.createTypeMap(TimelineElementInternal.class, TimelineElementInternal.class).setPostConverter(timelineElementInternalTimestampConverter);

        List<BiFunction> postMappingTransformers = new ArrayList<>();
        postMappingTransformers.add( (source, result)-> {
            if (!(source instanceof NotificationCancelledDetailsInt) && result instanceof TimelineElementDetailsV20){
                ((TimelineElementDetailsV20) result).setNotRefinedRecipientIndexes(null);
            }
            return result;
        });

        postMappingTransformer =  postMappingTransformers.stream()
            .reduce((f, g) -> (i, s) -> f.apply(i, g.apply(i, s)))
            .get();
    }

    public static  <S,T> T mapToClass(S source, Class<T> destinationClass ){
        T result;
        if( source != null) {
            result = modelMapper.map(source, destinationClass );

            result = (T) postMappingTransformer.apply(source, result);
        } else {
            result = null;
        }
        return result;
    }

    private static  TimelineElementInternal mapTimelineInternal(TimelineElementInternal source ){
        TimelineElementInternal result;
        if( source != null) {
            result = new TimelineElementInternal();
            modelMapper.map(source, result );

            result = (TimelineElementInternal) postMappingTransformer.apply(source, result);
        } else {
            result = null;
        }
        return result;
    }


    public static TimelineElementInternal mapTimelineInternal(TimelineElementInternal source, Set<TimelineElementInternal> timelineElementInternalSet) {
        TimelineElementInternal result = mapTimelineInternal(source);

        if(result != null) {
            switch (result.getCategory()) {
                case SEND_ANALOG_PROGRESS -> {
                    SendAnalogProgressDetailsInt details = (SendAnalogProgressDetailsInt) result.getDetails();
                    log.debug("MAP TIMESTAMP: elem category {}, elem previous timestamp {}, elem new timestamp {}", result.getCategory(), result.getTimestamp(), details.getNotificationDate());
                    result.setTimestamp(details.getNotificationDate());
                }
                case SEND_ANALOG_FEEDBACK -> {
                    SendAnalogFeedbackDetailsInt details = (SendAnalogFeedbackDetailsInt) result.getDetails();
                    log.debug("MAP TIMESTAMP: elem category {}, elem previous timestamp {}, elem new timestamp {} ", result.getCategory(), result.getTimestamp(), details.getNotificationDate());
                    result.setTimestamp(details.getNotificationDate());
                }
                case SCHEDULE_REFINEMENT, ANALOG_SUCCESS_WORKFLOW, ANALOG_FAILURE_WORKFLOW, COMPLETELY_UNREACHABLE_CREATION_REQUEST, COMPLETELY_UNREACHABLE -> {
                    SendAnalogFeedbackDetailsInt details = findLastSendAnalogFeedbackDetails(result, timelineElementInternalSet);
                    if(details != null){
                        log.debug("MAP TIMESTAMP: elem category {}, elem previous timestamp {}, elem new timestamp {} ", result.getCategory(), result.getTimestamp(), details.getNotificationDate());
                        result.setTimestamp(details.getNotificationDate());
                    }else{
                        log.debug("SEARCH LAST SEND_ANALOG_FEEDBACK DETAILS NULL element {}",result);
                    }
                }
                case REFINEMENT -> {
                    ScheduleRefinementDetailsInt details = findScheduleRefinementDetails(result,timelineElementInternalSet);
                    if(details != null){
                        log.debug("MAP TIMESTAMP: elem category {}, elem previous timestamp {}, elem new timestamp {}", result.getCategory(), result.getTimestamp(), details.getSchedulingDate());
                        result.setTimestamp(details.getSchedulingDate());
                    }
                }
                default -> log.debug("NOTHING TO MAP: element category {} ", result.getCategory());
            }
        }

        return result;
    }

    private static ScheduleRefinementDetailsInt findScheduleRefinementDetails(TimelineElementInternal elementInternal, Set<TimelineElementInternal> timelineElementInternalSet) {
        int recIndex;

        if(elementInternal.getDetails() instanceof RecipientRelatedTimelineElementDetails relatedTimelineElementDetails){
            recIndex = relatedTimelineElementDetails.getRecIndex();
        }else{
            return null;
        }

        TimelineElementInternal scheduleRefinementTimelineElment = timelineElementInternalSet.stream().filter(e ->
                e.getCategory() == TimelineElementCategoryInt.SCHEDULE_REFINEMENT &&
                        e.getDetails() instanceof RecipientRelatedTimelineElementDetails scheduleRefinementTimelineElementDetails &&
                        scheduleRefinementTimelineElementDetails.getRecIndex() == recIndex
        ).findFirst().orElseThrow(() -> new PnInternalException("SCHEDULE_REFINEMENT NOT PRESENT, ERROR IN MAPPING", PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_TIMELINE_ELEMENT_NOT_PRESENT));

        return (ScheduleRefinementDetailsInt) scheduleRefinementTimelineElment.getDetails();
    }

    private static SendAnalogFeedbackDetailsInt findLastSendAnalogFeedbackDetails(TimelineElementInternal elementInternal, Set<TimelineElementInternal> timelineElementInternalSet) {
        int recIndex;

        if(elementInternal.getDetails() instanceof RecipientRelatedTimelineElementDetails relatedTimelineElementDetails){
            recIndex = relatedTimelineElementDetails.getRecIndex();
        }else{
            return null;
        }

        TimelineElementInternal lastSendAnalogFeedback = timelineElementInternalSet.stream().filter(e ->
                e.getCategory() == TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK &&
                        e.getDetails() instanceof SendAnalogFeedbackDetailsInt sendAnalogFeedbackDetails &&
                        sendAnalogFeedbackDetails.getRecIndex() == recIndex
        ).max((sendFeedback1, sendFeedback2) -> {
            SendAnalogFeedbackDetailsInt sendFeedback1Details = (SendAnalogFeedbackDetailsInt) sendFeedback1.getDetails();
            SendAnalogFeedbackDetailsInt sendFeedback2Details = (SendAnalogFeedbackDetailsInt) sendFeedback2.getDetails();
            return sendFeedback1Details.getNotificationDate().compareTo(sendFeedback2Details.getNotificationDate());
        }).orElse(null);

        if(lastSendAnalogFeedback != null){
            return (SendAnalogFeedbackDetailsInt)lastSendAnalogFeedback.getDetails();
        }

        return null;
    }

}
