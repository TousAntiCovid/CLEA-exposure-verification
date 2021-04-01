package fr.inria.clea.lsp.exception;

import java.util.Set;

import javax.validation.ConstraintViolation;

import fr.inria.clea.lsp.LocationSpecificPart;
import lombok.Getter;

public class CleaInvalidLocationMessageException extends CleaCryptoException {
    private static final long serialVersionUID = 1L;
    
    @Getter
    Set<ConstraintViolation<LocationSpecificPart>> violations;
    
    public CleaInvalidLocationMessageException(Set<ConstraintViolation<LocationSpecificPart>> violations) {
        super();
        this.violations = violations;
    }

}
