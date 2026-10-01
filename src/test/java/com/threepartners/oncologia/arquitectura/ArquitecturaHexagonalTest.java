package com.threepartners.oncologia.arquitectura;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Hace cumplir en cada build la regla de dependencias de la arquitectura
 * hexagonal: infrastructure -> application -> domain. Si alguien importa un
 * adaptador desde un caso de uso, o Spring/JPA desde el dominio, el build
 * falla en vez de degradarse en silencio.
 */
@AnalyzeClasses(packages = "com.threepartners.oncologia", importOptions = ImportOption.DoNotIncludeTests.class)
class ArquitecturaHexagonalTest {

    @ArchTest
    static final ArchRule elDominioNoDependeDeOtrasCapas = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..application..", "..infrastructure..", "..config..")
            .because("el dominio es el nucleo y no conoce casos de uso, adaptadores ni configuracion");

    @ArchTest
    static final ArchRule elDominioNoDependeDeFrameworks = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..", "jakarta.persistence..", "org.hibernate..")
            .because("las reglas de negocio y las formulas de los indicadores deben poder probarse sin Spring ni JPA");

    @ArchTest
    static final ArchRule laAplicacionNoDependeDeAdaptadores = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
            .because("los casos de uso hablan con el exterior solo a traves de puertos del dominio");
}
