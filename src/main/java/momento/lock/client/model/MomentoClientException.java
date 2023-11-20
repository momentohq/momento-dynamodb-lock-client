package momento.lock.client.model;


public class MomentoClientException extends RuntimeException {
    public MomentoClientException(String message) {
        super(message);
    }

    public MomentoClientException(String message, Throwable t) {
        super(message, t);
    }
}
