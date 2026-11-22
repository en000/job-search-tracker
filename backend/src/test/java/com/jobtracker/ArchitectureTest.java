package com.jobtracker;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.*;
import com.tngtech.archunit.lang.ArchRule;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.jobtracker", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {
    @ArchTest
    static final ArchRule controllersDoNotAccessRepositories = noClasses().that().resideInAPackage("..controllers..")
            .should().dependOnClassesThat().resideInAPackage("..repository..");

    @ArchTest
    static final ArchRule persistenceDoesNotAccessWebOrServices = noClasses().that()
            .resideInAnyPackage("..repository..", "..domain..")
            .should().dependOnClassesThat().resideInAnyPackage("..controllers..", "..services..");

    @ArchTest
    static final ArchRule domainUsesJavaAndJpa = com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes()
            .that().resideInAPackage("..domain..").should().onlyDependOnClassesThat()
            .resideInAnyPackage("..domain..", "java..", "jakarta.persistence..");
}
