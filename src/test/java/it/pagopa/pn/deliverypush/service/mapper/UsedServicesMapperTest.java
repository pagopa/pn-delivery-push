package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.UsedServicesInt;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.delivery.model.UsedServices;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class UsedServicesMapperTest {
    @Test
    void testExternalToInternal() {
        UsedServices external = new UsedServices();
        external.setPhysicalAddressLookup(true);

        UsedServicesInt internal = UsedServicesMapper.externalToInternal(external);

        Assertions.assertNotNull(internal);
        Assertions.assertTrue(internal.getPhysicalAddressLookUp());
    }

    @Test
    void testExternalToInternalWithNull() {
        UsedServices external = null;
        UsedServicesInt internal = UsedServicesMapper.externalToInternal(external);

        Assertions.assertNull(internal);
    }
}
