package com.karim.examples.rabbitmq.common.enums;

public enum DeliveryModeEnum {
	NONPERSISTENT(1)
	, PERSISTENT(2);
	
	private int value;
	private DeliveryModeEnum(int value){
		this.value = value;
	}
	
	public int value() {
		return this.value;
	}
}
