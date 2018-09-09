package com.karim.examples.rabbitmq.connector.configures;

import java.util.function.Consumer;

import com.karim.examples.rabbitmq.common.enums.ContentTypeEnum;
import com.karim.examples.rabbitmq.connector.exceptions.AMQPCustomException;
import com.karim.examples.rabbitmq.connector.util.AMQPResourceBundle;

public final class ConsumerConfigurer {

	// Consumed queue name
	private final String _queueName;
	// No of messages to read from the server per consumer.
	private final Integer _prefetchCount; 
	// No of consumers to consumer messages parallel.
	private final Integer _noOfConumers;
	// Is message acknowledge send when read message or after successfully process the message.
	private final Boolean _autoAck; 
	// Used as default consumer content type if message has not content_type property 
	private final ContentTypeEnum _contentType;
	
	// Default pre-fetch count per consumer
	private static final int DEFAULT_PRE_FETCH_COUNT = 5;
	// Default no of consumers
	private static final int DEFAULT_NO_OF_CONSUMERS = 1;
	// Default not auto acknowledge 
	private static final boolean DEFAULT_AUTO_ACK = false;
	
	public ConsumerConfigurer(final Builder builder) {
		this._queueName = builder._queueName;
		this._prefetchCount = builder._prefetchCount;
		this._noOfConumers = builder._noOfConumers;
		this._autoAck = builder._autoAck;
		this._contentType = builder._contentType;
	}
	
	public String getQueueName() {
		return this._queueName;
	}
	
	public int getPrefetchCount() {
		return _prefetchCount == null? 
				DEFAULT_PRE_FETCH_COUNT 
				: _prefetchCount;
	}
	
	public int getNoOfConumers() {
		return (_noOfConumers == null || _noOfConumers < DEFAULT_NO_OF_CONSUMERS)? 
				DEFAULT_NO_OF_CONSUMERS 
				: _noOfConumers;
	}

	public boolean isAutoAck() {
		return _autoAck == null? 
				DEFAULT_AUTO_ACK 
				: _autoAck;
	}
	
	public ContentTypeEnum getContentType() {
		return this._contentType;
	}

	// Builder Class
	public static final class Builder {
		private String _queueName;
		public Integer _prefetchCount; 
		public Integer _noOfConumers;
		public Boolean _autoAck;
		public ContentTypeEnum _contentType;
		
		public Builder(final String queueName) {
			this._queueName = queueName;
		}
		
		public Builder with(Consumer<Builder> builderFunction) {
	        builderFunction.accept(this);
	        return this;
	    }


		/**
		 * Sets pre-fetch count, default 
		 * set to {@link ConsumerConfigurer#DEFAULT_PRE_FETCH_COUNT}.
		 * 
		 * @param prefetchCount the value to be specified
		 * @return current object (this).
		 * @see ConsumerConfigurer#_prefetchCount
		 */
		public Builder withPrefetchCount(final Integer prefetchCount) {
			this._prefetchCount = prefetchCount;
			return this;
		}

		/**
		 * Sets no of consumers, default 
		 * set to {@link ConsumerConfigurer#DEFAULT_NO_OF_CONSUMERS}.
		 * 
		 * @param noOfConumers the value to be specified
		 * @return current object (this).
		 * @see ConsumerConfigurer#_noOfConumers
		 */
		public Builder withNoOfConumers(final Integer noOfConumers) {
			this._noOfConumers = noOfConumers;
			return this;
		}
		
		/**
		 * Sets auto acknowledge, default 
		 * set to {@link ConsumerConfigurer#DEFAULT_AUTO_ACK}.
		 * 
		 * @param autoAck the value to be specified
		 * @return current object (this).
		 * @see ConsumerConfigurer#_autoAck
		 */
		public Builder withAutoAck(final Boolean autoAck) {
			this._autoAck = autoAck;
			return this;
		}
		
		/**
		 * Sets content type
		 * 
		 * @param contentType the value to be specified
		 * @return current object (this).
		 * @see ConsumerConfigurer#_contentType
		 */
		public Builder withContentType(final ContentTypeEnum contentType) {
			this._contentType = contentType;
			return this;
		}

		/**
		 * Use defined properties in the builder to initialize a new ConsumerConfigurer Object.
		 * 
		 * @return {@link ConsumerConfigurer#ConsumerConfigurer(Builder)}
		 */
		public ConsumerConfigurer build() throws AMQPCustomException { 
			//Queue name must be specified
			if(this._queueName == null || this._queueName.trim().isEmpty()) {
				throw new AMQPCustomException(AMQPResourceBundle.getMessage("error_AMQP022"));
			}
			
			return new ConsumerConfigurer(this); 
		}
		
	}
}
