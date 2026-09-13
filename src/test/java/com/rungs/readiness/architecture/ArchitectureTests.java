package com.rungs.readiness.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Checks every rule in {@link ArchitectureRules} against the main classes (REQ-001); test classes are exempt. */
class ArchitectureTests {

    private static final String ROOT = "com.rungs.readiness";

    private static JavaClasses mainClasses;

    @BeforeAll
    static void importMainClasses() {
        mainClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(ROOT);
    }

    @Test
    void acADomainHasNoFrameworkImports() {
        ArchitectureRules.AC_A_domainHasNoFrameworkImports(ROOT).check(mainClasses);
    }

    @Test
    void acBDomainHasNoApplicationOrAdapterDependencies() {
        ArchitectureRules.AC_B_domainHasNoApplicationOrAdapterDependencies(ROOT).check(mainClasses);
    }

    @Test
    void acCAdaptersAreMutuallyIndependent() {
        ArchitectureRules.AC_C_adaptersAreMutuallyIndependent(ROOT).check(mainClasses);
    }

    @Test
    void acCOutboundAdaptersReachDomainOnlyThroughPorts() {
        ArchitectureRules.AC_C_outboundAdaptersReachDomainOnlyThroughPorts(ROOT).check(mainClasses);
    }

    @Test
    void acCWebViewDependsOnApplicationNotDomainInternals() {
        ArchitectureRules.AC_C_webViewDependsOnApplicationNotDomainInternals(ROOT).check(mainClasses);
    }

    @Test
    void acDNoFieldInjection() {
        ArchitectureRules.AC_D_noFieldInjection(ROOT).check(mainClasses);
    }

    @Test
    void acEDomainTypesAreImmutable() {
        ArchitectureRules.AC_E_domainTypesAreImmutable(ROOT).check(mainClasses);
    }

    @Test
    void acENoPublicMutableFields() {
        ArchitectureRules.AC_E_noPublicMutableFields(ROOT).check(mainClasses);
    }

    @Test
    void acFDomainHasNoFloatingPoint() {
        ArchitectureRules.AC_F_domainHasNoFloatingPoint(ROOT).check(mainClasses);
    }

    @Test
    void nfr4DomainIsDeterministic() {
        ArchitectureRules.NFR_4_domainIsDeterministic(ROOT).check(mainClasses);
    }
}
