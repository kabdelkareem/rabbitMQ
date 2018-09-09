package com.karim.examples.rabbitmq.installer.model;

public class Binding {
	private String exchangeName;
	private String queueName;
	private String routingKey;

	public Binding(String exchangeName, String queueName, String routingKey) {
		this.exchangeName = exchangeName;
		this.queueName = queueName;
		this.routingKey = routingKey;
	}

	public String getExchangeName() {
		return exchangeName;
	}

	public void setExchangeName(String exchangeName) {
		this.exchangeName = exchangeName;
	}

	public String getQueueName() {
		return queueName;
	}

	public void setQueueName(String queueName) {
		this.queueName = queueName;
	}

	public String getRoutingKey() {
		return routingKey;
	}

	public void setRoutingKey(String routingKey) {
		this.routingKey = routingKey;
	}
	
	@Override
	public int hashCode() {
		return this.exchangeName.hashCode() +
				this.queueName.hashCode() +
				this.routingKey.hashCode();
	}
	
	@Override
	public boolean equals(Object otherObj) {
		Binding otherBinding = (Binding) otherObj;
		return this.exchangeName.equals(otherBinding.getExchangeName())
				&& this.queueName.equals(otherBinding.getQueueName())
				&& this.routingKey.equals(otherBinding.getRoutingKey());
	}

}
