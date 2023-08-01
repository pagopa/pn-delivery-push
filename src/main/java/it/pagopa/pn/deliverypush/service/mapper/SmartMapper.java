package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.deliverypush.dto.timeline.details.NormalizedAddressDetailsInt;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NormalizedAddressDetails;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementDetails;
import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.boot.autoconfigure.security.SecurityProperties.User;

public class SmartMapper {
    private static ModelMapper modelMapper;

    private SmartMapper (){}


    static PropertyMap<NormalizedAddressDetailsInt, TimelineElementDetails> addressDetailPropertyMap = new PropertyMap<NormalizedAddressDetailsInt,TimelineElementDetails>() {
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
