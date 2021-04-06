package fr.inria.clea.lsp.exception;

/**
 * Generic Clea exception thrown when the layout of the message
 * is not respected. 
 */
public class CleaEncodingException extends CleaCryptoException {
    private static final long serialVersionUID = 1L;

    public CleaEncodingException(String message) {
        super(message);
    }
}
