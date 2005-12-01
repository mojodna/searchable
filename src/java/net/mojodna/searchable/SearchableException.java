package net.mojodna.searchable;

public abstract class SearchableException extends Exception {
    private static final long serialVersionUID = 1L;

    public SearchableException() {
        super();
    }

    public SearchableException(final String message) {
        super(message);
    }

    public SearchableException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public SearchableException(final Throwable cause) {
        super(cause);
    }

}

