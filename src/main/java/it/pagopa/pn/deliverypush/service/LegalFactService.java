package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.api.dto.legalfacts.LegalFactsListEntry;

import java.util.List;

public interface LegalFactService {

    List<LegalFactsListEntry> getLegalFacts(String iun);
}
