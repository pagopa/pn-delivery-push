package it.pagopa.pn.deliverypush.service.mapper;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;

public class SmartMapper {
    private static ModelMapper modelMapper;

    private SmartMapper (){}

    static{
        modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
    }

    public static  <S,T> T mapToClass(S source, Class<T> destinationClass ){
        T result = null;
        if( source != null) {
            result = modelMapper.map(source, destinationClass );
        }
        return result;
    }


}
