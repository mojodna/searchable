package net.mojodna.searchable;

public class IndexingException extends IndexException {
    private static final long serialVersionUID = 1L;

    public IndexingException() {
        super();
    }

    public IndexingException(final String message) {
        super(message);
    }

    public IndexingException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public IndexingException(final Throwable cause) {
        super(cause);
    }

}

