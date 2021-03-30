package fr.inria.clea.lsp.exception;

import java.util.Set;

import javax.validation.ConstraintViolation;

import fr.inria.clea.lsp.LocationContact;
import lombok.Getter;

public class CleaInvalidLocationContactMessageException extends CleaCryptoException {
    private static final long serialVersionUID = 1L;
    @Getter
    Set<ConstraintViolation<LocationContact>> violations;
    
    public CleaInvalidLocationContactMessageException(Set<ConstraintViolation<LocationContact>> violations) {
        super();
        this.violations = violations;
    }

    @Override
    public String getMessage() {
        return violations.toString();
    }
    
}
