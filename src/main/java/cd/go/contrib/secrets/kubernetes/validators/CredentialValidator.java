package cd.go.contrib.secrets.kubernetes.validators;

import com.github.bdpiparva.plugin.base.validation.ValidationResult;
import com.github.bdpiparva.plugin.base.validation.Validator;

import java.util.Map;

public class CredentialValidator implements Validator {
    @Override
    public ValidationResult validate(Map<String, String> requestBody) {
        return new ValidationResult();
    }
}
