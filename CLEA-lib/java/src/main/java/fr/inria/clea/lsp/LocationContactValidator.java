package fr.inria.clea.lsp;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import fr.inria.clea.lsp.exception.CleaInvalidLocationContactMessageException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LocationContactValidator {
    private Validator validator;

    public LocationContactValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();        
    }

    protected void validateMessage(LocationContact message) throws CleaInvalidLocationContactMessageException {
        Set<ConstraintViolation<LocationContact>> violations = validator.validate(message);
        for (ConstraintViolation<LocationContact> violation : violations) {
            log.error(violation.getMessage()); 
        }
        if (!violations.isEmpty()) {
            throw new CleaInvalidLocationContactMessageException(violations);
        }
    }
}
