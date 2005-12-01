package net.mojodna.searchable;

public class IndexException extends SearchableException {
    private static final long serialVersionUID = 1L;

    public IndexException() {
        super();
    }

    public IndexException(final String message) {
        super(message);
    }

    public IndexException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public IndexException(final Throwable cause) {
        super(cause);
    }

}

