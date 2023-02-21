package it.pagopa.pn.deliverypush.dto.timeline.details;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.EnumSet;
class ServiceLevelIntTest {
    @ParameterizedTest
    @EnumSource(ServiceLevelInt.class)
    void test(ServiceLevelInt data) {
        EnumSet<ServiceLevelInt> datas = EnumSet.of(ServiceLevelInt.AR_REGISTERED_LETTER, ServiceLevelInt.REGISTERED_LETTER_890);
        Assertions.assertTrue(datas.contains(data));
    }
}