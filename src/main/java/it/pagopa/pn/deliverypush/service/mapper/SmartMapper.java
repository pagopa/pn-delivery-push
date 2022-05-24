package it.pagopa.pn.deliverypush.service.mapper;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;

public class SmartMapper {
    private SmartMapper (){}
    
    public static  <S,T> T mapToClass(S source, Class<T> destinationClass ){
        T result = null;
        if( source != null) {
            ModelMapper modelMapper = new ModelMapper();
            modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
            result = modelMapper.map(source, destinationClass );
        }
        return result;
    }
}
