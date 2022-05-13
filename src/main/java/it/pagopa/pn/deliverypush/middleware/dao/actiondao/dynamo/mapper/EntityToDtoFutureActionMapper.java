package it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementCategory;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.FutureActionEntity;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class EntityToDtoFutureActionMapper {
    private final ObjectMapper objectMapper;
    private final Map<TimelineElementCategory, ObjectReader> objectReaders;

    public EntityToDtoFutureActionMapper(ObjectMapper objectMapper ) {
        this.objectMapper = objectMapper;
        this.objectReaders = new ConcurrentHashMap<>();
    }

    public Action entityToDto(FutureActionEntity entity ) {
        Action.ActionBuilder builder =  Action.builder()
                .actionId(entity.getActionId())
                .notBefore(entity.getNotBefore())
                .recipientIndex(entity.getRecipientIndex())
                .type(entity.getType())
                .iun(entity.getIun());
/*
                .attachmentKeys(entity.getAttachmentKeys())
                .digitalAddressSource(entity.getDigitalAddressSource())
                .responseStatus(entity.getResponseStatus())
                .retryNumber(entity.getRetryNumber())

        if(entity.getNewPhysicalAddress() != null){
            builder.newPhysicalAddress(
                    PhysicalAddress.builder()
                            .address(entity.getNewPhysicalAddress().getAddress())
                            .at(entity.getNewPhysicalAddress().getAt())
                            .addressDetails(entity.getNewPhysicalAddress().getAddressDetails())
                            .foreignState(entity.getNewPhysicalAddress().getForeignState())
                            .municipality(entity.getNewPhysicalAddress().getMunicipality())
                            .province(entity.getNewPhysicalAddress().getProvince())
                            .zip(entity.getNewPhysicalAddress().getZip())
                            .build()
            );
        }
*/
        
        return builder.build();
    }
}
