package it.pagopa.pn.deliverypush.springbootcfg;

import it.pagopa.pn.commons.abstractions.impl.AbstractCachedSsmParameterConsumer;
import it.pagopa.pn.commons.configs.IsMVPParameterConsumer;
import it.pagopa.pn.commons.configs.aws.AwsConfigs;
import it.pagopa.pn.commons.configs.aws.AwsServicesClientsConfig;
import it.pagopa.pn.commons.exceptions.ExceptionHelper;
import it.pagopa.pn.commons.exceptions.PnErrorWebExceptionHandler;
import it.pagopa.pn.commons.pnclients.RestTemplateFactory;
import it.pagopa.pn.commons.utils.ClockConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import software.amazon.awssdk.services.ssm.SsmClient;

@SpringBootTest
class ActivatorTest {
    @Autowired
    private AwsConfigs awsConfigs;
    @Autowired
    private AbstractCachedSsmParameterConsumer abstractCachedSsmParameterConsumer;
    @Autowired
    private SsmClient ssmClient;
    @Autowired
    private ExceptionHelper exceptionHelper;
    
    @Test
    void activatorTest(){
        Assertions.assertDoesNotThrow( ()  -> {
            new AbstractCachedSsmParameterConsumerActivation(ssmClient);
            new AwsConfigsActivation();
            new AwsServicesClientsConfigActivation(awsConfigs);
            new ClockConfigActivation();
            new IsMVPParameterConsumerActivation(abstractCachedSsmParameterConsumer);
            new PnErrorWebExceptionHandlerActivation(exceptionHelper);
            new PnResponseEntityExceptionHandlerActivation(exceptionHelper);
        });
    }
    
}