package com.cadence.codingassessmentservice.execution;

/**
 * Strategy interface -- one implementation per sandboxed execution
 * backend. Actually compiling and running untrusted candidate code is
 * a real security boundary, so this service never executes code
 * in-process; it delegates to an external sandbox (Judge0 today).
 * CodeExecutionProviderFactory (see the strategy/ package) is the
 * context that selects one at runtime, same pattern as the AI
 * provider factories -- kept as its own small abstraction even with a
 * single implementation today, so adding a second execution backend
 * later is a config change, not a rewrite.
 */
public interface CodeExecutionProvider {

    ExecutionResult execute(ExecutionRequest request);

    String getProviderName();
}
