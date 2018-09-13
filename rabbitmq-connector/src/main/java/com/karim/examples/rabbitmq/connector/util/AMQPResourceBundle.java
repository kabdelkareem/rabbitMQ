package com.karim.examples.rabbitmq.connector.util;

import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * This class handle the resource bundle across the application. 
 * This class can not be instantiated.
 * 
 * 
 * This a class following <i>Singleton Design Pattern</i>.
 * 
 * @author Karim Abd ElKareem
 * @since 1.0
 */
public final class AMQPResourceBundle {
	//Represents the resource bundle
	public static ResourceBundle bundle = null;
	
	/**
	 * Disable the constructor
	 */
	private AMQPResourceBundle(){ }
	
	/**
	 * Gets the value from bundle according to current language.
	 * 
	 * @param	msg_key the message key to retrieve from the bundle 
	 * @return	the value from bundle according to current language if specified
	 * 			 or according to default bundle.
	 */
	public static String getMessage(String msg_key){
		if(bundle == null)
			bundle =  ResourceBundle.getBundle("messages");
		return bundle.getString(msg_key);
	}
	
	
	/**
	 * Gets the value from bundle according to current language and replace placeholders
	 * with parameters in parameters array.
	 * 
	 * @param msg_key		the message key to retrieve from the bundle 
	 * @param parameters 	the message parameters
	 * @return	the value from bundle according to current language if specified
	 * 			 or according to default bundle.
	 */
	public static String getParameterizedMessage(String msg_key, Object... parameters){
		if(bundle == null)
			bundle =  ResourceBundle.getBundle("messages");
		String message = bundle.getString(msg_key);
		return MessageFormat.format(message, parameters);
	}
}
