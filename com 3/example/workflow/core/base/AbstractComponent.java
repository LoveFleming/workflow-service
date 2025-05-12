package com.example.workflow.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractComponent<I, O> implements WorkflowComponent<I, O> {
    protected ComponentConfig config;
    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final PlaceholderResolver resolver = new PlaceholderResolver();

    @Override public void configure(ComponentConfig config) {
        this.config = config;
    }

    @Override
    public final O execute(ExecutionContext ctx, I input) {
        long start = System.nanoTime();
        try {
            ctx.logger().info(getName(), "Started");
            O output = doExecute(ctx, input);
            ctx.logger().info(getName(), "Finished");
            return output;
        } catch (Exception e) {
            ctx.logger().error(getName(), "Failed", e);
            throw new ComponentFailedException(getName(), e);
        } finally {
            long dur = (System.nanoTime() - start) / 1_000_000;
            log.debug("{} finished in {} ms", getName(), dur);
        }
    }

    protected abstract O doExecute(ExecutionContext ctx, I input) throws Exception;
}