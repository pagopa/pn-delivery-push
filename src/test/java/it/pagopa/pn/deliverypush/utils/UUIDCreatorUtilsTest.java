package it.pagopa.pn.deliverypush.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


class UUIDCreatorUtilsTest {

    @Test
    void createUUIDTest() {
        assertThat(new UUIDCreatorUtils().createUUID())
                .isNotNull()
                .isInstanceOf(String.class);
    }
}
