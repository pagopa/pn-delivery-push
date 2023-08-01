package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.f24.PnF24Client;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;


class F24ServiceImplTest {

    private F24ServiceImpl f24Service;

    @Mock
    private PnF24Client pnF24Client;

    @BeforeEach
    void setup() {
        f24Service = new F24ServiceImpl(
                pnF24Client);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void validate() {
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void validateInvalidException() {
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void validateGenericException() {
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void generateAllPDF() {
        assertDoesNotThrow(() -> f24Service.generateAllPDF("REQUEST", "IUN", 0, 10));
    }
}