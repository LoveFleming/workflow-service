package com.example.workflow;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class HttpRequestNode implements WorkflowComponent {

    private final RestTemplate rest = new RestTemplate();

    @Override
    public ComponentResult execute(ComponentContext ctx) {
        String url = (String) ctx.inputs().get("url");
        try {
            ResponseEntity<String> resp = rest.getForEntity(url, String.class);
            ctx.globals().put("http_response", resp.getBody());
            return ComponentResult.ok(resp.getBody());
        } catch (Exception ex) {
            return ComponentResult.error(ex.getMessage());
        }
    }
}
