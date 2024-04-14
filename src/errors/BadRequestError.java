package errors;

public class BadRequestError extends Exception {
    public BadRequestError(String message) {
        super(message);
    }
}