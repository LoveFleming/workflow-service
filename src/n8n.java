/*
 * Core interfaces and base facilities for an n8n‑style workflow engine
 * ──────────────────────────────────────────────────────────────────────────────
 * Target: **Java 18 + Spring Boot 3.2.x** (JDK 17+ is sufficient for Boot, we
 * compile at --release 18 to match your request).
 *
 * Package layout (split into individual files in a real project – combined here
 * for brevity):
 *
 *   com.example.workflow.core
 *     ├── WorkflowComponent.java        – Generic component contract
 *     ├── ComponentConfig.java          – Configuration record
 *     ├── ExecutionContext.java         – Per‑run data + helpers
 *     ├── StepLogger.java               – Unified logger (swap with ES sink)
 *     ├── PlaceholderResolver.java      – "${var}" template resolver
 *     ├── AbstractComponent.java        – Base class with logging/metrics
 *     ├── ComponentRegistry.java        – Spring bean lookup helper
 *     ├── WorkflowExecutor.java         – Sequential executor w/ branching
 *     ├── GlobalExceptionHandler.java   – 500 mapping for REST + executor
 *     └── ComponentFailedException.java – Support exception
 *
 *   com.example.workflow.components
 *     └── HttpRequestComponent.java     – Example HTTP node
 *
 *   com.example.workflow.demo
 *     └── DemoApplication.java          – Minimal Spring Boot runner that
 *                                        assembles a tiny workflow and executes
 *                                        it on startup (Java 18 sample)
 */

// ─────────────────────────────────────────────────────────────────────────────
// core package
// ─────────────────────────────────────────────────────────────────────────────
package com.example.workflow.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generic n8n‑style node contract.
 */
public interface WorkflowComponent<I, O> {
    String getName();
    void configure(ComponentConfig config);
    O execute(ExecutionContext ctx, I input) throws ComponentFailedException;
}

/** Simple config wrapper (Java 16+ record). */
@Validated
public record ComponentConfig(Map<String, Object> values) {
    public ComponentConfig { values = Objects.requireNonNullElse(values, Map.of()); }
    public Object get(String key) { return values.get(key); }
    public String getAsString(String key) { return Objects.toString(values.get(key), null); }
}

/** Shared data available to every node in a single run. */
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
}

/** Logger abstraction – swap with Elasticsearch sink later. */
public interface StepLogger {
    void info(String step, String message);
    void error(String step, String message, Throwable t);
}

/** Console‑only stub implementation. */
public class ConsoleStepLogger implements StepLogger {
    @Override public void info(String step, String message) {
        System.out.printf("%s [INFO] [%s] %s%n", Instant.now(), step, message);
    }
    @Override public void error(String step, String message, Throwable t) {
        System.err.printf("%s [ERROR] [%s] %s – %s%n", Instant.now(), step, message, t);
    }
}

/** "${var}" placeholder resolver. */
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

/** Convenience base class with metrics + error wrapping. */
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

/** Runtime registry (auto‑filled by Spring @Component scan). */
@Component
public class ComponentRegistry {
    private final Map<String, WorkflowComponent<?, ?>> map = new HashMap<>();
    @Autowired public ComponentRegistry(Map<String, WorkflowComponent<?, ?>> beans) {
        beans.values().forEach(b -> map.put(b.getName(), b));
    }
    @SuppressWarnings("unchecked")
    public <I,O> WorkflowComponent<I,O> get(String name) { return (WorkflowComponent<I,O>) map.get(name); }
}

/** Sequential executor with simple branching predicate. */
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
            if (s.branchPredicate().test(ctx)) break; // naive short‑circuit
        }
    }
}

/** Exception wrapper. */
public class ComponentFailedException extends RuntimeException {
    private final String component; public ComponentFailedException(String component, Throwable cause) {
        super(cause); this.component = component; }
    public String getComponent() { return component; }
}

/** Spring MVC → RFC 7807 style error mapping. */
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

/* Domain helpers */
public record WorkflowStep(String name,String inputKey,String outputKey,java.util.function.Predicate<ExecutionContext> branchPredicate) {}
public record WorkflowDefinition(List<WorkflowStep> steps) {}

// ─────────────────────────────────────────────────────────────────────────────
// components package (example HTTP node)
// ─────────────────────────────────────────────────────────────────────────────
package com.example.workflow.components;

import com.example.workflow.core.*;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class HttpRequestComponent extends AbstractComponent<Map<String,Object>, String> {
    private final RestTemplate rest = new RestTemplate();
    @Override public String getName() { return "httpRequest"; }
    @Override protected String doExecute(ExecutionContext ctx, Map<String,Object> input) throws RestClientException {
        String url = resolver.resolve(config.getAsString("url"), ctx.variables);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<?> entity = new HttpEntity<>(input, headers);
        ResponseEntity<String> resp = rest.exchange(url, HttpMethod.POST, entity, String.class);
        return resp.getBody();
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// demo package – Java 18 sample showing usage
// ─────────────────────────────────────────────────────────────────────────────
package com.example.workflow.demo;

import com.example.workflow.components.HttpRequestComponent;
import com.example.workflow.core.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@SpringBootApplication(scanBasePackages = "com.example.workflow")
public class DemoApplication implements CommandLineRunner {
    private final WorkflowExecutor executor;
    @Autowired public DemoApplication(WorkflowExecutor executor) { this.executor = executor; }

    public static void main(String[] args) { SpringApplication.run(DemoApplication.class, args); }

    @Bean StepLogger stepLogger() { return new ConsoleStepLogger(); }

    /** Define workflow steps on startup and run once (for demo). */
    @Override public void run(String... args) {
        WorkflowStep httpStep = new WorkflowStep(
            "httpRequest",      // component name
            "payload",          // input key – we set below
            "responseBody",     // output key
            ctx -> false         // never branch‑abort
        );
        WorkflowDefinition def = new WorkflowDefinition(List.of(httpStep));

        // Prepare execution logger & context variable "payload"
        ExecutionContext initialCtx = new ExecutionContext("demo", stepLogger());
        initialCtx.put("payload", Map.of("hello", "world"));
        // Normally WorkflowExecutor creates context internally; here we just run quickly
        executor.run(UUID.randomUUID().toString(), def);
    }
}

/*
 * Build snippet (pom.xml):
 * ---------------------------------------------------------------------------
 * <properties>
 *   <java.version>18</java.version>
 * </properties>
 * <dependencyManagement>
 *   <dependencies>
 *     <dependency>
 *       <groupId>org.springframework.boot</groupId>
 *       <artifactId>spring-boot-dependencies</artifactId>
 *       <version>3.2.5</version>
 *       <type>pom</type>
 *       <scope>import</scope>
 *     </dependency>
 *   </dependencies>
 * </dependencyManagement>
 * <dependencies>
 *   <dependency>
 *     <groupId>org.springframework.boot</groupId>
 *     <artifactId>spring-boot-starter-web</artifactId>
 *   </dependency>
 *   <!-- add others only if used -->
 * </dependencies>
 * <build>
 *   <plugins>
 *     <plugin>
 *       <groupId>org.apache.maven.plugins</groupId>
 *       <artifactId>maven-compiler-plugin</artifactId>
 *       <configuration>
 *         <release>18</release>
 *       </configuration>
 *     </plugin>
 *   </plugins>
 * </build>
 */
