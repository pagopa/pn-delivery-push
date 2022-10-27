package it.pagopa.pn.deliverypush.springbootcfg;

import it.pagopa.pn.commons.abstractions.impl.AbstractCachedSsmParameterConsumer;
import it.pagopa.pn.commons.configs.IsMVPParameterConsumer;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IsMVPParameterConsumerActivation extends IsMVPParameterConsumer {
    public IsMVPParameterConsumerActivation(AbstractCachedSsmParameterConsumer abstractCachedSsmParameterConsumer) {
        super(abstractCachedSsmParameterConsumer);
    }
}
