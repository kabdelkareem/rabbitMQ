package com.karim.examples.rabbitmq.connector.parser;


import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.Unmarshaller;

import com.karim.examples.rabbitmq.connector.exceptions.JAXBCustomException;
import com.karim.examples.rabbitmq.connector.util.AMQPResourceBundle;

/**
 * This class consists exclusively of static methods that operates on parsing 
 * the object to it's XML representation and vice-versa.
 * 
 * This class uses the library <i>JAX-B</i>, it's exist native from <i>Java SE 6</i>.
 * 
 * @author Karim Abd ElKareem
 * @since 1.2
 */

public class XmlFormatter  {

	/**
	 * Parses the specified object to it's XML representation.
	 * 
	 * @param obj is the object to be parsed.
	 * @return a XML string representation of the parsed object
	 * @throws JAXBCustomException if an error occurs during the parsing 
	 * 			of the object.
	 */
	public static String marshalObjectToXML(Object obj) throws JAXBCustomException {
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance(obj.getClass());
			
			Marshaller marshaller = jaxbContext.createMarshaller();

			// output pretty printed
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

			StringWriter sw = new StringWriter();
			marshaller.marshal(obj, sw);
			return sw.toString();
		} catch (PropertyException e) {
			throw new JAXBCustomException(AMQPResourceBundle.getMessage("error_AMQP001"), e);
		} catch (JAXBException e) {
			throw new JAXBCustomException(AMQPResourceBundle.getMessage("error_AMQP001"), e);
		}
	}
	

	/**
	 * Parses the specified XML string to an object.
	 * 
	 * @param xmlString represents XML string that will parse to the it's opposite object
	 * @param ObjClass the class describe the object' types
	 * @return the parsed object.
	 * @throws JAXBCustomException if an error occurs during the parsing 
	 * 			of the XML string.
	 */
	public static <E> E unmarshalXMLToObject(String xmlString, Class<E> ObjClass) throws JAXBCustomException   {	
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance(ObjClass);
			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
			StringReader reader = new StringReader(xmlString);
			return ObjClass.cast(jaxbUnmarshaller.unmarshal(reader));
		} catch (JAXBException e) {
			throw new JAXBCustomException(AMQPResourceBundle.getMessage("error_AMQP002"), e);
		}
	}
}
