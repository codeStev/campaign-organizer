package com.campaignorganizer.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.Set;

/**
 * Architecture fitness functions (harness §3). Rules are keyed to the hexagonal ring
 * package structure (`..domain..`, `..application..`, `..adapter..`), so legacy flat
 * feature packages are not matched yet — the rules pass today and automatically
 * enforce every context as it is migrated.
 */
@AnalyzeClasses(packages = "com.campaignorganizer", importOptions = DoNotIncludeTests.class)
class ArchitectureTest {

    /** Top-level packages that are bounded contexts (ADR-0050); others are cross-cutting infra. */
    private static final Set<String> CONTEXTS =
            Set.of("worldbuilding", "campaign", "characters", "media", "whiteboard", "interchange",
                    "ai", "tables", "handouts");

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

    /** The web adapter never reaches into persistence. (Aggregate-namespaced, e.g. adapter.<agg>.in.web.) */
    @ArchTest
    static final ArchRule webHasNoPersistence = noClasses()
            .that().resideInAPackage("..in.web..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..out.persistence..",
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

    /**
     * A bounded context may reference another bounded context only through the
     * target's {@code application.port.published} interfaces (ADR-0050): no
     * foreign domain, adapter, or out-port/service reference. Generic
     * cross-cutting infra (auth, config, security, shared) is exempt in both
     * directions.
     */
    @ArchTest
    static final ArchRule contextsOnlyUsePublishedPorts = noClasses()
            .should(new ArchCondition<JavaClass>("depend on another bounded context outside its published ports") {
                @Override
                public void check(JavaClass clazz, ConditionEvents events) {
                    String from = contextOf(clazz);
                    if (from == null) {
                        return;
                    }
                    for (Dependency dependency : clazz.getDirectDependenciesFromSelf()) {
                        JavaClass target = dependency.getTargetClass();
                        String to = contextOf(target);
                        if (to == null || to.equals(from)) {
                            continue;
                        }
                        if (target.getPackageName().contains(".application.port.published")) {
                            continue;
                        }
                        events.add(SimpleConditionEvent.violated(dependency,
                                dependency.getDescription() + " crosses from context '" + from
                                        + "' into context '" + to + "' outside its published ports"));
                    }
                }
            })
            .allowEmptyShould(true);

    /** The bounded context (top-level package segment) a class belongs to, or null if not one. */
    private static String contextOf(JavaClass javaClass) {
        String name = javaClass.getPackageName();
        String prefix = "com.campaignorganizer.";
        if (!name.startsWith(prefix)) {
            return null;
        }
        String rest = name.substring(prefix.length());
        int dot = rest.indexOf('.');
        String segment = dot < 0 ? rest : rest.substring(0, dot);
        return CONTEXTS.contains(segment) ? segment : null;
    }
}
