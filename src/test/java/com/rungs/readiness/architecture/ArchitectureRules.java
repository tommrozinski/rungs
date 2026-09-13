package com.rungs.readiness.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

import com.tngtech.archunit.core.domain.AccessTarget;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaCodeUnitAccess;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.domain.JavaType;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.CompositeArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.time.Clock;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.random.RandomGenerator;

/**
 * The build-enforced architecture constraints of PRD §8, one factory per constraint id. Each rule is built
 * for a root package so the same rule checks the real code ({@code com.rungs.readiness}) and proves it fires
 * on a violation fixture ({@code com.rungs.readiness.archfixtures.<case>}). Every rule states its constraint
 * in {@code because}, so a failure names it.
 */
final class ArchitectureRules {

    /** Adapter packages, relative to the root, that must stay mutually independent. */
    private static final List<String> ADAPTERS = List.of("adapter.in.web.api", "adapter.in.web.view", "adapter.out.ai");

    /** Type names banned in the domain by AC-F. */
    private static final Set<String> FLOATING_POINT_TYPES = Set.of("double", "float", "java.lang.Double", "java.lang.Float");

    private ArchitectureRules() {
    }

    /** AC-A: {@code domain} depends on nothing from Spring, Jakarta, or Jackson. */
    static ArchRule AC_A_domainHasNoFrameworkImports(String root) {
        return noClasses().that().resideInAPackage(domain(root))
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..", "jakarta..", "tools.jackson..", "com.fasterxml.jackson..")
                .because("AC-A: the domain imports nothing from Spring, Jakarta, or Jackson");
    }

    /** AC-B: {@code domain} depends on nothing in {@code application} or {@code adapter}. */
    static ArchRule AC_B_domainHasNoApplicationOrAdapterDependencies(String root) {
        return noClasses().that().resideInAPackage(domain(root))
                .should().dependOnClassesThat().resideInAnyPackage(root + ".application..", root + ".adapter..")
                .because("AC-B: the domain has no dependency on the application or adapter packages");
    }

    /**
     * AC-C: the adapters {@code adapter.in.web.api}, {@code adapter.in.web.view}, and {@code adapter.out.ai}
     * are mutually independent. Adapters may still be empty, so an empty layer passes.
     */
    static ArchRule AC_C_adaptersAreMutuallyIndependent(String root) {
        CompositeArchRule rule = null;
        for (String adapter : ADAPTERS) {
            String[] others = ADAPTERS.stream()
                    .filter(other -> !other.equals(adapter))
                    .map(other -> root + "." + other + "..")
                    .toArray(String[]::new);
            ArchRule independent = noClasses().that().resideInAPackage(root + "." + adapter + "..")
                    .should().dependOnClassesThat().resideInAnyPackage(others)
                    .allowEmptyShould(true)
                    .because("AC-C: adapters never depend on other adapters");
            rule = rule == null ? CompositeArchRule.of(independent) : rule.and(independent);
        }
        return rule;
    }

    /**
     * AC-C: {@code adapter.out} reaches the core through {@code domain.port}: it depends on neither
     * {@code application} nor the domain internals {@code domain.stage} and {@code domain.projection}. Port
     * signatures carry {@code domain.model} types, so those stay reachable.
     */
    static ArchRule AC_C_outboundAdaptersReachDomainOnlyThroughPorts(String root) {
        return noClasses().that().resideInAPackage(root + ".adapter.out..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        root + ".application..", root + ".domain.stage..", root + ".domain.projection..")
                .allowEmptyShould(true)
                .because("AC-C: outbound adapters depend on domain ports, not on the application layer or domain internals");
    }

    /** AC-C: {@code adapter.in.web.view} works through {@code application}, never {@code domain.stage} or {@code domain.projection}. */
    static ArchRule AC_C_webViewDependsOnApplicationNotDomainInternals(String root) {
        return noClasses().that().resideInAPackage(root + ".adapter.in.web.view..")
                .should().dependOnClassesThat().resideInAnyPackage(root + ".domain.stage..", root + ".domain.projection..")
                .allowEmptyShould(true)
                .because("AC-C: web view controllers depend on the application layer, never on domain internals");
    }

    /** AC-D: no field is annotated for injection anywhere under the root; constructor injection only. */
    static ArchRule AC_D_noFieldInjection(String root) {
        return noFields().that().areDeclaredInClassesThat().resideInAPackage(root + "..")
                .should().beAnnotatedWith("org.springframework.beans.factory.annotation.Autowired")
                .orShould().beAnnotatedWith("jakarta.inject.Inject")
                .orShould().beAnnotatedWith("javax.inject.Inject")
                .allowEmptyShould(true)
                .because("AC-D: no field injection, constructor injection only");
    }

    /**
     * AC-E: {@code domain} classes are records, enums, interfaces, or final classes whose fields are all final.
     * Compiler-generated synthetic classes (such as enum switch maps) are not domain types and are exempt.
     * Anonymous classes cannot be subclassed but javac never marks them final, so only their fields are checked.
     */
    static ArchRule AC_E_domainTypesAreImmutable(String root) {
        return classes().that().resideInAPackage(domain(root))
                .should(new ArchCondition<JavaClass>("be records, enums, interfaces, or final classes with only final fields") {
                    @Override
                    public void check(JavaClass type, ConditionEvents events) {
                        if (type.isRecord() || type.isEnum() || type.isInterface()
                                || type.getModifiers().contains(JavaModifier.SYNTHETIC)) {
                            return;
                        }
                        if (!type.isAnonymousClass() && !type.getModifiers().contains(JavaModifier.FINAL)) {
                            events.add(SimpleConditionEvent.violated(type,
                                    "Class <" + type.getName() + "> is not final"));
                        }
                        for (JavaField field : type.getFields()) {
                            if (!field.getModifiers().contains(JavaModifier.FINAL)) {
                                events.add(SimpleConditionEvent.violated(field,
                                        "Field <" + field.getFullName() + "> in class <" + type.getName() + "> is not final"));
                            }
                        }
                    }
                })
                .because("AC-E: domain types are records or immutable classes");
    }

    /** AC-E: no public non-final field anywhere under the root; public constants are allowed. */
    static ArchRule AC_E_noPublicMutableFields(String root) {
        return fields().that().areDeclaredInClassesThat().resideInAPackage(root + "..")
                .and().arePublic()
                .should().beFinal()
                .allowEmptyShould(true)
                .because("AC-E: no public mutable state");
    }

    /**
     * AC-F: {@code domain} uses no {@code double}, {@code float}, {@code Double}, or {@code Float} in field
     * types, parameter types, or return types (including generic type arguments), nor calls or references a
     * method or constructor whose signature has one.
     */
    static ArchRule AC_F_domainHasNoFloatingPoint(String root) {
        return classes().that().resideInAPackage(domain(root))
                .should(new ArchCondition<JavaClass>("use no double, float, Double, or Float") {
                    @Override
                    public void check(JavaClass type, ConditionEvents events) {
                        for (JavaField field : type.getFields()) {
                            if (isFloatingPoint(field.getType())) {
                                events.add(SimpleConditionEvent.violated(field,
                                        "Field <" + field.getFullName() + "> has a floating-point type"));
                            }
                        }
                        for (JavaCodeUnit unit : type.getCodeUnits()) {
                            if (isFloatingPoint(unit.getReturnType()) || unit.getParameterTypes().stream().anyMatch(ArchitectureRules::isFloatingPoint)) {
                                events.add(SimpleConditionEvent.violated(unit,
                                        "<" + unit.getFullName() + "> has a floating-point parameter or return type"));
                            }
                        }
                        for (JavaCodeUnitAccess<?> access : type.getCodeUnitAccessesFromSelf()) {
                            if (isFloatingPoint(access.getTarget().getReturnType())
                                    || access.getTarget().getParameterTypes().stream().anyMatch(ArchitectureRules::isFloatingPoint)) {
                                events.add(SimpleConditionEvent.violated(access,
                                        access.getDescription() + " with a floating-point signature"));
                            }
                        }
                    }
                })
                .because("AC-F: double and float are banned in the domain");
    }

    /**
     * NFR-4 determinism: {@code domain} reads no wall clock and uses no randomness, whether it calls the source
     * or takes a method reference to it. Banned: {@code now} on any {@code java.time..} type unless given an
     * injected {@link Clock}; {@code Clock.system*}; {@code System.currentTimeMillis} and {@code nanoTime};
     * {@code RandomGenerator.getDefault} and {@code of}; {@code UUID.randomUUID}; and any dependency on a
     * {@link RandomGenerator} implementation ({@code Random}, {@code SecureRandom}, {@code ThreadLocalRandom},
     * {@code SplittableRandom}, ...).
     */
    static ArchRule NFR_4_domainIsDeterministic(String root) {
        return classes().that().resideInAPackage(domain(root))
                .should(new ArchCondition<JavaClass>("read no wall clock and use no randomness") {
                    @Override
                    public void check(JavaClass type, ConditionEvents events) {
                        for (JavaCodeUnitAccess<?> access : type.getCodeUnitAccessesFromSelf()) {
                            if (isNondeterministicSource(access.getTarget())) {
                                events.add(SimpleConditionEvent.violated(access, access.getDescription()));
                            }
                        }
                        for (Dependency dependency : type.getDirectDependenciesFromSelf()) {
                            if (dependency.getTargetClass().isAssignableTo(RandomGenerator.class)) {
                                events.add(SimpleConditionEvent.violated(dependency, dependency.getDescription()));
                            }
                        }
                    }
                })
                .because("NFR-4: identical input gives identical output, so the domain reads no wall clock and no randomness");
    }

    private static boolean isNondeterministicSource(AccessTarget.CodeUnitAccessTarget target) {
        String owner = target.getOwner().getName();
        String packageName = target.getOwner().getPackageName();
        String name = target.getName();
        boolean wallClockNow = name.equals("now")
                && (packageName.equals("java.time") || packageName.startsWith("java.time."))
                && target.getRawParameterTypes().stream().noneMatch(parameter -> parameter.isEquivalentTo(Clock.class));
        boolean systemClock = owner.equals(Clock.class.getName()) && name.startsWith("system");
        boolean systemTime = owner.equals(System.class.getName())
                && (name.equals("currentTimeMillis") || name.equals("nanoTime"));
        boolean defaultGenerator = owner.equals(RandomGenerator.class.getName())
                && (name.equals("getDefault") || name.equals("of"));
        boolean randomUuid = owner.equals(UUID.class.getName()) && name.equals("randomUUID");
        return wallClockNow || systemClock || systemTime || defaultGenerator || randomUuid;
    }

    private static boolean isFloatingPoint(JavaType type) {
        return type.getAllInvolvedRawTypes().stream().anyMatch(raw -> FLOATING_POINT_TYPES.contains(raw.getName()));
    }

    private static String domain(String root) {
        return root + ".domain..";
    }
}
