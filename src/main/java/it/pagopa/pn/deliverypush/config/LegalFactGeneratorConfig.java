package it.pagopa.pn.deliverypush.config;

import it.pagopa.pn.deliverypush.action.utils.InstantNowSupplier;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.templatesengine.api.TemplateApi;
import it.pagopa.pn.deliverypush.legalfacts.CustomInstantWriter;
import it.pagopa.pn.deliverypush.legalfacts.DocumentComposition;
import it.pagopa.pn.deliverypush.legalfacts.LegalFactGeneratorFactoryImpl;
import it.pagopa.pn.deliverypush.legalfacts.PhysicalAddressWriter;
import it.pagopa.pn.deliverypush.legalfacts.generatorfactory.LegalFactGeneratorFactory;
import it.pagopa.pn.deliverypush.utils.PnSendModeUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LegalFactGeneratorConfig {

    private final CustomInstantWriter instantWriter;
    private final PhysicalAddressWriter physicalAddressWriter;
    private final PnDeliveryPushConfigs pnDeliveryPushConfigs;
    private final PnSendModeUtils pnSendModeUtils;
    private final TemplateApi templateEngineClient;

    private final DocumentComposition documentComposition;
    private final InstantNowSupplier instantNowSupplier;

    public LegalFactGeneratorConfig(
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

    @Bean
    @ConditionalOnProperty(name = "features.templates.client", havingValue = "default", matchIfMissing = true)
    public LegalFactGeneratorFactory legalFactGeneratorFactoryDefault() {
        return new LegalFactGeneratorFactoryImpl(
                instantWriter,
                physicalAddressWriter,
                pnDeliveryPushConfigs,
                pnSendModeUtils,
                templateEngineClient,
                documentComposition,
                instantNowSupplier
        );
    }

    @Bean
    @ConditionalOnProperty(name = "features.templates.client", havingValue = "experimental")
    public LegalFactGeneratorFactory legalFactGeneratorFactoryExperimental() {
        return new LegalFactGeneratorFactoryImpl(
                instantWriter,
                physicalAddressWriter,
                pnDeliveryPushConfigs,
                pnSendModeUtils,
                templateEngineClient,
                documentComposition,
                instantNowSupplier
        );
    }

}
