package exceptions;

/**
 * Exception raised when ActionCode added is not found in the ActionCodes list
 * @author Aldric
 * @see ActionCodes
 */
public class CodeNotFoundException extends Exception {

	public CodeNotFoundException() {
		super();
	}

	public CodeNotFoundException(String arg0, Throwable arg1, boolean arg2, boolean arg3) {
		super(arg0, arg1, arg2, arg3);
	}

	public CodeNotFoundException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public CodeNotFoundException(String arg0) {
		super(arg0);
	}

	public CodeNotFoundException(Throwable arg0) {
		super(arg0);
	}

}
