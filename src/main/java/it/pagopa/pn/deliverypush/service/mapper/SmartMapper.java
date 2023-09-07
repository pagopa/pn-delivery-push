package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.deliverypush.dto.timeline.details.NormalizedAddressDetailsInt;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementDetailsV20;
import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.modelmapper.convention.MatchingStrategies;

public class SmartMapper {
    private static ModelMapper modelMapper;

    private SmartMapper (){}


    static PropertyMap<NormalizedAddressDetailsInt, TimelineElementDetailsV20> addressDetailPropertyMap = new PropertyMap<>() {
        @Override
        protected void configure() {
            skip(destination.getNewAddress());
            skip(destination.getPhysicalAddress());
        }
    };

    static{
        modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        modelMapper.addMappings(addressDetailPropertyMap);
    }

    public static  <S,T> T mapToClass(S source, Class<T> destinationClass ){
        T result = null;
        if( source != null) {
            result = modelMapper.map(source, destinationClass );
        }
        return result;
    }


}
