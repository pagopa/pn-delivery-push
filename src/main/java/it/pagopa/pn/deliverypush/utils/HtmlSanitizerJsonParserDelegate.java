package it.pagopa.pn.deliverypush.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.util.JsonParserDelegate;
import org.apache.commons.text.StringEscapeUtils;
import org.owasp.html.PolicyFactory;

import java.io.IOException;

/**
 * Class that overrides the default behavior of JsonParserDelegate by sanitizing all fields of type String,
 * returned by the {@link #getText()} method.
 * <p>
 * It allows you to scroll through a JsonNode (via @{@link com.fasterxml.jackson.databind.JsonNode}.traverse),
 * and customize the parsing of each field.
 * <p>
 * Example usage:
 * <p>
 * JsonNode jsonNode = objectMapper.valueToTree(model);
 * JsonParser traverse = jsonNode.traverse();
 * HtmlSanitizerJsonParserDelegate htmlSanitizerJsonParserDelegate = new HtmlSanitizerJsonParserDelegate(traverse, policy);
 * Object sanitizedObject = objectMapper.readValue(htmlSanitizerJsonParserDelegate, model.getClass());
 */
public class HtmlSanitizerJsonParserDelegate extends JsonParserDelegate {

    private final PolicyFactory policy;


    public HtmlSanitizerJsonParserDelegate(JsonParser d, PolicyFactory policy) {
        super(d);
        this.policy = policy;
    }

    /**
     * @return a string sanitized from HTML elements by the PolicyFactory class.
     * <p>
     * The use of StringEscapeUtils.unescapeHtml4(sanitized) is necessary because PolicyFactory encodes special
     * characters such as apostrophe.
     * <p>
     * Example of the use of string with special characters:
     * String sanitized = policy.sanitize("via dell'Aquila"); //sanitized = "via dell&#39;Aquila"
     * StringEscapeUtils.unescapeHtml4("via dell&#39;Aquila"); // return "via dell'Aquila"
     * @throws IOException if an error occurs during parsing.
     */
    @Override
    public String getText() throws IOException {
        String text = this.delegate.getText();
        String sanitized = policy.sanitize(text);
        return StringEscapeUtils.unescapeHtml4(sanitized);
    }


}
