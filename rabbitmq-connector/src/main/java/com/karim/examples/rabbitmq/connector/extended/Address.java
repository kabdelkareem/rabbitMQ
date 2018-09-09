package com.karim.examples.rabbitmq.connector.extended;

public class Address extends com.rabbitmq.client.Address {

	public Address(String host) {
		super(host);
	}
	
    public Address(String host, int port) {
    	super(host, port);
    }

}
