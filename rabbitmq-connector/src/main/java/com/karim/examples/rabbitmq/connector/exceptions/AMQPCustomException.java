package com.karim.examples.rabbitmq.connector.exceptions;

/**
 * <p>This class used as a <i>wrapper</i> for {@link Throwable} thrown during
 * work with the amqp server.</p>
 * 
 * <p>It's store a meaningful message and the true cause of the {@link Throwable}. 
 * See {@link #AMQPCustomException(String, Throwable)} for more information.</p>
 * 
 * @author Karim Abd ElKareem
 * @since 1.0
*/

public class AMQPCustomException extends Exception {
	private static final long serialVersionUID = 1L;
	
	/**
	 * 
	 * @param message meaningful message.
	 */
	public AMQPCustomException(String message){
		super(message);
	}
	
	/**
	 * 
	 * @param message meaningful message.
	 * @param cause the true cause of the problem.
	 */
	public AMQPCustomException(String message, Throwable cause){
		super(message, cause);
	}
	
}
