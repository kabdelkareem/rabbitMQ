package com.karim.examples.rabbitmq.common.enums;

public enum ContentTypeEnum {
	TEXT_PLAIN("text/plain")
	, TEXT_XML("application/xml")
	, TEXT_JSON("application/json");
	
	private String value;
	private ContentTypeEnum(String value){
		this.value = value;
	}
	
	public String value() {
		return this.value;
	}
}
