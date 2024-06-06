package it.pagopa.pn.deliverypush.legalfacts;

import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.utils.CheckRADDExperimentation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class RADDExperimentationChooseStrategyTest {
    @Mock
    private CheckRADDExperimentation checkRADDExperimentation;
    private RADDExperimentationChooseStrategy raddExperimentationChooseStrategy;
    
    @BeforeEach
    public void init(){
        raddExperimentationChooseStrategy = new RADDExperimentationChooseStrategy(checkRADDExperimentation);
    }
    @Test
    void choose() {
        PhysicalAddressInt address = PhysicalAddressInt.builder()
                .build();
        AarTemplateType aarTemplateType = raddExperimentationChooseStrategy.choose(address);
        
    }
}