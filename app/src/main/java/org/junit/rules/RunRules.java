package org.junit.rules;

import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class RunRules extends Statement {
    private final Statement statement;

    public RunRules(Statement base, Iterable<TestRule> rules, Description description) {
        this.statement = applyAll(base, rules, description);
    }

    public void evaluate() throws Throwable {
        this.statement.evaluate();
    }

    private static Statement applyAll(Statement result, Iterable<TestRule> rules, Description description) {
        for (TestRule each : rules) {
            result = each.apply(result, description);
        }
        return result;
    }
}
