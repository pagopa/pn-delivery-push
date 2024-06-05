package it.pagopa.pn.deliverypush.legalfacts;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class AarTemplateStrategyFactory {
    private final RADDExperimentationChooseStrategy raddExperimentationChooseStrategy;
    public static final String START_DYNAMIC_PROPERTY_CHARACTER = "<";

    public AarTemplateChooseStrategy getAarTemplateStrategy(String baseAarTemplateType){
        if(baseAarTemplateType.startsWith(START_DYNAMIC_PROPERTY_CHARACTER)){
            return raddExperimentationChooseStrategy;
        }else{
            final AarTemplateType aarTemplateType = AarTemplateType.valueOf(baseAarTemplateType);
            return new BasicAarTemplateChooseStrategy(aarTemplateType);
        }
    }
}
