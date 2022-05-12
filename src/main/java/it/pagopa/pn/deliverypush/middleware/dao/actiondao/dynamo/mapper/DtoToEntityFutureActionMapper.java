package it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementCategory;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.FutureActionEntity;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.PhysicalAddressConv;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DtoToEntityFutureActionMapper {
    private final ObjectMapper objectMapper;
    private final Map<TimelineElementCategory, ObjectWriter> objectWriters;

    public DtoToEntityFutureActionMapper(ObjectMapper objectMapper, Map<TimelineElementCategory, ObjectWriter> objectWriters) {
        this.objectMapper = objectMapper;
        this.objectWriters = objectWriters;
    }

    public FutureActionEntity dtoToEntity(Action dto, String timeSlot) {
        FutureActionEntity.FutureActionEntityBuilder builder = FutureActionEntity.builder()
                .timeSlot(timeSlot)
                .actionId(dto.getActionId())
                .attachmentKeys(dto.getAttachmentKeys())
                .digitalAddressSource(dto.getDigitalAddressSource())
                .notBefore(dto.getNotBefore())
                .recipientIndex(dto.getRecipientIndex())
                .responseStatus(dto.getResponseStatus())
                .retryNumber(dto.getRetryNumber())
                .type(dto.getType())
                .iun(dto.getIun());

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

        return builder.build();
    }

}
