package exceptions;

/**
 * Exception raised when protcol is not valid, either not well formatted or action code is missing
 * @author Aldric
 * @see ActionCodes
 */
public class InvalidProtocolException extends Exception {

	public InvalidProtocolException() {
		super();
	}

	public InvalidProtocolException(String arg0, Throwable arg1, boolean arg2, boolean arg3) {
		super(arg0, arg1, arg2, arg3);
	}

	public InvalidProtocolException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public InvalidProtocolException(String arg0) {
		super(arg0);
	}

	public InvalidProtocolException(Throwable arg0) {
		super(arg0);
	}
}
