package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.dto.ext.mandate.MandateDtoInt;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.mandate.PnMandateClient;
import it.pagopa.pn.deliverypush.service.MandateService;
import it.pagopa.pn.deliverypush.service.mapper.MandateDtoMapper;
import it.pagopa.pn.mandate.generated.openapi.clients.mandate.model.InternalMandateDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MandateServiceImpl implements MandateService {
    private final PnMandateClient mandateClient;

    public MandateServiceImpl(PnMandateClient mandateClient) {
        this.mandateClient = mandateClient;
    }

    @Override
    public List<MandateDtoInt> listMandatesByDelegate(String delegated, String mandateId) {
        List<InternalMandateDto> listMandateDto = mandateClient.listMandatesByDelegate(delegated, mandateId);
        
        return listMandateDto.stream().map(
                MandateDtoMapper::externalToInternal
        ).collect(Collectors.toList());
    }
}
