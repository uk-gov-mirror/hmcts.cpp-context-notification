package uk.gov.moj.cpp.notification.command.api;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.kie.api.KieServices;

/**
 * BC-20 guard (J17 -> J25 parity, see /J25-PARITY-FINDINGS.md).
 *
 * The Drools access-control test harness can silently load 0 rules (e.g. a Drools 7->10 / MVEL packaging
 * shift), which makes the deny-tests pass vacuously. This asserts the kbase actually compiled at least one
 * rule, so a zero-rule load fails loudly and specifically rather than hiding behind a green deny-test.
 */
public class AccessControlRuleCountTest {

    @Test
    public void commandApiKieBaseShouldCompileAtLeastOneRule() {
        final long ruleCount = KieServices.get().getKieClasspathContainer()
                .getKieBase("COMMAND_API")
                .getKiePackages().stream()
                .mapToLong(kiePackage -> kiePackage.getRules().size())
                .sum();

        assertTrue(ruleCount > 0,
                "COMMAND_API kbase compiled 0 rules — access-control deny-tests would pass vacuously (BC-20)");
    }
}
