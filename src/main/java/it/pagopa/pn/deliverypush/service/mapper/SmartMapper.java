package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.deliverypush.dto.timeline.details.NormalizedAddressDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.NotificationCancelledDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.PrepareAnalogDomicileFailureDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementDetailsInt;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementDetailsV20;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import org.modelmapper.Condition;
import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.modelmapper.convention.MatchingStrategies;
import org.modelmapper.spi.MappingContext;

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


    static PropertyMap<PrepareAnalogDomicileFailureDetailsInt, TimelineElementDetailsV20> prepareAnalogDomicileFailureDetailsInt = new PropertyMap<>() {
        @Override
        protected void configure() {
            skip(destination.getPhysicalAddress());
        }
    };


    static Condition<TimelineElementDetailsInt, TimelineElementDetailsV20> notCancellation = new Condition<TimelineElementDetailsInt, TimelineElementDetailsV20>() {
        @Override
        public boolean applies(MappingContext<TimelineElementDetailsInt, TimelineElementDetailsV20> context) {
            return !(context.getSource() instanceof NotificationCancelledDetailsInt);
        }
    };

    static{
        modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        modelMapper.addMappings(addressDetailPropertyMap);
        modelMapper.addMappings(prepareAnalogDomicileFailureDetailsInt);

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
