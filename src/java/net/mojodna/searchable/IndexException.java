/*
Copyright 2005-2006 Seth Fitzsimmons <seth@note.amherst.edu>

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package net.mojodna.searchable;

/**
 * Exception thrown when errors occur while accessing an index.
 * 
 * @author Seth Fitzsimmons
 */
public class IndexException extends SearchableException {
    private static final long serialVersionUID = 1L;

    /**
     * Basic constructor.
     */
    public IndexException() {
        super();
    }

    /**
     * Constructor with message.
     * 
     * @param message Error message.
     */
    public IndexException(final String message) {
        super(message);
    }

    /**
     * Constructor with message and cause.
     * 
     * @param message Error message.
     * @param cause Cause of exception.
     */
    public IndexException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor with cause.
     * 
     * @param cause Cause of exception.
     */
    public IndexException(final Throwable cause) {
        super(cause);
    }
}

