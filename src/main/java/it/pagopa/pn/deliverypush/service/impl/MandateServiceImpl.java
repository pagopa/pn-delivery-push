package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.dto.ext.mandate.MandateDtoInt;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.CxTypeAuthFleet;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.mandate.PnMandateClient;
import it.pagopa.pn.deliverypush.service.MandateService;
import it.pagopa.pn.deliverypush.service.mapper.MandateDtoMapper;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.mandate.model.InternalMandateDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class MandateServiceImpl implements MandateService {

    private final PnMandateClient mandateClient;

    public MandateServiceImpl(PnMandateClient mandateClient) {
        this.mandateClient = mandateClient;
    }

    @Override
    public List<MandateDtoInt> listMandatesByDelegate(String delegated,
                                                      String mandateId,
                                                      CxTypeAuthFleet cxType,
                                                      List<String> cxGroups) {

        List<InternalMandateDto> listMandateDto = mandateClient.listMandatesByDelegate(delegated, mandateId, convertToMandateCxType(cxType), cxGroups);
        
        return listMandateDto.stream()
                .map(MandateDtoMapper::externalToInternal)
                .toList();
    }

    private it.pagopa.pn.deliverypush.generated.openapi.msclient.mandate.model.CxTypeAuthFleet convertToMandateCxType(CxTypeAuthFleet cxType) {
        return it.pagopa.pn.deliverypush.generated.openapi.msclient.mandate.model.CxTypeAuthFleet.valueOf(cxType.getValue());
    }
}
