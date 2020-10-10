package data.enums;

public enum ActionCodes {
	CONNECTION("0001"),
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
	
	//Codes sent by server only
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
}
