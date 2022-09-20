package it.pagopa.pn.deliverypush.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.deliverypush.legalfacts.PhysicalAddressWriter;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.time.temporal.Temporal;
import java.util.*;

/**
 * Class that performs via the {@link #sanitize(Object)} method a cleanup of input parameters,
 * allowing only determined HTML elements, based on the policies set in the constructor via the field {@link #policy}.
 *
 * Classes that do not need to be sanitized should be handled in the {@link #canSanitize(Object)} method.
 */
@Component
public class HtmlSanitizer {


    private final ObjectMapper objectMapper;

    private final PolicyFactory policy;

    public HtmlSanitizer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.policy = new HtmlPolicyBuilder().allowElements("").toFactory();
    }

    public Object sanitize(Object model) {
        try {
            if(canSanitize(model)) {
                if(model instanceof Map) {
                    return doSanitize((Map) model);
                }
                if(model instanceof Collection) {
                    return doSanitize((Collection) model);
                }

                return doSanitize(model);
            }

            else {
                return model;
            }

        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Object doSanitize(Object model) throws IOException {
        JsonNode jsonNode = objectMapper.valueToTree(model);
        JsonParser traverse = jsonNode.traverse();
        HtmlSanitizerJsonParserDelegate htmlSanitizerJsonParserDelegate = new HtmlSanitizerJsonParserDelegate(traverse, policy);
        Object notificationInt1 = objectMapper.readValue(htmlSanitizerJsonParserDelegate, model.getClass());
        return notificationInt1;
    }

    public Map<String, Object> doSanitize(Map modelMap) {
        if(CollectionUtils.isEmpty(modelMap)) {
            return modelMap;
        }

        Map<String, Object> sanitizedMap = new HashMap<>(modelMap);

        for(Map.Entry<String, Object> entry: sanitizedMap.entrySet()) {
            Object sanitized = sanitize(entry.getValue());
            sanitizedMap.put(entry.getKey(), sanitized);
        }

        return sanitizedMap;

    }

    public Collection doSanitize(Collection collection) {
        if(CollectionUtils.isEmpty(collection)) {
            return collection;
        }

        List sanitizedList = new ArrayList();

        for (Object o: collection) {
            Object sanitized = sanitize(o);
            sanitizedList.add(sanitized);
        }
        return sanitizedList;
    }

    private boolean canSanitize(Object model) {
        if(
                model instanceof Temporal ||
                model instanceof Number ||
                model instanceof PhysicalAddressWriter

        )
        {
            return false;
        }
        return true;
    }
}
