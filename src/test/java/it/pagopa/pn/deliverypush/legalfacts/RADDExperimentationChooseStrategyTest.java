package it.pagopa.pn.deliverypush.legalfacts;

import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.utils.CheckRADDExperimentation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RADDExperimentationChooseStrategyTest {
    @Mock
    private CheckRADDExperimentation checkRADDExperimentation;
    private DynamicRADDExperimentationChooseStrategy raddExperimentationChooseStrategy;
    
    @BeforeEach
    public void init(){
        raddExperimentationChooseStrategy = new DynamicRADDExperimentationChooseStrategy(checkRADDExperimentation);
    }
    @Test
    void chooseRAADalt() {
        //GIVEN
        Mockito.when(checkRADDExperimentation.checkAddress(Mockito.any(PhysicalAddressInt.class)))
                .thenReturn(true);
        PhysicalAddressInt address = PhysicalAddressInt.builder()
                .build();
        
        //WHEN
        AarTemplateType aarTemplateType = raddExperimentationChooseStrategy.choose(address);
        //THEN
        Assertions.assertEquals(AarTemplateType.AAR_NOTIFICATION_RADD_ALT, aarTemplateType);
    }

    @Test
    void chooseDefault() {
        //GIVEN
        Mockito.when(checkRADDExperimentation.checkAddress(Mockito.any(PhysicalAddressInt.class)))
                .thenReturn(false);
        PhysicalAddressInt address = PhysicalAddressInt.builder()
                .build();

        //WHEN
        AarTemplateType aarTemplateType = raddExperimentationChooseStrategy.choose(address);
        //THEN
        Assertions.assertEquals(AarTemplateType.AAR_NOTIFICATION, aarTemplateType);
    }
}