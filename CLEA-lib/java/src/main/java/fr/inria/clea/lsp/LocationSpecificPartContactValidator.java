package fr.inria.clea.lsp;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import fr.inria.clea.lsp.exception.CleaInvalidLocationMessageException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LocationSpecificPartContactValidator {
    private Validator validator;

    public LocationSpecificPartContactValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();        
    }

    protected void validateMessage(LocationSpecificPart message) throws CleaInvalidLocationMessageException {
        Set<ConstraintViolation<LocationSpecificPart>> violations = validator.validate(message);
        for (ConstraintViolation<LocationSpecificPart> violation : violations) {
            log.error(violation.getMessage()); 
        }
        if (!violations.isEmpty()) {
            throw new CleaInvalidLocationMessageException(violations);
        }
    }
}
