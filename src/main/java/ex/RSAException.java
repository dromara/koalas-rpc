package ex;

public class RSAException extends  RuntimeException {

    public RSAException() {
        super();
    }


    public RSAException(String message) {
        super(message);
    }

    public RSAException(String message, Throwable cause) {
        super(message, cause);
    }

    public RSAException(Throwable cause) {
        super(cause);
    }

}
