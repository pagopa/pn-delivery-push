package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.dto.ext.datavault.BaseRecipientDtoInt;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.datavault.model.BaseRecipientDto;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.datavault.PnDataVaultClientReactive;
import it.pagopa.pn.deliverypush.service.ConfidentialInformationService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class ConfidentialInformationServiceImplTest {
    private ConfidentialInformationService confidentialInformationService;
    private PnDataVaultClientReactive pnDataVaultClientReactive;
    
    @BeforeEach
    void setup() {
        pnDataVaultClientReactive = Mockito.mock( PnDataVaultClientReactive.class );

        confidentialInformationService = new ConfidentialInformationServiceImpl(
                pnDataVaultClientReactive);

    }

    @Test
    void getRecipientInformationByInternalId() {
        //GIVEN
        String internalId = "internalId";
        String taxId = "testTaxId";
        String denomination = "denomination1";
        
        Flux<BaseRecipientDto> flux = Flux.just(BaseRecipientDto.builder()
                        .taxId(taxId)
                        .internalId(internalId)
                        .denomination(denomination)
                .build());
        Mockito.when(pnDataVaultClientReactive.getRecipientsDenominationByInternalId(Mockito.any())).thenReturn(flux);
        Mono<BaseRecipientDtoInt> monoBaseRec = confidentialInformationService.getRecipientInformationByInternalId(internalId);

        BaseRecipientDtoInt baseRecipientDto = monoBaseRec.block();
        Assertions.assertNotNull(baseRecipientDto);
        Assertions.assertEquals(taxId, baseRecipientDto.getTaxId());
        Assertions.assertEquals(denomination, baseRecipientDto.getDenomination());
    }
}