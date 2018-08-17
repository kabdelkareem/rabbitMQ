package com.karim.examples.rabbitmq.installer.model;

public class Queue {
	private String name;
	private String properties;

	public Queue(String name, String properties) {
		this.name = name;
		this.properties = properties;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getProperties() {
		return properties;
	}

	public void setProperties(String properties) {
		this.properties = properties;
	}
	
	@Override
	public int hashCode() {
		return this.name.hashCode();
	}
	
	@Override
	public boolean equals(Object otherObj) {
		Queue otherQueue = (Queue) otherObj;
		return this.name.equals(otherQueue.getName());
	}
}
