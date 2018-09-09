package com.karim.examples.rabbitmq.connector.parser;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.karim.examples.rabbitmq.connector.exceptions.JSONCustomException;
import com.karim.examples.rabbitmq.connector.util.AMQPResourceBundle;


/**
 * This class consists exclusively of static methods that operates on parsing 
 * the object to it's JSON representation and vice-versa.
 * 
 * This class uses the library jackson, so you must include it
 * See <a href="http://github.com/FasterXML/jackson/">jackson</a>.
 * 
 * @author Karim Abd ElKareem
 * @since 1.0
 */
public class JSONFormatter {
    
	/**
	 * Parses the specified object to JSON string.
	 *  
	 * @param obj is the object to be parsed.
	 * @return a JSON string represents the parsed object
	 * @throws JSONCustomException if an error occurs during the parsing 
	 * 			of the object.
	 */
	public static String marshalObjectToJSON(Object obj) throws JSONCustomException  {
		ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.writeValueAsString(obj);
		} catch (JsonProcessingException e) {
			throw new JSONCustomException(AMQPResourceBundle.getMessage("error_AMQP003"), e);
		}
	}
	
	/**
	 * Parses the specified JSON string to an object.
	 *   
	 * @param jsonString represents JSON string that will parse to the it's opposite object
	 * @param cls the class of the object to parse the JSON string to it.
	 * @return the parsed object.
	 * @throws JSONCustomException if an error occurs during the parsing 
	 * 			of the JSON string.
	 */
	public static <T> T unmarshalJSONToObject(String jsonString, Class<T> cls) throws JSONCustomException  {
		ObjectMapper mapper = new ObjectMapper();
		try{
			return cls.cast(mapper.readValue(jsonString, cls)) ;  
		}catch (IOException  e) {
			throw new JSONCustomException(AMQPResourceBundle.getMessage("error_AMQP004"), e);
		}
	}
}
