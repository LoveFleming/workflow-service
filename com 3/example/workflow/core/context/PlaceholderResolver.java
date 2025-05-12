package com.example.workflow.core;

import java.util.Map;
import java.util.regex.*;

public class PlaceholderResolver {
    private static final Pattern P = Pattern.compile("\$\{([a-zA-Z0-9_.-]+)}");

    public String resolve(String tpl, Map<String, Object> ctx) {
        Matcher m = P.matcher(tpl);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String key = m.group(1);
            Object val = ctx.getOrDefault(key, "");
            m.appendReplacement(sb, Matcher.quoteReplacement(val.toString()));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}