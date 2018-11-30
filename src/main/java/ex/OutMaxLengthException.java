package ex;
/**
 * Copyright (C) 2018
 * All rights reserved
 * User: yulong.zhang
 * Date:2018年11月23日11:13:33
 */
public class OutMaxLengthException extends  RuntimeException {

    public OutMaxLengthException() {
        super();
    }


    public OutMaxLengthException(String message) {
        super(message);
    }

    public OutMaxLengthException(String message, Throwable cause) {
        super(message, cause);
    }

    public OutMaxLengthException(Throwable cause) {
        super(cause);
    }

}
