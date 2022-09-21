package it.pagopa.pn.deliverypush.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.*;

/**
 * Class that performs via the {@link #sanitize(Object)} method a cleanup of input parameters,
 * allowing only determined HTML elements, based on the policies set in the constructor via the field {@link #policy}.
 * <p>
 * For example, if you wanted to allow only the HTML img element in the string, you could use:
 * PolicyFactory policy = Sanitizers.IMAGES;
 * or if you want no one HTML element: PolicyFactory policy = new HtmlPolicyBuilder().allowElements("").toFactory();
 * <p>
 * Example of usage with no HTML element policy:
 * <p>
 * String sanitized = policy.sanitize("<html><h1>SSRF WITH IMAGE POC</h1> <img src='https://prova.it'></img></html>");
 * // sanitized value is "SSRF WITH IMAGE POC".
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

            if (model instanceof Map) {
                return doSanitize((Map) model);
            }
            if (model instanceof Collection) {
                return doSanitize((Collection) model);
            }

            return doSanitize(model);


        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Object doSanitize(Object model) throws IOException {
        if (model == null) return null;

        JsonNode jsonNode = objectMapper.valueToTree(model);
        JsonParser traverse = jsonNode.traverse();
        HtmlSanitizerJsonParserDelegate htmlSanitizerJsonParserDelegate = new HtmlSanitizerJsonParserDelegate(traverse, policy);
        Object sanitizedObject = objectMapper.readValue(htmlSanitizerJsonParserDelegate, model.getClass());
        return sanitizedObject;
    }

    public Map<String, Object> doSanitize(Map modelMap) {
        if (CollectionUtils.isEmpty(modelMap)) {
            return modelMap;
        }

        Map<String, Object> sanitizedMap = new HashMap<>(modelMap);

        for (Map.Entry<String, Object> entry : sanitizedMap.entrySet()) {
            Object sanitized = sanitize(entry.getValue());
            sanitizedMap.put(entry.getKey(), sanitized);
        }

        return sanitizedMap;

    }

    public Collection doSanitize(Collection collection) {
        if (CollectionUtils.isEmpty(collection)) {
            return collection;
        }

        List sanitizedList = new ArrayList();

        for (Object o : collection) {
            Object sanitized = sanitize(o);
            sanitizedList.add(sanitized);
        }
        return sanitizedList;
    }
}
