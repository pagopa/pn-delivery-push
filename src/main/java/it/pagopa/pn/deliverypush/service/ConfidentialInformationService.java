package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.deliverypush.dto.ext.datavault.BaseRecipientDtoInt;
import reactor.core.publisher.Mono;

public interface ConfidentialInformationService {
    Mono<BaseRecipientDtoInt> getRecipientInformationByInternalId(String internalId);
}
