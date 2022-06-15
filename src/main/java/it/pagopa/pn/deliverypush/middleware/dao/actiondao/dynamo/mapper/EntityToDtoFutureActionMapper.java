package it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.mapper;

import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.entity.FutureActionEntity;
import org.springframework.stereotype.Component;

@Component
public class EntityToDtoFutureActionMapper {


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
                    PhysicalAddressInt.builder()
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
