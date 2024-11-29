package it.pagopa.pn.deliverypush.legalfacts.generatorfactory;

import it.pagopa.pn.deliverypush.legalfacts.LegalFactGeneratorDocComposition;
import it.pagopa.pn.deliverypush.legalfacts.LegalFactGeneratorTemplatesClient;

public interface LegalFactGeneratorFactory {

    LegalFactGeneratorTemplatesClient createTemplatesClient();

    LegalFactGeneratorDocComposition createLegalFactGeneratorDocComposition();
}
