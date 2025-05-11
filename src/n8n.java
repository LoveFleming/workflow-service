/*
 * n8n‑style workflow engine – **Java 18 + Spring Boot 3.2.x**
 * ──────────────────────────────────────────────────────────────────────────────
 * NEW: OpenTelemetry trace propagation
 *   • Every component can access the current **trace ID** via ExecutionContext#getOtelTraceId().
 *   • HttpRequestComponent automatically adds the trace ID header (default **X‑Trace-Id**, overridable).
 *   • No extra boilerplate required – OpenTelemetry instrumentation on your app/server populates Span.current().
 *
 * Package overview (one class per file IRL – merged here for brevity):
 *
 *   com.example.workflow.core
 *     ├── WorkflowComponent.java
 *     ├── ComponentConfig.java
 *     ├── ExecutionContext.java        ← trace‑ID helper added
 *     ├── StepLogger.java | ConsoleStepLogger.java
 *     ├── PlaceholderResolver.java
 *     ├── AbstractComponent.java
 *     ├── ComponentRegistry.java
 *     ├── WorkflowExecutor.java
 *     ├── ComponentFailedException.java
 *     └── GlobalExceptionHandler.java
 *
 *   com.example.workflow.components
 *     └── HttpRequestComponent.java    ← sets trace header
 *
 *   com.example.workflow.demo
 *     └── DemoApplication.java         (unchanged)
 */

// ─────────────────────────────────────────────────────────────────────────────
// core package
// ─────────────────────────────────────────────────────────────────────────────
package com.example.workflow.core;

import io.opentelemetry.api.trace.Span;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface WorkflowComponent<I, O> {
    String getName();
    void configure(ComponentConfig config);
    O execute(ExecutionContext ctx, I input) throws ComponentFailedException;
}

@Validated
public record ComponentConfig(Map<String, Object> values) {
    public ComponentConfig { values = Objects.requireNonNullElse(values, Map.of()); }
    public Object get(String key) { return values.get(key); }
    public String getAsString(String key) { return Objects.toString(values.get(key), null); }
}

public class ExecutionContext {
    final Map<String, Object> variables = new HashMap<>();
    final StepLogger logger;
    final String correlationId;

    public ExecutionContext(String correlationId, StepLogger logger) {
        this.correlationId = correlationId;
        this.logger = logger;
    }

    public Object get(String key) { return variables.get(key); }
    public void put(String key, Object value) { variables.put(key, value); }
    public String getCorrelationId() { return correlationId; }
    public StepLogger logger() { return logger; }

    /**
     * Returns the OpenTelemetry traceId of the **current Span** or null if none.
     * Components can use this for downstream headers/logging.
     */
    public String getOtelTraceId() {
        var span = Span.current();
        var sc = span.getSpanContext();
        return sc.isValid() ? sc.getTraceId() : null;
    }
}

public interface StepLogger {
    void info(String step, String message);
    void error(String step, String message, Throwable t);
}

public class ConsoleStepLogger implements StepLogger {
    @Override public void info(String step, String message) {
        System.out.printf("%s [INFO] [%s] %s%n", Instant.now(), step, message);
    }
    @Override public void error(String step, String message, Throwable t) {
        System.err.printf("%s [ERROR] [%s] %s – %s%n", Instant.now(), step, message, t);
    }
}

public class PlaceholderResolver {
    private static final Pattern P = Pattern.compile("\\$\\{([a-zA-Z0-9_.-]+)}");
    public String resolve(String tpl, Map<String, Object> ctx) {
        Matcher m = P.matcher(tpl);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String key = m.group(1);
            Object val = ctx.getOrDefault(key, "");
            m.appendReplacement(sb, Matcher.quoteReplacement(Objects.toString(val)));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}

public abstract class AbstractComponent<I, O> implements WorkflowComponent<I, O> {
    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected ComponentConfig config;
    @Autowired(required = false) protected PlaceholderResolver resolver = new PlaceholderResolver();
    @Override public void configure(ComponentConfig config) { this.config = config; }
    @Override public final O execute(ExecutionContext ctx, I input) {
        long start = System.nanoTime();
        try {
            ctx.logger().info(getName(), "Starting");
            O out = doExecute(ctx, input);
            ctx.logger().info(getName(), "Completed");
            return out;
        } catch (Exception e) {
            ctx.logger().error(getName(), "Failed", e);
            throw new ComponentFailedException(getName(), e);
        } finally {
            long dur = (System.nanoTime() - start) / 1_000_000;
            log.debug("{} finished in {} ms", getName(), dur);
        }
    }
    protected abstract O doExecute(ExecutionContext ctx, I input) throws Exception;
}

@Component
public class ComponentRegistry {
    private final Map<String, WorkflowComponent<?, ?>> map = new HashMap<>();
    @Autowired public ComponentRegistry(Map<String, WorkflowComponent<?, ?>> beans) {
        beans.values().forEach(b -> map.put(b.getName(), b));
    }
    @SuppressWarnings("unchecked")
    public <I,O> WorkflowComponent<I,O> get(String name) { return (WorkflowComponent<I,O>) map.get(name); }
}

public class WorkflowExecutor {
    private final ComponentRegistry registry; private final StepLogger logger;
    public WorkflowExecutor(ComponentRegistry registry, StepLogger logger) {
        this.registry = registry; this.logger = logger; }
    public void run(String correlationId, WorkflowDefinition def) {
        ExecutionContext ctx = new ExecutionContext(correlationId, logger);
        for (WorkflowStep s : def.steps()) {
            WorkflowComponent<Object,Object> c = registry.get(s.name());
            Object in = ctx.get(s.inputKey());
            Object out = c.execute(ctx, in);
            ctx.put(s.outputKey(), out);
            if (s.branchPredicate().test(ctx)) break;
        }
    }
}

public class ComponentFailedException extends RuntimeException {
    private final String component; public ComponentFailedException(String component, Throwable cause) { super(cause); this.component = component; }
    public String getComponent() { return component; }
}

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ComponentFailedException.class)
    public ResponseEntity<Map<String,Object>> handle(ComponentFailedException ex) {
        Map<String,Object> body = Map.of(
            "type", "about:blank",
            "title", "Component failed",
            "component", ex.getComponent(),
            "detail", ex.getCause() == null ? ex.getMessage() : ex.getCause().getMessage()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}

public record WorkflowStep(String name,String inputKey,String outputKey,java.util.function.Predicate<ExecutionContext> branchPredicate) {}
public record WorkflowDefinition(List<WorkflowStep> steps) {}

// ─────────────────────────────────────────────────────────────────────────────
// components package
// ─────────────────────────────────────────────────────────────────────────────
package com.example.workflow.components;

import com.example.workflow.core.*;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class HttpRequestComponent extends AbstractComponent<Map<String,Object>, String> {
    private final RestTemplate rest = new RestTemplate();
    @Override public String getName() { return "httpRequest"; }

    @Override protected String doExecute(ExecutionContext ctx, Map<String,Object> input) {
        String url = resolver.resolve(config.getAsString("url"), ctx.variables);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Trace header (configurable, default X‑Trace‑Id)
        String headerName = Objects.requireNonNullElse(config.getAsString("traceHeader"), "X-Trace-Id");
        String traceId = ctx.getOtelTraceId();
        if (traceId != null) headers.set(headerName, traceId);

        HttpEntity<?> entity = new HttpEntity<>(input, headers);
        ResponseEntity<String> resp = rest.exchange(url, HttpMethod.POST, entity, String.class);
        return resp.getBody();
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// demo package remains the same (omitted for brevity)
// ─────────────────────────────────────────────────────────────────────────────

/* Maven additions for OpenTelemetry API (only compile scope needed if runtime provided elsewhere):
 * <dependency>
 *   <groupId>io.opentelemetry</groupId>
 *   <artifactId>opentelemetry-api</artifactId>
 *   <version>1.40.0</version>
 * </dependency>
 */
