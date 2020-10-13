package process.protocol;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import data.enums.ActionCodes;
import data.protocol.Protocol;
import exceptions.CodeNotFoundException;
import exceptions.InvalidProtocolException;

/**
 * Used when recieve protocol string and need to translate it.
 * 
 * @author Aldric
 */
public class ProtocolExtractor {

	private String protocolString;

	public ProtocolExtractor(String protocolString) {
		this.protocolString = protocolString;
	}

	public Protocol extract() throws InvalidProtocolException {
		/*
		 * The String is like this : <ActionCode><opt1><opt2>... We want to get all
		 * strings between '<' and '>', so we will use a regular expression
		 */
		List<String> args = getFields();

		// if no args, we can already say that no code is provided
		if (args.size() == 0) {
			throw new InvalidProtocolException("Aucun champ n'a été trouvé dans la chaine");
		}
		
		ActionCodes actionCode;
		//we will try to get the action code located at the first index of the list (normally).
		try {
			actionCode = ActionCodes.fromCode(args.get(0));
		} catch (CodeNotFoundException e) {
			throw new InvalidProtocolException(e.getMessage());
		}
		
		//if we are here, we have a well formatted protocol, but we didn't check the content for now
		return new Protocol(actionCode, args);

	}

	private List<String> getFields() throws InvalidProtocolException {
		StringBuilder stringBuilder = new StringBuilder();
		boolean isInArg = false;
		LinkedList<String> fields = new LinkedList<String>();
		/* we will iterate over each character of the string and find all args */
		for (char c : protocolString.toCharArray()) {

			switch (c) {
			case '<':
				// if we are parsing an arg, we have a formatting issue in the string
				if (isInArg) {
					throw new InvalidProtocolException("Un '<' a été trouvé avant d'avoir trouvé un '>' fermant.");
				} else {
					// else, we will reset the stringBuilder for the next arg
					isInArg = true;
					stringBuilder.setLength(0);
				}
				break;
				
			case '>':
				// if we are not parsing an arg, we have a formatting issue in the string
				if (!isInArg) {
					throw new InvalidProtocolException("Un '>' a été trouvé avant d'avoir trouvé un '<' ouvrant.");
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
			throw new InvalidProtocolException("Le dernier '>' n'a pas été trouvé.");
		}
		
		return fields;
	}

}
