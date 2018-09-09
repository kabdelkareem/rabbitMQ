package com.karim.examples.rabbitmq.connector.configures;

import java.util.function.Consumer;

import com.karim.examples.rabbitmq.common.enums.ContentTypeEnum;
import com.karim.examples.rabbitmq.connector.exceptions.AMQPCustomException;
import com.karim.examples.rabbitmq.connector.util.AMQPResourceBundle;

public final class ProducerConfigurer {
	//represents the exchange name or empty string if use default exchange, cannot be null
	private final String _exhange; 
	
	//represents the routing key or empty, cannot be null
	private final String _routingKey;
	
	//the content type of message to be send {@link ContentTypeEnum}
	private final ContentTypeEnum _messageContentType;
	
	/*
	 * used to send a reply in the message to this queue. 
	 * Note: current user must has write privilege on this queue or there'll no 
	 * 		response as it will be dropped silence 
	 */
	private String _replyToQueue;
	
	/*
	 * used as identifier of reply message to make it related to
	 * published messages
	 */
	private String _correlationId;

	//DEFAULTS
	private final static ContentTypeEnum DEFAULT_CONTENT_TYPE = ContentTypeEnum.TEXT_JSON;
	
	public ProducerConfigurer(Builder builder) {
		this._exhange = builder._exhange;
		this._routingKey = builder._routingKey;
		this._messageContentType = builder._messageContentType;
		this._replyToQueue = builder._replyToQueue;
		this._correlationId = builder._correlationId;
	}
	
	public String getExhange() {
		return this._exhange == null? "" : this._exhange.trim();
	}
	
	public String getRoutingKey() {
		return this._routingKey == null? "" : this._routingKey.trim();
	}
	
	public ContentTypeEnum getMessageContentType() {
		return this._messageContentType == null? 
				DEFAULT_CONTENT_TYPE 
				: this._messageContentType;
	}
	
	public String getReplyToQueue() {
		return this._replyToQueue;
	}

	public String getCorrelationId() {
		return this._correlationId;
	}

	public static class Builder {
		private ProducerConfigurer _producerConfigurer;
		private String _exhange; 
		private String _routingKey;
		public ContentTypeEnum _messageContentType;
		public String _replyToQueue;
		public String _correlationId;
		
		public Builder(String exchange, String routingKey) {
			this._exhange = exchange;
			this._routingKey = routingKey;
		}
		
		public Builder(ProducerConfigurer producerConfigurer) {
			this._producerConfigurer = producerConfigurer;
			this._exhange = producerConfigurer._exhange;
			this._routingKey = producerConfigurer._routingKey;
			this._messageContentType = producerConfigurer._messageContentType;
			this._replyToQueue = producerConfigurer._replyToQueue;
			this._correlationId = producerConfigurer._correlationId;
		}

		public Builder with(Consumer<Builder> builderFunction) {
	        builderFunction.accept(this);
	        return this;
	    }


		/**
		 * Sets message content type, default is 
		 * {@link ProducerConfigurer#DEFAULT_CONTENT_TYPE}
		 * 
		 * @param messageContentType the value to be specified
		 * @return current object (this).
		 * @see ProducerConfigurer#_messageContentType
		 */
		public Builder withMessageContentType(ContentTypeEnum messageContentType) {
			this._messageContentType = messageContentType;
			return this;
		}

		/**
		 * Sets reply to queue.
		 * 
		 * @param replyToQueue the value to be specified
		 * @return current object (this).
		 * @see ProducerConfigurer#_replyToQueue
		 */
		public Builder withReplyToQueue(String replyToQueue) {
			this._replyToQueue = replyToQueue;
			if(this._producerConfigurer != null) this._producerConfigurer._replyToQueue = replyToQueue;
			return this;
		}

		/**
		 * Sets correlation id.
		 * 
		 * @param correlationId the value to be specified
		 * @return current object (this).
		 * @see ProducerConfigurer#_correlationId
		 */
		public Builder withCorrelationId(String correlationId) {
			this._correlationId = correlationId;
			if(this._producerConfigurer != null) this._producerConfigurer._correlationId = correlationId;
			return this;
		}


		/**
		 * Use defined properties in the builder to initialize a new ProducerConfigurer Object.
		 * 
		 * @return {@link ProducerConfigurer#ProducerConfigurer(Builder)}
		 */
		public ProducerConfigurer build() throws AMQPCustomException {
			//Exchange name or routing key must be specified
			if((this._exhange == null || this._exhange.trim().isEmpty())
					&& (this._routingKey == null || this._routingKey.trim().isEmpty())) {
				throw new AMQPCustomException(AMQPResourceBundle.getMessage("error_AMQP020"));
			}
			
			return new ProducerConfigurer(this);
		}
	}
}
