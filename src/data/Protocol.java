package data;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import data.enums.ActionCodes;

/**
 * Protocol is a data class containing all data in order to create strings that will be send to the client.<p>
 * A protocol string is composed like this : <p>
 * {@code <Action code><option 1><option 2>...<option n>}<p>
 * The number of options will vary depending of the action code 
 * @author Aldric Vitali Silvestre
 *
 */
public class Protocol {
	/**
	 * The action code will be the first string in every protocol
	 */
	private ActionCodes actionCode;
	
	/**
	 * options are all strings that are added after actionCode in protocol
	 */
	private List<String> options = new LinkedList<>();
	
	public Protocol(ActionCodes actionCode) {
		this.actionCode = actionCode;
	}
	
	public Protocol(ActionCodes actionCode, List<String> args) {
		this.actionCode = actionCode;
		options = args;
	}

	public ActionCodes getActionCode() {
		return actionCode;
	}
	
	public void setActionCode(ActionCodes actionCode) {
		this.actionCode = actionCode;
	}
	
	/**
	 * Add an option in the protocol message.<p>
	 * @param optionString the option to add
	 */
	public void appendOption(String optionString) {
		options.add(optionString);
	}
	
	/**
	 * Add a special option : the data of an article
	 * @param productName the name of the article
	 * @param productPrice the price of the article
	 * @param productQuantity the quantity of the article
	 */
	public void appendProduct(String productName, String productPrice, String productQuantity) {
		String productOption = productName + ";" + productPrice + ";" + productQuantity;
		options.add(productOption);
	}
	
	/**
	 * Create the string containing all needed data, formatted respecting the protocol.
	 * @return the string to send
	 */
	public String toString() {
		//create StringBuilder to ensure good performance, even if there is many Strings to append
		//add the action code at the beginning
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append('<');
		stringBuilder.append(actionCode.getCode());
		stringBuilder.append('>');
		
		//then, add all options one after another
		for(String option : options) {
			stringBuilder.append('<');
			stringBuilder.append(option);
			stringBuilder.append('>');
		}
		
		return stringBuilder.toString();
	}
}
