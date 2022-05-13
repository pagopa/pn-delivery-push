package it.pagopa.pn.deliverypush.service.mapper;

import org.modelmapper.ModelMapper;

public class SmartMapper {
    private SmartMapper (){}
    
    public static  <S,T> T mapToClass(S source, Class<T> destinationClass ){
        return new ModelMapper().map(source, destinationClass );
    }
}
