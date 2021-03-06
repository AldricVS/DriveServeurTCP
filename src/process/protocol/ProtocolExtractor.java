package process.protocol;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import data.Protocol;
import data.enums.ActionCodes;
import exceptions.CodeNotFoundException;
import exceptions.InvalidProtocolException;
import logger.LoggerUtility;
import process.connection.ClientThread;

/**
 * Used when recieve protocol string and need to translate it.
 * 
 * @author Aldric
 */
public class ProtocolExtractor {
	private static Logger logger = LoggerUtility.getLogger(ProtocolExtractor.class, LoggerUtility.LOG_PREFERENCE);

	private String protocolString;
	private Protocol protocol;

	/**
	 * Construct a protocol from a String
	 * @param protocolString
	 * @throws InvalidProtocolException
	 */
	public ProtocolExtractor(String protocolString) throws InvalidProtocolException{
		this.protocolString = protocolString;
		protocol = extract();
	}
	
	/**
	 * Construct a protocol from a char array. Useful when reading with limited buffer size.
	 * If the array is full, the InvalidProtocoleException is raised.
	 * @param protocolCharArray
	 * @throws InvalidProtocolException
	 */
	public ProtocolExtractor(char[] protocolCharArray) throws InvalidProtocolException{
		//check if last character is terminal, in the other case, raise exception
		if(protocolCharArray[protocolCharArray.length - 1] != '\u0000') {
			logger.error("Message recieved too long.");
			throw new InvalidProtocolException("Le message re�u est trop long.");
		}
		this.protocolString = String.valueOf(protocolCharArray);
		protocol = extract();
	}

	public Protocol getProtocol() {
		return protocol;
	}
	
	/**
	 * Check if protocol options size is valid and throws exception if not
	 * @param expectedNumber the size expected for the protcol's options list
	 * @throws InvalidProtocolException if size is not equals
	 */
	public void assertOptionsNumberValid(int expectedNumber) throws InvalidProtocolException{
		int optionsListSize = protocol.getOptionsListSize();
		if(expectedNumber != optionsListSize) {
			throw new InvalidProtocolException(String.format("Number of options not valid (expected %d but have %d)", expectedNumber, optionsListSize));
		}
	}
	
	/**
	 * Check if protocol have one of the specified ActionCodes
	 * @param actionCodes the actions codes to check. Can be called with zero (not very useful though) or more arguments.
	 * @throws InvalidProtocolException if action code in protocol is not the one in the list.
	 */
	public void assertActionCodeValid(ActionCodes ... actionCodes) throws InvalidProtocolException{
		ActionCodes protocolActionCode = protocol.getActionCode();
		//check through all parameters
		for(ActionCodes code : actionCodes) {
			//if code is same as the one of protcol, we can end the method
			if(code == protocolActionCode) {
				return;
			}
		}
		//if we are here, it means that we don't have found the code in the parameters list
		logger.error(String.format("Number of options not valid (have %s)", protocolActionCode.name()));
		throw new InvalidProtocolException("Action non reconnue par le serveur.");
	}
	
	private Protocol extract() throws InvalidProtocolException {
		/*
		 * The String is like this : <ActionCode><opt1><opt2>... We want to get all
		 * strings between '<' and '>', so we will use a regular expression
		 */
		List<String> args = getFields();

		// if no args, we can already say that no code is provided
		if (args.size() == 0) {
			logger.error("No field found in the string");
			throw new InvalidProtocolException("Aucun champ n'a �t� trouv� dans le message");
		}
		
		ActionCodes actionCode;
		//we will try to get the action code located at the first index of the list (normally).
		try {
			actionCode = ActionCodes.fromCode(args.get(0));
			//remove it from the list
			args.remove(0);
		} catch (CodeNotFoundException e) {
			throw new InvalidProtocolException(e.getMessage());
		}
		
		//if we are here, we have a well formatted protocol, but we didn't check the content for now
		return new Protocol(actionCode, args);

	}

	private List<String> getFields() throws InvalidProtocolException {
		StringBuilder stringBuilder = new StringBuilder();
		boolean isInArg = false;
		ArrayList<String> fields = new ArrayList<String>();
		/* we will iterate over each character of the string and find all args */
		for (char c : protocolString.toCharArray()) {

			switch (c) {
			case '<':
				// if we are parsing an arg, we have a formatting issue in the string
				if (isInArg) {
					logger.error("'<' found before closing '>'.");
					throw new InvalidProtocolException("Le message envoy� n'est pas form� correctement.");
				} else {
					// else, we will reset the stringBuilder for the next arg
					isInArg = true;
					stringBuilder.setLength(0);
				}
				break;
				
			case '>':
				// if we are not parsing an arg, we have a formatting issue in the string
				if (!isInArg) {
					logger.error("'>' found before opening '<'.");
					throw new InvalidProtocolException("Le message envoy� n'est pas form� correctement.");
				} else {
					// else, we will get the content of the string builder and append it to the
					// result list
					isInArg = false;
					fields.add(stringBuilder.toString());
				}
				
			default:
				//just add current character to the builder
				stringBuilder.append(c);
				break;
			}
		}
		
		//last test : if isInArg is still true, it means that we didn't close the last field
		if(isInArg) {
			logger.error("Last '>' not found.");
			throw new InvalidProtocolException("Le message envoy� au serveur n'a peut-�tre pas �t� envoy� dans sa totalit�.");
		}
		
		return fields;
	}

}
