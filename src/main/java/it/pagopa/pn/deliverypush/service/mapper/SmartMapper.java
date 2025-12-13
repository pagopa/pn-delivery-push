package it.pagopa.pn.deliverypush.service.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.timelineservice.model.NotificationCancelledDetails;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.timelineservice.model.PublicRegistryValidationCallDetails;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.timelineservice.model.TimelineElementDetails;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementDetailsV27;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;


@Slf4j
@Component
public class SmartMapper {
    private final ObjectMapper objectMapper;
    private final ModelMapper modelMapper;
    private final BiFunction postMappingTransformer;

    public SmartMapper(ObjectMapper objectMapper, ModelMapper modelMapper) {
        this.objectMapper = objectMapper;
        this.modelMapper = modelMapper;
        this.postMappingTransformer = this.initPostMappingTransformer();
    }


    public <S,T> T mapToClass(S source, Class<T> destinationClass ){
        T result;
        if( source != null) {
            result = modelMapper.map(source, destinationClass );
            result = (T) postMappingTransformer.apply(source, result);
        } else {
            result = null;
        }
        return result;
    }

    public  <S,T> T mapToClassWithObjectMapper(S source, Class<T> destinationClass ){
        try {
            return objectMapper.readValue(objectMapper.writeValueAsBytes(source), destinationClass);
        } catch (IOException e) {
            throw new PnInternalException("Errore durante il mapping del dettaglio", "MAPPING_ERROR", e);
        }
    }

    private BiFunction initPostMappingTransformer() {
        List<BiFunction> postMappingTransformers = new ArrayList<>();
        postMappingTransformers.add( (source, result)-> {
            if (source instanceof TimelineElementDetails && !(source instanceof NotificationCancelledDetails) && result instanceof TimelineElementDetailsV27 resultCast){
                resultCast.setNotRefinedRecipientIndexes(null);
            }
            if (source instanceof TimelineElementDetails && !(source instanceof PublicRegistryValidationCallDetails) && result instanceof TimelineElementDetailsV27 resultCast){
                resultCast.setRecIndexes(null);
            }

            return result;
        });

        return postMappingTransformers.stream()
                .reduce((f, g) -> (i, s) -> f.apply(i, g.apply(i, s)))
                .get();
    }
}
