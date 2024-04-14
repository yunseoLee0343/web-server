package errors;

public class RecvTimeoutError extends Exception {
    public RecvTimeoutError(String message) {
        super(message);
    }
}