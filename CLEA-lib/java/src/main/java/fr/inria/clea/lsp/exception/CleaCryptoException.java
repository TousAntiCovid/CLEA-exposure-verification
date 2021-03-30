package fr.inria.clea.lsp.exception;


public abstract class CleaCryptoException extends Exception {
    private static final long serialVersionUID = 1L;

    public CleaCryptoException() {
        super();
    }

    public CleaCryptoException(String message) {
        super(message);
    }

    public CleaCryptoException(Throwable cause) {
        super(cause);
    }
}
