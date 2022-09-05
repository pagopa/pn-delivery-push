package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactsIdInt;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class LegalFactIdMapperTest {

    @Test
    void internalToExternal() {

        LegalFactIdMapper legalFactIdMapper = Mockito.mock(LegalFactIdMapper.class);

        try (MockedStatic<LegalFactIdMapper> mockedStatic = Mockito.mockStatic(LegalFactIdMapper.class)) {
            mockedStatic.when(() -> LegalFactIdMapper.internalToExternal(Mockito.any(LegalFactsIdInt.class))).thenReturn(legalFactIdMapper);
            // when(mockedLookupInstance.getCodeList("123", "456").thenReturn(yourMap):
        }
    }
}