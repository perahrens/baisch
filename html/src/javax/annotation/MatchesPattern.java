package javax.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** GWT stub — shadows jsr305 version which uses java.util.regex not available in GWT. */
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface MatchesPattern {
    String value();
    int flags() default 0;
}
