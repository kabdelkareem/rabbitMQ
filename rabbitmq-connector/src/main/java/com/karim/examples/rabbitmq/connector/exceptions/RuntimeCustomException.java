package com.karim.examples.rabbitmq.connector.exceptions;


/**
 * <p>This class used as a <i>wrapper</i> for {@link Throwable} thrown during
 * receiving, parsing, and handling a message.</p>
 * 
 * <p>It's not advisable to use this exception as a wrapper because it's throws 
 * <i>unchecked exception</i> {@link RuntimeException} so used only if there is 
 * no meaning for clients to handling the <i>unchecked exceptions</i>.</p>
 * 
 * <p>It's store a meaningful message and the true cause of the {@link Throwable}. 
 * See {@link #RuntimeCustomException(String, Throwable)} for more information.</p>
 * 
 * @author Karim Abd ElKareem
 * @since 1.0
*/

public class RuntimeCustomException extends RuntimeException{
	private static final long serialVersionUID = 1L;

	/**
	 * @param message meaningful message.
	 */
	public RuntimeCustomException(String message){
		super(message);
	}
	
	/**
	 * @param message meaningful message.
	 * @param cause the true cause of the problem.
	 */
	public RuntimeCustomException(String message, Throwable cause){
		super(message, cause);
	}
}
