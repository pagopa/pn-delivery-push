package it.pagopa.pn.deliverypush.utils;

import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactCategory;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactListElement;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactsId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LegalFactUtils {

    private LegalFactUtils() {
    }

    public static LegalFactListElement convert(LegalFactListElement element) {
        LegalFactListElement legalFactListElement = new LegalFactListElement();

        LegalFactsId legalFactsId = getLegalFactsId(element);
        legalFactListElement.setLegalFactsId(legalFactsId);
        legalFactListElement.setIun(element.getIun());
        legalFactListElement.setTaxId(element.getTaxId());

        return legalFactListElement;
    }

    private static LegalFactsId getLegalFactsId(LegalFactListElement element) {
        LegalFactsId legalFactsId = new LegalFactsId();
        legalFactsId.setKey(element.getLegalFactsId().getKey());
        LegalFactCategory category = element.getLegalFactsId().getCategory();

        legalFactsId.setCategory(category);

        return legalFactsId;
    }
}
