package com.rungs.readiness.architecture;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

/**
 * Proves each rule in {@link ArchitectureRules} fires (REQ-011): every rule is built for the root of one
 * violation fixture under {@code archfixtures}, checked against that fixture alone, and must fail naming
 * the constraint and the offending class.
 */
class ArchitectureFixtureTests {

    private static final String FIXTURES = "com.rungs.readiness.archfixtures";

    @Test
    void acAFailsOnDomainClassUsingSpring() {
        String root = FIXTURES + ".aca";

        assertFails(ArchitectureRules.AC_A_domainHasNoFrameworkImports(root), root,
                "AC-A", root + ".domain.FrameworkBoundModel");
    }

    @Test
    void acBFailsOnDomainClassUsingApplicationAndAdapter() {
        String root = FIXTURES + ".acb";

        assertFails(ArchitectureRules.AC_B_domainHasNoApplicationOrAdapterDependencies(root), root,
                "AC-B", root + ".domain.LayerLeakingModel",
                root + ".application.AssessmentService", root + ".adapter.in.web.api.AssessmentController");
    }

    @Test
    void acCFailsOnAdaptersDependingOnEachOther() {
        String root = FIXTURES + ".accadapters";

        assertFails(ArchitectureRules.AC_C_adaptersAreMutuallyIndependent(root), root,
                "AC-C", root + ".adapter.in.web.api.ApiEndpoint", root + ".adapter.in.web.view.ViewPage",
                root + ".adapter.out.ai.AiClient");
    }

    @Test
    void acCFailsOnOutboundAdapterBypassingItsPort() {
        String root = FIXTURES + ".accoutbound";
        ArchRule rule = ArchitectureRules.AC_C_outboundAdaptersReachDomainOnlyThroughPorts(root);

        assertFails(rule, root,
                "AC-C", root + ".adapter.out.ai.LeakyCategorizationClient", root + ".application.NarrationService",
                root + ".domain.stage.StageVerdict", root + ".domain.projection.PayoffSchedule");
        assertThatThrownBy(() -> rule.check(new ClassFileImporter().importPackages(root)))
                .as("implementing the port is allowed")
                .hasMessageNotContaining(root + ".domain.port.CategorizationPort>");
    }

    @Test
    void acCFailsOnViewAdapterUsingDomainInternals() {
        String root = FIXTURES + ".accview";
        ArchRule rule = ArchitectureRules.AC_C_webViewDependsOnApplicationNotDomainInternals(root);

        assertFails(rule, root,
                "AC-C", root + ".adapter.in.web.view.LeakyViewController", root + ".domain.stage.LadderStep",
                root + ".domain.projection.MonthlyBalance");
        assertThatThrownBy(() -> rule.check(new ClassFileImporter().importPackages(root)))
                .as("using the application layer is allowed")
                .hasMessageNotContaining(root + ".application.AssessmentQueries>");
    }

    @Test
    void acDFailsOnFieldInjection() {
        String root = FIXTURES + ".acd";
        ArchRule rule = ArchitectureRules.AC_D_noFieldInjection(root);

        assertFails(rule, root, "AC-D", root + ".application.FieldInjectedService.collaborator");
        assertThatThrownBy(() -> rule.check(new ClassFileImporter().importPackages(root)))
                .as("constructor injection is allowed")
                .hasMessageNotContaining("ConstructorInjectedService.collaborator");
    }

    @Test
    void acEFailsOnMutableOrExtensibleDomainClasses() {
        String root = FIXTURES + ".acedomain";
        ArchRule rule = ArchitectureRules.AC_E_domainTypesAreImmutable(root);

        assertFails(rule, root, "AC-E", root + ".domain.OpenTotal", root + ".domain.ReassignableTotal",
                "Field <" + root + ".domain.ReassignableTotal$1.runs>");
        assertThatThrownBy(() -> rule.check(new ClassFileImporter().importPackages(root)))
                .as("final classes with final fields, records, enums, interfaces, and anonymous classes are allowed")
                .hasMessageNotContaining("FrozenTotal")
                .hasMessageNotContaining("Class <" + root + ".domain.ReassignableTotal$1> is not final");
    }

    @Test
    void acEFailsOnPublicNonFinalField() {
        String root = FIXTURES + ".acefields";
        ArchRule rule = ArchitectureRules.AC_E_noPublicMutableFields(root);

        assertFails(rule, root, "AC-E", root + ".application.PublicCounter.count");
        assertThatThrownBy(() -> rule.check(new ClassFileImporter().importPackages(root)))
                .as("public constants are allowed")
                .hasMessageNotContaining("PublicCounter.LIMIT");
    }

    @Test
    void acFFailsOnFloatingPointInDomain() {
        String root = FIXTURES + ".acf";
        String type = root + ".domain.FloatingRate";

        assertFails(ArchitectureRules.AC_F_domainHasNoFloatingPoint(root), root,
                "AC-F",
                "Field <" + type + ".rate>",
                "Field <" + type + ".history>",
                "<" + type + ".scaled(float)>",
                "<" + type + ".boxed()>",
                "java.math.BigDecimal.doubleValue()");
    }

    @Test
    void nfr4FailsOnWallClockAndRandomnessInDomain() {
        String root = FIXTURES + ".nfr4";
        String type = root + ".domain.WallClockReader";
        ArchRule rule = ArchitectureRules.NFR_4_domainIsDeterministic(root);

        assertFails(rule, root,
                "NFR-4",
                "<" + type + ".instant()> calls method <java.time.Instant.now()>",
                "<" + type + ".zonedDate()> calls method <java.time.LocalDate.now(java.time.ZoneId)>",
                "<" + type + ".millis()> calls method <java.lang.System.currentTimeMillis()>",
                "<" + type + ".nanos()> calls method <java.lang.System.nanoTime()>",
                "<" + type + ".random()> calls constructor <java.util.Random.<init>()>",
                "<" + type + ".threadLocalRandom()> calls method <java.util.concurrent.ThreadLocalRandom.current()>",
                "<" + type + ".secureRandom()> calls constructor <java.security.SecureRandom.<init>()>",
                "<" + type + ".uuid()> calls method <java.util.UUID.randomUUID()>",
                "<" + type + ".chronoDate()> calls method <java.time.chrono.JapaneseDate.now()>",
                "<" + type + ".systemClockDate()> calls method <java.time.Clock.systemDefaultZone()>",
                "<" + type + ".utcClock()> calls method <java.time.Clock.systemUTC()>",
                "<" + type + ".zonedClock()> calls method <java.time.Clock.system(java.time.ZoneId)>",
                "<" + type + ".splittable()> calls constructor <java.util.SplittableRandom.<init>()>",
                "<" + type + ".defaultGenerator()> calls method <java.util.random.RandomGenerator.getDefault()>",
                "<" + type + ".namedGenerator()> calls method <java.util.random.RandomGenerator.of(java.lang.String)>",
                "<" + type + ".instantReference()> references method <java.time.Instant.now()>",
                "<" + type + ".millisReference()> references method <java.lang.System.currentTimeMillis()>",
                "<" + type + ".nanosReference()> references method <java.lang.System.nanoTime()>",
                "<" + type + ".clockReference()> references method <java.time.Clock.systemUTC()>",
                "<" + type + ".uuidReference()> references method <java.util.UUID.randomUUID()>");
        assertThatThrownBy(() -> rule.check(new ClassFileImporter().importPackages(root)))
                .as("reading an injected Clock is allowed")
                .hasMessageNotContaining("LocalDate.now(java.time.Clock)");
    }

    private static void assertFails(ArchRule rule, String root, String... expectedFragments) {
        JavaClasses fixture = new ClassFileImporter().importPackages(root);

        assertThatThrownBy(() -> rule.check(fixture))
                .isInstanceOf(AssertionError.class)
                .hasMessageContainingAll(expectedFragments);
    }
}
