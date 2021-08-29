package it.pagopa.pn.deliverypush.actions;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.abstractions.FileStorage;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.util.Collections;
import java.util.Map;

@Component
public class LegalFactUtils {

    private final FileStorage fileStorage;
    private final ObjectMapper objMapper;

    public LegalFactUtils(FileStorage fileStorage, ObjectMapper objMapper) {
        this.fileStorage = fileStorage;
        this.objMapper = objMapper;
    }

    public  void saveLegalFact(String iun, String name, Object legalFact ) {
        try {
            String key = iun + "/legalfacts/" + name + ".json";
            String bodyString = objMapper.writeValueAsString( legalFact );
            Map<String, String> metadata = Collections.singletonMap( "Content-Type", "application/json; charset=utf-8");

            byte[] body = bodyString.getBytes(StandardCharsets.UTF_8);
            try( InputStream bodyStream = new ByteArrayInputStream( body )) {
                fileStorage.putFileVersion(key, bodyStream, body.length, metadata);
            }
        } catch ( IOException exc) {
            throw new PnInternalException("Generating legal fact", exc);
        }
    }

    public String instantToDate(Instant instant) {
        OffsetDateTime odt = instant.atOffset( ZoneOffset.UTC );
        int year = odt.get( ChronoField.YEAR_OF_ERA );
        int month = odt.get( ChronoField.MONTH_OF_YEAR );
        int day = odt.get( ChronoField.DAY_OF_MONTH );
        return String.format("%04d-%02d-%02d", year, month, day);
    }
}
