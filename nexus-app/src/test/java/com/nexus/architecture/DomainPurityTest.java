package com.nexus.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Enforces domain purity (playbook §2.3):
 * Classes inside any {@code *.domain..} package must not import
 * Spring or JPA — they are pure Java business logic.
 *
 * <p>This test exists from Phase 0 onward, before any domain classes
 * exist, so the constraint is in place before anyone can violate it.
 */
class DomainPurityTest {

    private static JavaClasses allClasses;

    @BeforeAll
    static void importClasses() {
        allClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.nexus");
    }

    @Test
    void domain_must_not_import_spring() {
        ArchRule rule = noClasses()
                .that().resideInAnyPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "org.springframework..",
                        "org.springframework.boot.."
                )
                .allowEmptyShould(true);

        rule.check(allClasses);
    }

    @Test
    void domain_must_not_import_jpa_or_persistence() {
        ArchRule rule = noClasses()
                .that().resideInAnyPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "jakarta.persistence..",
                        "org.hibernate.."
                )
                .allowEmptyShould(true);

        rule.check(allClasses);
    }
}
