package com.achmadrizkin.splitbill;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/** Keeps the layering described in CLAUDE.md honest, enforced at build time. */
@AnalyzeClasses(packages = "com.achmadrizkin.splitbill", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule domainIsFrameworkFree = noClasses()
            .that().resideInAPackage("..splitbill.domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..", "jakarta..", "org.hibernate..", "com.fasterxml..");

    @ArchTest
    static final ArchRule domainDoesNotDependOnOuterLayers = noClasses()
            .that().resideInAPackage("..splitbill.domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..splitbill.application..", "..splitbill.api..", "..splitbill.infrastructure..");

    @ArchTest
    static final ArchRule apiNeverTouchesPersistence = noClasses()
            .that().resideInAPackage("..splitbill.api..")
            .should().dependOnClassesThat().resideInAPackage("..splitbill.infrastructure.persistence..");
}
