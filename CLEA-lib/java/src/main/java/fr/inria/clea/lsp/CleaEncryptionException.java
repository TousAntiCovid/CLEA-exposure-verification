package fr.inria.clea.lsp;

/**
 * Generic Clea exception thrown when something went wrong while encoding / decoding. 
 */
public class CleaEncryptionException extends Exception {
    private static final long serialVersionUID = 1L;

    public CleaEncryptionException(Throwable cause) {
        super(cause);
    }
}
