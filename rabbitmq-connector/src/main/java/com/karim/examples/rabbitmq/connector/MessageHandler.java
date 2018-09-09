package com.karim.examples.rabbitmq.connector;

import java.util.Map;

@FunctionalInterface
public interface MessageHandler<E, R> {
	public R handleDelivery(E message, Map<String, Object> headers) throws Exception;
}
