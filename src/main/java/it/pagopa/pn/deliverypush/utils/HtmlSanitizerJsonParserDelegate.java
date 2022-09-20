package it.pagopa.pn.deliverypush.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.util.JsonParserDelegate;
import org.apache.commons.text.StringEscapeUtils;
import org.owasp.html.PolicyFactory;

import java.io.IOException;

public class HtmlSanitizerJsonParserDelegate extends JsonParserDelegate {

    private final PolicyFactory policy;



    public HtmlSanitizerJsonParserDelegate(JsonParser d, PolicyFactory policy) {
        super(d);
        this.policy = policy;
    }

    @Override
    public String getText() throws IOException {
        String text = this.delegate.getText();
        String sanitized = policy.sanitize(text);
        return StringEscapeUtils.unescapeHtml4(sanitized);
//        return sanitized;
    }




}
