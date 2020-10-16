package process.protocol;

import data.Protocol;
import data.enums.ActionCodes;

/**
 * Factory class used to create specific protocols directly
 * @author Aldric Vitali Silvestre <aldric.vitali@outlook.fr>
 */
public class ProtocolFactory {

	/**
	 * Create a simple error protocol message
	 * @param errorMessage the message describing the error
	 * @return a new protocol containing all this data
	 */
	public static Protocol createErrorProtocol(String errorMessage) {
		Protocol protocol = new Protocol(ActionCodes.ERROR);
		protocol.appendOption(errorMessage);
		return protocol;
	}
	
	/**
	 * Create a sucess protocol containg only the success action code
	 */
	public static Protocol createSuccessProtocol() {
		return new Protocol(ActionCodes.SUCESS);
	}

}
