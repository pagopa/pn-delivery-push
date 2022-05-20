package it.pagopa.pn.deliverypush.service.mapper;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;

public class SmartMapper {
    private SmartMapper (){}
    
    public static  <S,T> T mapToClass(S source, Class<T> destinationClass ){
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        return modelMapper.map(source, destinationClass );
    }
}
