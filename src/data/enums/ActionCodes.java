package data.enums;

import exceptions.CodeNotFoundException;

/**
 * Enum class storing all actions codes specified in protocol
 * @author Aldric
 */
public enum ActionCodes {
	CONNECTION_NORMAL("0001"),
	CONNECTION_ADMIN("0002"),
	DISCONNECT("1000"),
	ADD_NEW_PRODUCT("0101"),
	ADD_PRODUCT_QUANTITY("0102"),
	REMOVE_PRODUCT_QUANTITY("0103"),
	REMOVE_PRODUCT_DEFINITELY("0104"),
	VALIDATE_ORDER("0201"),
	DELETE_ORDER("0202"),
	GET_PRODUCT_LIST("0301"),
	GET_ORDER_LIST("0302"),
	GET_SPECIFIC_PRDUCT("0303"),
	GET_SPECIFIC_ORDER("0304"),
	APPLY_PROMOTION("0501"),
	REMOVE_PROMOTION("0502"),
	
	//Administrator only
	GET_EMPLOYEE_LIST("0305"),
	ADD_EMPLOYE("0401"),
	REMOVE_EMPLOYE("0402"),
	
	//Code sent by server only
	ERROR("9991"),
	ERROR_TIME_OUT("9992"),
	SUCESS("9993");
	
	private String code;
	private ActionCodes(String code) {
		this.code = code;
	}
	
	public String getCode() {
		return code;
	}
	
	/**
	 * Get the action code enum related to the string 
	 * @param actionCode the code we need to get enum
	 * @return the ActionCodes associated with it
	 * @throws CodeNotFoundException if code could not be found
	 */
	public static ActionCodes fromCode(String actionCode) throws CodeNotFoundException{
		//A code is only 4 chars, so if actionCode has not 4 chars, we can stop already
		if(actionCode.length() != 4) {
			throw new CodeNotFoundException(actionCode + " n'est pas composé de 4 caractères.");
		}
		
		for(ActionCodes ac : values()) {
			if(ac.getCode().equals(actionCode)) {
				return ac;
			}
		}
		//no action code found here
		throw new CodeNotFoundException(actionCode + " n'est pas un code valide.");
	}
}
