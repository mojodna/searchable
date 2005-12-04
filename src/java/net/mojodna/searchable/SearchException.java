package net.mojodna.searchable;

public class SearchException extends IndexException {
    private static final long serialVersionUID = 1L;

    public SearchException() {
        super();
    }

    public SearchException(final String message) {
        super(message);
    }

    public SearchException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public SearchException(final Throwable cause) {
        super(cause);
    }

}

