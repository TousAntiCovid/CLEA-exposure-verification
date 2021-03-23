package fr.inria.clea.lsp;

/**
 * Generic Clea exception thrown when the layout of the message
 * is not respected. 
 */
public class CleaEncodingException extends Exception {
    private static final long serialVersionUID = 1L;

    public CleaEncodingException(String message) {
        super(message);
    }
}
