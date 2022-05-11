package it.pagopa.pn.deliverypush.middleware.actiondao.dynamo.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.PhysicalAddress;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementCategory;
import it.pagopa.pn.deliverypush.middleware.actiondao.dynamo.FutureActionEntity;
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
                .attachmentKeys(entity.getAttachmentKeys())
                .digitalAddressSource(entity.getDigitalAddressSource())
                .notBefore(entity.getNotBefore())
                .recipientIndex(entity.getRecipientIndex())
                .responseStatus(entity.getResponseStatus())
                .retryNumber(entity.getRetryNumber())
                .type(entity.getType())
                .iun(entity.getIun());

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
        
        return builder.build();
    }
}
