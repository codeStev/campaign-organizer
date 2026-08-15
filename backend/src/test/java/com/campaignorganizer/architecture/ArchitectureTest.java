package com.campaignorganizer.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Architecture fitness functions (harness §3). Rules are keyed to the hexagonal ring
 * package structure (`..domain..`, `..application..`, `..adapter..`), so legacy flat
 * feature packages are not matched yet — the rules pass today and automatically
 * enforce every context as it is migrated.
 */
@AnalyzeClasses(packages = "com.campaignorganizer", importOptions = DoNotIncludeTests.class)
class ArchitectureTest {

    /** The domain ring is pure Java: no Spring, JPA, Jackson, or Hibernate. */
    @ArchTest
    static final ArchRule domainIsFrameworkFree = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..",
                    "jakarta.persistence..",
                    "com.fasterxml.jackson..",
                    "org.hibernate..")
            .allowEmptyShould(true);

    /** The core (domain + application) never depends on HTTP/web types. */
    @ArchTest
    static final ArchRule coreHasNoWeb = noClasses()
            .that().resideInAnyPackage("..domain..", "..application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework.web..",
                    "org.springframework.http..",
                    "jakarta.servlet..")
            .allowEmptyShould(true);

    /** The web adapter never reaches into persistence. */
    @ArchTest
    static final ArchRule webHasNoPersistence = noClasses()
            .that().resideInAPackage("..adapter.in.web..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..adapter.out.persistence..",
                    "org.springframework.data..",
                    "jakarta.persistence..")
            .allowEmptyShould(true);

    /** Mapper types are MapStruct-generated, never hand-written. */
    @ArchTest
    static final ArchRule mappersAreMapStruct = classes()
            .that().haveSimpleNameEndingWith("Mapper")
            .and().areInterfaces()
            .should().beAnnotatedWith("org.mapstruct.Mapper")
            .allowEmptyShould(true);
}
