package it.pagopa.pn.deliverypush.dto.timeline.details;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.EnumSet;
class DeliveryModeIntTest {
    @ParameterizedTest
    @EnumSource(DeliveryModeInt.class)
    void test(DeliveryModeInt data) {
        EnumSet<DeliveryModeInt> datas = EnumSet.of(DeliveryModeInt.DIGITAL, DeliveryModeInt.ANALOG);
        Assertions.assertTrue(datas.contains(data));
    }
}