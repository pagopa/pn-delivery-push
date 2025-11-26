package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.*;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.timelineservice.model.TimelineElementDetails;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementDetailsV27;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;


@Slf4j
@Component
public class SmartMapper {
    private static ModelMapper modelMapper;
    private static BiFunction postMappingTransformer;

    static PropertyMap<NormalizedAddressDetailsInt, TimelineElementDetailsV27> addressDetailPropertyMap = new PropertyMap<>() {
        @Override
        protected void configure() {
            skip(destination.getNewAddress());
            skip(destination.getPhysicalAddress());
        }
    };


    static PropertyMap<PrepareAnalogDomicileFailureDetailsInt, TimelineElementDetailsV27> prepareAnalogDomicileFailureDetailsInt = new PropertyMap<>() {
        @Override
        protected void configure() {
            skip(destination.getPhysicalAddress());
        }
    };
    static Converter<TimelineElementInternal, TimelineElementInternal> timelineElementInternalTimestampConverter =
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

        modelMapper.createTypeMap(TimelineElementInternal.class, TimelineElementInternal.class).setPostConverter(timelineElementInternalTimestampConverter);

        List<BiFunction> postMappingTransformers = new ArrayList<>();
        postMappingTransformers.add( (source, result)-> {
            if (source instanceof TimelineElementDetailsInt && !(source instanceof NotificationCancelledDetailsInt) && result instanceof TimelineElementDetailsV27 resultCast){
                resultCast.setNotRefinedRecipientIndexes(null);
            }
            if (source instanceof TimelineElementDetailsInt && !(source instanceof PublicRegistryValidationCallDetailsInt) && result instanceof TimelineElementDetailsV27 resultCast){
                resultCast.setRecIndexes(null);
            }

            /*
                Le successive condizioni sono state aggiunte per gestire la conversione tra gli oggetti di TimelineElementDetails
                e TimelineElementDetailsV27, nell'ambito dell'API di history di delivery-push. Riprendono lo spirito delle
                precedenti condizioni, ma a causa della differenza di implementazione richiedono un trattamento diverso.
                E' necessario settare a null sull'oggetto result i campi notRefinedRecipientIndexes e recIndexes poichè
                essendo attributi required sull'openapi (schemas-pn-timeline.yaml) nei rispettivi schemi di details
                (PublicRegistryValidationCallDetails e NotificationCancelledDetails), la classe generata che li contiene
                (TimelineElementDetailsV27) cerca sempre di istanziarli come liste vuote, anche quando non sono previsti.
                In questo modo vengono rimappati a null per tutti gli elementi di timeline che non sono di tipo
                PUBLIC_REGISTRY_VALIDATION_CALL o NOTIFICATION_CANCELLED, cioè gli unici per i quali ha senso impostare questi 2 campi.
            */

            if(source instanceof TimelineElementDetails details && !details.getCategoryType().equals("NOTIFICATION_CANCELLED") && result instanceof TimelineElementDetailsV27 resultCast) {
                resultCast.setNotRefinedRecipientIndexes(null);
            }
            if(source instanceof TimelineElementDetails details && !details.getCategoryType().equals("PUBLIC_REGISTRY_VALIDATION_CALL") && result instanceof TimelineElementDetailsV27 resultCast) {
                resultCast.setRecIndexes(null);
            }

            return result;
        });

        postMappingTransformer =  postMappingTransformers.stream()
            .reduce((f, g) -> (i, s) -> f.apply(i, g.apply(i, s)))
            .get();
    }

    /*
        Mapping effettuato per la modifica dei timestamp per gli
        elementi di timeline che implementano l'interfaccia ElementTimestampTimelineElementDetails
     */
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
