package it.pagopa.pn.deliverypush.legalfacts;

import it.pagopa.pn.deliverypush.action.utils.AarUtils;
import it.pagopa.pn.deliverypush.utils.CheckRADDExperimentation;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class AarTemplateStrategyFactory {
    private final CheckRADDExperimentation checkRADDExperimentation;

    public AarTemplateChooseStrategy getAarTemplateStrategy(String baseAarTemplateType){
        if(AarUtils.needDynamicAarRADDDefinition(baseAarTemplateType)){
            return new DynamicRADDExperimentationChooseStrategy(checkRADDExperimentation);
        }else{
            final AarTemplateType aarTemplateType = AarTemplateType.valueOf(baseAarTemplateType);
            return new StaticAarTemplateChooseStrategy(aarTemplateType);
        }
    }
}
