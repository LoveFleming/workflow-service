package com.example.workflow;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class HardCodedExecutor {

    private final WebhookNode webhook;
    private final IfNode ifNode;
    private final SetNode nextStep;
    private final HttpRequestNode httpRequest;
    private final RespondToWebhookNode respond200;
    private final RespondToWebhookNode respond400;

    public HardCodedExecutor(WebhookNode webhook,
                             IfNode ifNode,
                             SetNode nextStep,
                             HttpRequestNode httpRequest,
                             RespondToWebhookNode respond200,
                             RespondToWebhookNode respond400) {
        this.webhook = webhook;
        this.ifNode = ifNode;
        this.nextStep = nextStep;
        this.httpRequest = httpRequest;
        this.respond200 = respond200;
        this.respond400 = respond400;
    }

    public Map<String, Object> execute(Map<String, Object> requestBody) {
        Map<String, Object> globals = new HashMap<>();

        // 1. Webhook
        webhook.execute(new ComponentContext(Map.of("payload", requestBody), globals));

        // 2. IF node
        Map<String, Object> query = (Map<String, Object>) requestBody.get("query");
        ComponentResult ifRes = ifNode.execute(new ComponentContext(
                Map.of("value1", query.get("email"), "value2", "hmchiud@tsmc.com"), globals));
        boolean ok = (boolean) ((Map<?, ?>) ifRes.data()).get("condition");

        if (ok) {
            // Success branch
            nextStep.execute(new ComponentContext(
                    Map.of("message", "Validation passed"), globals));
            httpRequest.execute(new ComponentContext(
                    Map.of("url", "http://localhost:5678/webhook-test/validate"), globals));
            return respond200.execute(new ComponentContext(
                    Map.of("responseCode", 200, "body", Map.of("email", query.get("email"))), globals)).data();
        } else {
            // Failure branch
            return respond400.execute(new ComponentContext(
                    Map.of("responseCode", 400,
                           "body", Map.of("error", "Bad request – invalid email")), globals)).data();
        }
    }
}
