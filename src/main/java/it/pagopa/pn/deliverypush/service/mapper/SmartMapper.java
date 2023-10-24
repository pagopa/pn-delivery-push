package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.*;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementDetailsV20;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.modelmapper.TypeMap;
import org.modelmapper.convention.MatchingStrategies;

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

    static PropertyMap<SendDigitalProgressDetailsInt, TimelineElementDetailsV20> sendDigitalProgrressDetailPropertyMap = new PropertyMap<>() {
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
            timestampReplacer =
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
        modelMapper.addMappings(sendDigitalProgrressDetailPropertyMap);

        modelMapper.createTypeMap(TimelineElementInternal.class, TimelineElementInternal.class).setPostConverter(timestampReplacer);

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



}
