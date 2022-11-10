package it.pagopa.pn.deliverypush.dto.timeline.details;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.EnumSet;
class ContactPhaseIntTest {
    @ParameterizedTest
    @EnumSource(ContactPhaseInt.class)
    void test(ContactPhaseInt data) {
        EnumSet<ContactPhaseInt> datas = EnumSet.of(ContactPhaseInt.SEND_ATTEMPT, ContactPhaseInt.CHOOSE_DELIVERY);
        Assertions.assertTrue(datas.contains(data));
    }
}