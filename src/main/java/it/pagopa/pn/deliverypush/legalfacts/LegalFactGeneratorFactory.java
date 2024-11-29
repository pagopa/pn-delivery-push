package it.pagopa.pn.deliverypush.legalfacts;

public interface LegalFactGeneratorFactory {

    LegalFactGeneratorTemplatesClient createTemplatesClient();

    LegalFactGenerator createLegalFactGenerator();
}
