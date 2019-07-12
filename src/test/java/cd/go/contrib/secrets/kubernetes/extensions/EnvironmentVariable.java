package cd.go.contrib.secrets.kubernetes.extensions;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@ExtendWith(SystemExtension.class)
@Repeatable(EnvironmentVariables.class)
public @interface EnvironmentVariable {
    String key();

    String value();
}
