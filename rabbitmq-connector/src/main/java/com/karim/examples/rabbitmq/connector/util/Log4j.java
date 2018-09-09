package com.karim.examples.rabbitmq.connector.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 * This class manages the log files in the system
 * 
 * @author nour
 * 
 */
public class Log4j extends Logger {
	static {
		ClassLoader cl = Log4j.class.getClassLoader();
		Properties props = new Properties();
		try {
			props.clear();
			props.load(cl.
				getResourceAsStream("log4j.properties"));
			PropertyConfigurator.configure(props);
		} catch (Exception e) {
			System.out.println("Log4j : Error loading Log4j.properties");
		}
	}

	public Log4j(String name) {
		super(name);
	}

	public static void traceInfo(Class<?> cl, String msg) {
		Logger logger = Logger.getLogger(cl);
		logger.info(msg);
	}

	public static void traceDebug(Class<?> cl, String msg) {
		Logger logger = Logger.getLogger(cl);
		logger.debug(msg);
	}

	public static void traceLog(Class<?> cl, String msg) {
		Logger logger = Logger.getLogger(cl);
		logger.info(msg);
	}

	public static void traceError(Class<?> cl, String msg) {
		Logger logger = Logger.getLogger(cl);
		logger.error(msg);
	}

	public static void traceFatal(Class<?> cl, String msg) {
		Logger logger = Logger.getLogger(cl);
		logger.fatal(msg);
	}

	/**
	 * Trace Error Exception and write it into server .out file,custom .
	 * log file and send E-mail with error string, Context-Root must be 
	 * written in log4j.properties file as # <context-root>Name Here</context-root>
	 * 
	 * @param cl
	 * @param ex
	 * @param msg
	 */
	public static void traceErrorException(Class<?> cl, Throwable ex, String msg) {
		Logger logger = Logger.getLogger(cl);
		logger.error(msg, ex);
	}

	public static void traceFatalException(Class<?> cl, Exception ex, String msg) {
		Logger logger = Logger.getLogger(cl);
		logger.fatal(msg, ex);
	}

	public static void traceDebugException(Class<?> cl, Exception ex, String msg) {
		Logger logger = Logger.getLogger(cl);
		logger.debug(msg, ex);
	}
	
	/**
	 * return the error stacktrace
	 * 
	 * @param exception
	 * @return
	 */
	public static String getExceptionStackTrace(Throwable exception) {
		StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
		try {
	        exception.printStackTrace(printWriter);
	        printWriter.flush();
	        return  stringWriter.toString();
		} catch(Throwable e) {
			// Ignore the problem
		} finally {
			printWriter.close();
		}
		return null;
	}
}