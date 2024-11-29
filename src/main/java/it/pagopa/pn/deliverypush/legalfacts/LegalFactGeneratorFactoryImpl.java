package it.pagopa.pn.deliverypush.legalfacts;

import it.pagopa.pn.deliverypush.action.utils.InstantNowSupplier;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.templatesengine.api.TemplateApi;
import it.pagopa.pn.deliverypush.legalfacts.generatorfactory.LegalFactGeneratorFactory;
import it.pagopa.pn.deliverypush.utils.PnSendModeUtils;
import org.springframework.stereotype.Component;

@Component
public class LegalFactGeneratorFactoryImpl implements LegalFactGeneratorFactory {

    private final CustomInstantWriter instantWriter;
    private final PhysicalAddressWriter physicalAddressWriter;
    private final PnDeliveryPushConfigs pnDeliveryPushConfigs;
    private final PnSendModeUtils pnSendModeUtils;
    private final TemplateApi templateEngineClient;

    private final DocumentComposition documentComposition;
    private final InstantNowSupplier instantNowSupplier;

    public LegalFactGeneratorFactoryImpl(
            CustomInstantWriter instantWriter,
            PhysicalAddressWriter physicalAddressWriter,
            PnDeliveryPushConfigs pnDeliveryPushConfigs,
            PnSendModeUtils pnSendModeUtils,
            TemplateApi templateEngineClient,
            DocumentComposition documentComposition,
            InstantNowSupplier instantNowSupplier
    ) {
        this.instantWriter = instantWriter;
        this.physicalAddressWriter = physicalAddressWriter;
        this.pnDeliveryPushConfigs = pnDeliveryPushConfigs;
        this.pnSendModeUtils = pnSendModeUtils;
        this.templateEngineClient = templateEngineClient;
        this.documentComposition = documentComposition;
        this.instantNowSupplier = instantNowSupplier;
    }

    @Override
    public LegalFactGeneratorTemplatesClient createTemplatesClient() {
        return new LegalFactGeneratorTemplatesClient(
                instantWriter,
                physicalAddressWriter,
                pnDeliveryPushConfigs,
                pnSendModeUtils,
                templateEngineClient
        );
    }

    @Override
    public LegalFactGeneratorDocComposition createLegalFactGeneratorDocComposition() {
        return new LegalFactGeneratorDocComposition(
                documentComposition,
                instantWriter,
                physicalAddressWriter,
                pnDeliveryPushConfigs,
                instantNowSupplier,
                pnSendModeUtils
        );
    }
}
