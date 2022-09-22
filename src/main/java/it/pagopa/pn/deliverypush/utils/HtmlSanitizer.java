package it.pagopa.pn.deliverypush.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Class that performs via the {@link #sanitize(Object)} method a cleanup of input parameters,
 * escaping HTML element or deleting them, based on the {@link #sanitizeMode} property (ESCAPING or DELETE_HTML).
 * <p>
 * The sanitizeMode property can be valued by the env variable SANITIZE_MODE, or as a property called sanitize-mode
 * in the application.properties.
 * Priority is given to the ENV variable. If it is present neither as env variable nor as property in the file, it will
 * take the value of ESCAPING.
 * <p>
 * DELETE_HTML mode allows only determined HTML elements, based on the policies set in the constructor via the field {@link #policy}.
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

    private final SanitizeMode sanitizeMode;

    private final PolicyFactory policy;

    public HtmlSanitizer(ObjectMapper objectMapper, @Value("${sanitize-mode:ESCAPING}") SanitizeMode sanitizeMode) {
        this.objectMapper = objectMapper;
        this.sanitizeMode = sanitizeMode;
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
        HtmlSanitizerJsonParserDelegate htmlSanitizerJsonParserDelegate = new HtmlSanitizerJsonParserDelegate(traverse, policy, sanitizeMode);
        Object sanitizedObject = objectMapper.readValue(htmlSanitizerJsonParserDelegate, model.getClass());
        return sanitizedObject;
    }

    public Map doSanitize(Map modelMap) {
        if (CollectionUtils.isEmpty(modelMap)) {
            return modelMap;
        }

        Map<Object, Object> sanitizedMap = copyMap(modelMap);

        for (Map.Entry<Object, Object> entry : sanitizedMap.entrySet()) {
            Object sanitized = sanitize(entry.getValue());
            sanitizedMap.put(entry.getKey(), sanitized);
        }

        return sanitizedMap;

    }

    private Map copyMap(Map map) {
        if (map instanceof SortedMap) {
            return new TreeMap((SortedMap) map);
        }
        if (map instanceof ConcurrentMap) {
            return new ConcurrentHashMap(map);
        }
        if (map instanceof LinkedHashMap) {
            return new LinkedHashMap(map);
        }
        return new HashMap(map);
    }

    public Collection doSanitize(Collection collection) {
        if (CollectionUtils.isEmpty(collection)) {
            return collection;
        }

        Collection sanitizedCollection = createCollectionInstance(collection);

        for (Object o : collection) {
            Object sanitized = sanitize(o);
            sanitizedCollection.add(sanitized);
        }
        return sanitizedCollection;
    }

    private Collection createCollectionInstance(Collection collection) {
        if (collection instanceof Set) {
            return createSetInstance((Set) collection);
        }

        return createListInstance((List) collection);
    }

    private Set createSetInstance(Set set) {
        if (set instanceof SortedSet) {
            return new TreeSet();
        }
        if (set instanceof LinkedHashSet) {
            return new LinkedHashSet();
        }
        return new HashSet();
    }

    private List createListInstance(List list) {
        if (list instanceof AbstractSequentialList) {
            return new LinkedList();
        }
        return new ArrayList();
    }

    public enum SanitizeMode {
        ESCAPING,
        DELETE_HTML
    }
}
