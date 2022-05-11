package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.delivery.generated.openapi.clients.delivery.model.RequestUpdateStatusDto;
import it.pagopa.pn.deliverypush.dto.ext.delivery.RequestUpdateStatusDtoInt;

public class RequestUpdateStatusDtoMapper {
    private RequestUpdateStatusDtoMapper(){}

    public static RequestUpdateStatusDto internalToExternal(RequestUpdateStatusDtoInt internalDto) {
        RequestUpdateStatusDto dto = new RequestUpdateStatusDto();

        dto.setIun(internalDto.getIun());
        dto.setNextStatus(it.pagopa.pn.delivery.generated.openapi.clients.delivery.model.NotificationStatus.valueOf(internalDto.getNextState().name()));

        return dto;
    }
}
