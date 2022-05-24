package it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.mapper;

import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.entity.ActionEntity;
import org.springframework.stereotype.Component;

@Component
public class DtoToEntityActionMapper {

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
