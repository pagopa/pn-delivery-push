package it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementCategory;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.ActionEntity;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DtoToEntityActionMapper {
    private final ObjectMapper objectMapper;
    private final Map<TimelineElementCategory, ObjectWriter> objectWriters;

    public DtoToEntityActionMapper(ObjectMapper objectMapper, Map<TimelineElementCategory, ObjectWriter> objectWriters) {
        this.objectMapper = objectMapper;
        this.objectWriters = objectWriters;
    }

    public ActionEntity dtoToEntity(Action dto) {
        ActionEntity.ActionEntityBuilder builder =  ActionEntity.builder()
                .actionId(dto.getActionId())
                .notBefore(dto.getNotBefore())
                .recipientIndex(dto.getRecipientIndex())
                .type(dto.getType())
                .iun(dto.getIun());
/*
                .attachmentKeys(dto.getAttachmentKeys())
                .digitalAddressSource(dto.getDigitalAddressSource())
                .responseStatus(dto.getResponseStatus())
                .retryNumber(dto.getRetryNumber());


        if (dto.getNewPhysicalAddress() != null) {
            builder.newPhysicalAddress(
                    PhysicalAddressConv.builder()
                            .address(dto.getNewPhysicalAddress().getAddress())
                            .at(dto.getNewPhysicalAddress().getAt())
                            .addressDetails(dto.getNewPhysicalAddress().getAddressDetails())
                            .foreignState(dto.getNewPhysicalAddress().getForeignState())
                            .municipality(dto.getNewPhysicalAddress().getMunicipality())
                            .province(dto.getNewPhysicalAddress().getProvince())
                            .zip(dto.getNewPhysicalAddress().getZip())
                            .build()
            );
        }
*/
        
        return builder.build();
    }

}
