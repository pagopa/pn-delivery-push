package it.pagopa.pn.deliverypush.springbootcfg;

import it.pagopa.pn.commons.pnclients.RestTemplateRetryable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.web.client.RestTemplate;

class RestTemplateFactoryTest {

    @Mock
    private it.pagopa.pn.commons.pnclients.RestTemplateFactory restTemplateFactory;

    private RestTemplateFactory factory;

    @BeforeEach
    void setUp() {
        restTemplateFactory = Mockito.mock(it.pagopa.pn.commons.pnclients.RestTemplateFactory.class);
        factory = new RestTemplateFactory(restTemplateFactory);
    }

    @Test
    void restTemplateWithOffsetDateTimeFormatter() {
        Mockito.when(restTemplateFactory.restTemplateWithTracing(3, 10000)).thenReturn(new RestTemplateRetryable(3));
        RestTemplate template = factory.restTemplateWithOffsetDateTimeFormatter(3, 10000);
        Assertions.assertNotNull(template);
    }
}