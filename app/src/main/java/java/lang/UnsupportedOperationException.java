package java.lang;

public class UnsupportedOperationException extends RuntimeException {
    static final long serialVersionUID = -1242599979055084673L;

    public UnsupportedOperationException(String message) {
        super(message);
    }

    public UnsupportedOperationException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnsupportedOperationException(Throwable cause) {
        super(cause);
    }
}
