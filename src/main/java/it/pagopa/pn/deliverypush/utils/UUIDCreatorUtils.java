package it.pagopa.pn.deliverypush.utils;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UUIDCreatorUtils {

    public String createUUID() {
        return UUID.randomUUID().toString();
    }

}
