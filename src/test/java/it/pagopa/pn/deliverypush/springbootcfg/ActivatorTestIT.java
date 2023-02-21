package it.pagopa.pn.deliverypush.springbootcfg;

import it.pagopa.pn.commons.abstractions.impl.AbstractCachedSsmParameterConsumer;
import it.pagopa.pn.commons.configs.aws.AwsConfigs;
import it.pagopa.pn.commons.exceptions.ExceptionHelper;
import it.pagopa.pn.deliverypush.LocalStackTestConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import software.amazon.awssdk.services.ssm.SsmClient;

@SpringBootTest
@Import(LocalStackTestConfig.class)
class ActivatorTestIT {
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
            new MVPParameterConsumerActivation(abstractCachedSsmParameterConsumer);
            new PnErrorWebExceptionHandlerActivation(exceptionHelper);
            new PnResponseEntityExceptionHandlerActivation(exceptionHelper);
        });
    }
    
}
