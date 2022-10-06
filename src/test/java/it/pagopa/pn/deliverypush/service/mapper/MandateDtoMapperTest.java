package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.deliverypush.dto.ext.mandate.MandateDtoInt;
import it.pagopa.pn.mandate.generated.openapi.clients.mandate.model.InternalMandateDto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

class MandateDtoMapperTest {

    @Test
    void externalToInternal() {

        MandateDtoInt actual = MandateDtoMapper.externalToInternal(buildInternalMandateDto());

        Assertions.assertEquals(buildMandateDtoInt().getMandateId(), actual.getMandateId());
    }

    private MandateDtoInt buildMandateDtoInt() {
        Instant instant = Instant.parse("2022-08-30T16:04:13.913859900Z");
        List<String> ids = new ArrayList<>();
        ids.add("004");

        return MandateDtoInt.builder()
                .dateFrom(instant)
                .dateTo(instant)
                .mandateId("001")
                .delegate("002")
                .delegator("003")
                .visibilityIds(ids)
                .build();
    }

    private InternalMandateDto buildInternalMandateDto() {
        String date = "2022-08-30T16:04:13.913859900Z";
        List<String> ids = new ArrayList<>();
        ids.add("004");

        InternalMandateDto dto = new InternalMandateDto();
        dto.setDatefrom(date);
        dto.setDateto(date);
        dto.setMandateId("001");
        dto.setDelegate("002");
        dto.setDelegator("003");
        dto.setVisibilityIds(ids);
        return dto;
    }
}