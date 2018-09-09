package com.karim.examples.rabbitmq.connector.exceptions;

/**
 * <p>This class used as a <i>wrapper</i> for {@link Throwable} thrown during
 * work with the amqp server due to timeout elapsed.</p>
 * 
 * <p>It's store a meaningful message and the true cause of the {@link Throwable}. 
 * See {@link #TimeoutCustomException(String, Throwable)} for more information.</p>
 * 
 * @author Karim Abd ElKareem
 * @since 1.0
*/

public class TimeoutCustomException extends Exception {
	private static final long serialVersionUID = 1L;
	
	/**
	 * 
	 * @param message meaningful message.
	 */
	public TimeoutCustomException(String message){
		super(message);
	}
	
	/**
	 * 
	 * @param message meaningful message.
	 * @param cause the true cause of the problem.
	 */
	public TimeoutCustomException(String message, Throwable cause){
		super(message, cause);
	}
	
}
