package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.api.dto.addressbook.AddressBookEntry;
import it.pagopa.pn.api.dto.addressbook.DigitalAddresses;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.NotificationSender;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressType;
import it.pagopa.pn.api.dto.notification.address.PhysicalAddress;
import it.pagopa.pn.commons.pnclients.addressbook.AddressBook2;
import it.pagopa.pn.deliverypush.service.AddressBookService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

class AddressBookServiceImplTest {
    @Mock
    private AddressBook2 addressBook;

    AddressBookService service;

    @BeforeEach
    public void setup() {
        service = new AddressBookServiceImpl(addressBook);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void retrievePlatformAddress() {

        Notification notification = getNotification();
        NotificationSender sender = notification.getSender();
        NotificationRecipient recipient = notification.getRecipients().get(0);

        AddressBookEntry entry = AddressBookEntry.builder()
                .digitalAddresses(
                        DigitalAddresses.builder()
                                .platform(
                                        DigitalAddress.builder()
                                                .address("testAddress")
                                                .type(DigitalAddressType.PEC)
                                                .build()
                                ).build()
                ).build();

        Mockito.when(addressBook.getAddresses(Mockito.anyString(), Mockito.any(NotificationSender.class)))
                .thenReturn(Optional.of(entry));

        DigitalAddress address = service.retrievePlatformAddress(recipient, sender);
        Assertions.assertNotNull(address);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void retrievePlatformAddressNull() {

        Notification notification = getNotification();
        NotificationSender sender = notification.getSender();
        NotificationRecipient recipient = notification.getRecipients().get(0);

        Mockito.when(addressBook.getAddresses(Mockito.anyString(), Mockito.any(NotificationSender.class)))
                .thenReturn(Optional.empty());

        DigitalAddress address = service.retrievePlatformAddress(recipient, sender);
        Assertions.assertNull(address);
    }

    private Notification getNotification() {
        return Notification.builder()
                .iun("IUN_01")
                .paNotificationId("protocol_01")
                .subject("Subject 01")
                .cancelledByIun("IUN_05")
                .cancelledIun("IUN_00")
                .sender(NotificationSender.builder()
                        .paId(" pa_02")
                        .build()
                )
                .recipients(Collections.singletonList(
                        NotificationRecipient.builder()
                                .taxId("testIdRecipient")
                                .denomination("Nome Cognome/Ragione Sociale")
                                .digitalDomicile(DigitalAddress.builder()
                                        .type(DigitalAddressType.PEC)
                                        .address("account@dominio.it")
                                        .build())
                                .physicalAddress(PhysicalAddress.builder()
                                        .address("test address")
                                        .build())
                                .build()
                ))
                .build();
    }
}