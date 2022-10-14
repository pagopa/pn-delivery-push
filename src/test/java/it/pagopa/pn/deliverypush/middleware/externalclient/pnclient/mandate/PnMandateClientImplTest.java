package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.mandate;

import it.pagopa.pn.deliverypush.dto.ext.mandate.MandateDtoInt;
import it.pagopa.pn.deliverypush.service.impl.MandateServiceImpl;
import it.pagopa.pn.mandate.generated.openapi.clients.mandate.model.InternalMandateDto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

class PnMandateClientImplTest {

    @Mock
    private PnMandateClient mandateClient;

    private MandateServiceImpl mandateService;

    @BeforeEach
    void setup() {
        mandateClient = Mockito.mock(PnMandateClient.class);
        mandateService = new MandateServiceImpl(mandateClient);
    }

    @Test
    void listMandatesByDelegate() {

        List<InternalMandateDto> listMandateDto = new ArrayList<>();
        InternalMandateDto dto = new InternalMandateDto();
        dto.setDelegate("001");
        listMandateDto.add(dto);

        Mockito.when(mandateClient.listMandatesByDelegate("001", "001")).thenReturn(listMandateDto);

        List<MandateDtoInt> response = mandateService.listMandatesByDelegate("001", "001");

        Assertions.assertEquals(1, response.size());

    }
}