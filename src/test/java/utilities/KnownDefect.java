package utilities;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Identifies tests asserting against known application defects cataloged in DEFECT_REGISTER.md.
 * Enables reporting listeners and loggers to differentiate verified application issues
 * from automation script breakage.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface KnownDefect {
    /**
     * Unique defect reference matching DEFECT_REGISTER.md (e.g., "PB-SEC-003", "PB-UI-001").
     */
    String id();

    /**
     * Brief summary of the observed application defect behavior.
     */
    String description() default "";
}
