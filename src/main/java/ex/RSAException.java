package ex;
/**
 * Copyright (C) 2018
 * All rights reserved
 * User: yulong.zhang
 * Date:2018年11月23日11:13:33
 */
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
