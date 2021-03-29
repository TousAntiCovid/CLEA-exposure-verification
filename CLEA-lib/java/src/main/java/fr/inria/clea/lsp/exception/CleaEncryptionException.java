package fr.inria.clea.lsp.exception;

/**
 * Generic Clea exception thrown when something went wrong while encoding / decoding. 
 */
public class CleaEncryptionException extends CleaCryptoException {
    private static final long serialVersionUID = 1L;

    public CleaEncryptionException(Throwable cause) {
        super(cause);
    }

    public CleaEncryptionException(String message) {
        super(message);
    }
}
