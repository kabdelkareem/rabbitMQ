package com.karim.examples.rabbitmq.connector;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import com.karim.examples.rabbitmq.common.enums.ContentTypeEnum;
import com.karim.examples.rabbitmq.common.enums.DeliveryModeEnum;
import com.karim.examples.rabbitmq.common.enums.MessageHeaderEnum;
import com.karim.examples.rabbitmq.connector.configures.ConnectionConfigurer;
import com.karim.examples.rabbitmq.connector.configures.ConsumerConfigurer;
import com.karim.examples.rabbitmq.connector.configures.ProducerConfigurer;
import com.karim.examples.rabbitmq.connector.exceptions.AMQPCustomException;
import com.karim.examples.rabbitmq.connector.exceptions.JAXBCustomException;
import com.karim.examples.rabbitmq.connector.exceptions.JSONCustomException;
import com.karim.examples.rabbitmq.connector.exceptions.RuntimeCustomException;
import com.karim.examples.rabbitmq.connector.exceptions.TimeoutCustomException;
import com.karim.examples.rabbitmq.connector.parser.JSONFormatter;
import com.karim.examples.rabbitmq.connector.parser.XmlFormatter;
import com.karim.examples.rabbitmq.connector.util.AMQPResourceBundle;
import com.karim.examples.rabbitmq.connector.util.Log4j;
import com.karim.examples.rabbitmq.connector.util.NetworkUtil;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

/**
 * This class contains a set of methods that operates on sending and receiving messages from 
 * a queue in different format (TEXT/ XML/ JSON). Also it can recover from different consumer
 * expected failures like consuming queue not exist in start time or deleted during listening 
 * to the queue.
 * 
 * @author Karim Abd ElKareem
 * @since 1.0
 */

public final class AMQPService {
	//Connection configurations
    private final ConnectionConfigurer connectionConfigurer;

    //Create an executor thread pool
    private final ExecutorService executorService;
    
	//Represent a connection to the queues in connection factory module
	private final Connection connection;
	
	//Default messages encoding
	private static final Charset UTF_8 = Charset.forName("UTF-8");
    
    //Available processors size
	private static final int AVAILABLE_PROCESSORS = Runtime.getRuntime().availableProcessors();
	
    //Max consumer executor pool size
	private static final int MAX_POOL_SIZE = 10;
	
	// Default waiting for a reply in seconds using pushAndWaitForReply.
	private static final long DEFAULT_WAIT_FOR_REPLY = TimeUnit.MINUTES.toSeconds(2);

	/////////////////////////////////////////// Constructor   /////////////////////////////////////	
	/**
	 * Construct the object, initialize connection factory, and start the connection
	 * 
	 * @param argsConfigurer the connection configuration
	 * @throws AMQPCustomException
	 */
	public AMQPService(final ConnectionConfigurer argsConfigurer) throws AMQPCustomException {
		// Application name must be provided
		if(argsConfigurer.getApplicationName() == null 
				|| argsConfigurer.getApplicationName().trim().isEmpty())
			throw new AMQPCustomException("Application name cannot be empty.");
		
		// Preserve the connection configuration parameters
		this.connectionConfigurer = argsConfigurer;
				
		// Start initializing the connection factory
		ConnectionFactory connectionFactory = new ConnectionFactory();

		try {
			// Setting the AMQP virtual host
			if(argsConfigurer.getVirtualHost() != null)
				connectionFactory.setVirtualHost(argsConfigurer.getVirtualHost());

			// Setting the AMQP connection credentials 
			connectionFactory.setUsername(argsConfigurer.getUsername());
			connectionFactory.setPassword(argsConfigurer.getPassword());
			
			// Setting SSL connection properties
			if(argsConfigurer.isUseSSL()) {
				if(argsConfigurer.getSslContext() != null) {
					connectionFactory.useSslProtocol(argsConfigurer.getSslContext());
				} else if(argsConfigurer.getProtocol() != null 
						&& argsConfigurer.getTrustManager() != null) {
					connectionFactory.useSslProtocol(argsConfigurer.getProtocol(), 
							argsConfigurer.getTrustManager());
				}  else if(argsConfigurer.getProtocol() != null) {
					connectionFactory.useSslProtocol(argsConfigurer.getProtocol());
				} else {
					connectionFactory.useSslProtocol();
				}
			}

			//Mark Connection as recoverable
			connectionFactory.setAutomaticRecoveryEnabled(argsConfigurer.isAutomaticRecoveryEnabled());
			connectionFactory.setNetworkRecoveryInterval(argsConfigurer.getNetworkRecoveryInterval());
			
			//Set connection heartbeat timeout
			connectionFactory.setRequestedHeartbeat(argsConfigurer.getRequestedHeartbeatTimeout());

			//Create the thread pool to be used be connection consumers
			int poolSize = AVAILABLE_PROCESSORS > MAX_POOL_SIZE ? 
					MAX_POOL_SIZE 
					: AVAILABLE_PROCESSORS;
			executorService = Executors.newFixedThreadPool(poolSize);
			
			// Establish the connection
			if(argsConfigurer.getHost() != null) {
				connectionFactory.setHost(argsConfigurer.getHost());
				connectionFactory.setPort(argsConfigurer.getPort());
				this.connection = connectionFactory.newConnection(executorService);
			} else {
				this.connection = connectionFactory.newConnection(executorService, 
						argsConfigurer.getAddresses());
			}
		} catch (KeyManagementException | NoSuchAlgorithmException e) { //Problem with SSL protocol
			close();
			
			Log4j.traceErrorException(AMQPService.class, e, e.getMessage());
			throw new AMQPCustomException(AMQPResourceBundle.getMessage("error_AMQP005"), e);
		} catch (IOException | TimeoutException e) { //Problem during establish the connection
			close();

			Log4j.traceErrorException(AMQPService.class, e, e.getMessage());
			throw new AMQPCustomException(AMQPResourceBundle.getMessage("error_AMQP006"), e);
		} catch (Exception e) { // Unknown problem
			close();

			Log4j.traceErrorException(AMQPService.class, e, e.getMessage());
			throw new AMQPCustomException(AMQPResourceBundle.getMessage("error_AMQP000"), e);
		}
	}
	
	/////////////////////////////////////////// Publish Basic Properties   ////////////////////////
    /**
	 * Enrich headers {@link Map} with some default headers
	 * 
	 * @category Producer
	 * 
     * @param headers				represent the message headers
     * @param exchangeName			represent the origin send to exchange
     * @param routingKey			represent the origin send to queue
     * @return
     */
    private Map<String, Object> enrichPublishHeaders(Map<String, Object> headers,
    		String exchangeName, 
    		String routingKey) {
    	if(headers == null)
			headers = new HashMap<String, Object>(3);
    	
		headers.put(MessageHeaderEnum.EJ_ORIGIN_ECHANGE_NAME.name(), exchangeName);
		headers.put(MessageHeaderEnum.EJ_ORIGIN_ROUTING_KEY.name(), routingKey);
		
		//Sender IP Address
		headers.put(MessageHeaderEnum.EJ_ORIGIN_IP.name(), NetworkUtil.getCurrentEnvironmentNetworkIP());
		
		return headers;
    }
    
    /**
     * Read message properties and headers
     * 
	 * @category Consumer
	 * 
     * @param properties  {@link BasicProperties} of the received message
     * @return {@link Map} contains the properties and headers in the received message
     */
	private Map<String, Object> buildHeadersFromMessageProperties(BasicProperties properties) {
		Map<String, Object> headers = new HashMap<String, Object>();
		if(properties.getHeaders() != null)
			headers.putAll(properties.getHeaders());
		
		headers.put(MessageHeaderEnum.CONTENT_TYPE.name(), properties.getContentType());
		headers.put(MessageHeaderEnum.CONTENT_ENCODING.name(), properties.getContentEncoding());
		headers.put(MessageHeaderEnum.REPLY_TO.name(), properties.getReplyTo());
		headers.put(MessageHeaderEnum.CORRELATION_ID.name(), properties.getCorrelationId());
		headers.put(MessageHeaderEnum.MESSAGE_ID.name(), properties.getMessageId());
		headers.put(MessageHeaderEnum.TIMESTAMP.name(), properties.getTimestamp());
		headers.put(MessageHeaderEnum.TYPE.name(), properties.getType());
		headers.put(MessageHeaderEnum.APPLICATION_ID.name(), properties.getAppId());
		headers.put(MessageHeaderEnum.USER_ID.name(), properties.getUserId());
		return headers;
	}
	
    /////////////////////////////////////////// Publish/Consume  //////////////////////////////////	
	/**
     * <p>Sends a message to an exchange.</p>
     * 
     * Note: if {@link ProducerConfigurer#getReplyToQueue()} is not empty, there'll be a 
     * response to the specified queue when message get consumed and  
     * {@link ProducerConfigurer#getCorrelationId()} must be not empty also.
     * 
     * 
	 * @category Producer
     * @param argsConfigurer	the producer channel configurations
     * @param headers			the headers to be send
     * @param msgObj			the message object to be send
     * @return the messageId which is a unique identifier for the message
	 * @throws AMQPCustomException there are a problem occurred during the sending of the message 
	 * @throws JAXBCustomException there are a problem during marshal msgObj to XML if 
	 * 								msgContentType is {@link ContentTypeEnum#TEXT_XML}
	 * @throws JSONCustomException there are a problem during marshal msgObj to JSON if 
	 * 								msgContentType is {@link ContentTypeEnum#TEXT_JSON}
     */	
	public <E> String push(ProducerConfigurer argsConfigurer,
			Map<String, Object> headers, 
			E msgObj) throws AMQPCustomException, JAXBCustomException, JSONCustomException {
		
		// You must specify the correlationId when you add replyTo property.
		if(argsConfigurer.getReplyToQueue() != null && argsConfigurer.getCorrelationId() == null) {
			String errorMsg = AMQPResourceBundle.getMessage("error_AMQP027");
			Log4j.traceError(AMQPService.class, errorMsg);
			throw new AMQPCustomException(errorMsg);
		}

		//  msgObj is mandatory
		if(msgObj == null) {
			String errorMsg = AMQPResourceBundle.getMessage("error_AMQP033");
			Log4j.traceError(AMQPService.class, errorMsg);
			throw new AMQPCustomException(errorMsg);
		}
		
		Channel channel = null;
		try {
			// Create channel
			channel = createChannel();
			
			// Push message
			return push(channel, argsConfigurer, headers, msgObj);
		} catch(AMQPCustomException | JAXBCustomException | JSONCustomException ex) {
			Log4j.traceErrorException(AMQPService.class, ex, ex.getMessage());
			throw ex;
		} catch(Throwable ex) {
			Log4j.traceErrorException(AMQPService.class, ex, ex.getMessage());
			throw new AMQPCustomException(ex.getMessage(), ex);
		} finally {
			closeChannel(channel);
		}
	}
	
	/**
	 * <p>Sends a message to an exchange and {@value #DEFAULT_WAIT_FOR_REPLY} seconds waiting
	 *  for reply. The reply message must be in	a JSON format.</p>
	 * 
	 * Note: it'll use {@link ProducerConfigurer#getReplyToQueue()} 
	 * and {@link ProducerConfigurer#getCorrelationId()}  if exist to wait for the reply and
	 * if one of there properties not exist, it'll generate one for you.
	 * Generated  {@link ProducerConfigurer#getReplyToQueue()} will be declare a server-named 
	 * exclusive, auto-delete, non-durable queue.
	 * Generated {@link ProducerConfigurer#getCorrelationId()} will be a random unique number
	 * generated using {@link UUID#randomUUID()}.
     * 
     * its preferred to use generated queue name as it'll avoid future annoying problems. 
     * 
     * current user on the connection must have read/write privilege 
     * on the {@link ProducerConfigurer#getReplyToQueue()}
     * 
	 * @category Producer
     * @param argsConfigurer		the producer channel configurations
     * @param headers				the headers to be send
     * @param msgObj				the message object to be send
     * @param returnClass			the expected return class
     * 
	 * @return	null				if reply empty
	 * 			otherwise			return the object from parameter returnClass type
	 * 
	 * @throws AMQPCustomException	there are a problem occurred during the sending of the message
	 * 									or receiving and parsing the reply
	 * @throws JAXBCustomException	there are a problem during marshal msgObj to XML if
	 * 									msgContentType is {@link ContentTypeEnum#TEXT_XML}
	 * @throws JSONCustomException	there are a problem during marshal msgObj to JSON if 
	 * 									msgContentType is {@link ContentTypeEnum#TEXT_JSON}
	 * @throws TimeoutCustomException when timeout elapsed
	 * @see #pushAndWaitForReply(ProducerConfigurer, Map, Object, Class, long)
     */	
	public <E, R> R pushAndWaitForReply(ProducerConfigurer argsConfigurer,
			Map<String, Object> headers, 
			E msgObj, 
			Class<R> returnClass) 
			throws AMQPCustomException, JAXBCustomException, JSONCustomException, TimeoutCustomException {
		return pushAndWaitForReply(argsConfigurer, 
				headers, 
				msgObj, 
				returnClass, 
				DEFAULT_WAIT_FOR_REPLY);
	}
	
	/**
	 * <p>Sends a message to an exchange and waiting for reply. The reply message must be in 
	 * a JSON format.</p>
	 * 
	 * Note: 
	 * it'll use {@link ProducerConfigurer#getReplyToQueue()} 
	 * and {@link ProducerConfigurer#getCorrelationId()}  if exist to wait for the reply and
	 * if one of there properties not exist, it'll generate one for you.
	 * 
	 * Generated  {@link ProducerConfigurer#getReplyToQueue()} will be declare a server-named 
	 * exclusive, auto-delete, non-durable queue.
	 * Generated {@link ProducerConfigurer#getCorrelationId()} will be a random unique number
	 * generated using {@link UUID#randomUUID()}.
     * 
     * its preferred to use generated queue name as it'll avoid future annoying problems. 
     * 
     * current user on the connection must have read/write privilege 
     * on the {@link ProducerConfigurer#getReplyToQueue()}
     * 
	 * @category Producer
	 * @param argsConfigurer		the producer configuration
     * @param headers				the headers to be send
     * @param msgObj				the message object to be send
     * @param returnClass			the expected return class
     * @param waitForReplyTimeout	the timeout for waiting for the reply in seconds
     * 
	 * @return	null				if reply empty
	 * 			otherwise			return the object from parameter returnClass type
	 * 
	 * @throws AMQPCustomException	there are a problem occurred during the sending of the message
	 * 									or receiving and parsing the reply
	 * @throws JAXBCustomException	there are a problem during marshal msgObj to XML if 
	 * 									msgContentType is {@link ContentTypeEnum#TEXT_XML}
	 * @throws JSONCustomException	there are a problem during marshal msgObj to JSON if 
	 * 									msgContentType is {@link ContentTypeEnum#TEXT_JSON}
	 * @throws TimeoutCustomException when timeout elapsed
	 * @see #pushAndWaitForReply(ProducerConfigurer, Map, Object, Class)
	 * @see #push(Channel, ProducerConfigurer, Map, Object) 
	 * 		and {@link #waitingForReply(Channel, String, String, Class, long)}
     */			
	public <E, R> R pushAndWaitForReply(ProducerConfigurer argsConfigurer,
			Map<String, Object> headers, 
			E msgObj, 
			Class<R> returnClass, 
			long waitForReplyTimeout) 
			throws AMQPCustomException, JAXBCustomException, JSONCustomException, TimeoutCustomException {
		

		//  returnClass mandatory
		if(returnClass == null) {
			String errorMsg = AMQPResourceBundle.getMessage("error_AMQP031");
			Log4j.traceError(AMQPService.class, errorMsg);
			throw new AMQPCustomException(errorMsg);
		}

		//  msgObj is mandatory
		if(msgObj == null) {
			String errorMsg = AMQPResourceBundle.getMessage("error_AMQP033");
			Log4j.traceError(AMQPService.class, errorMsg);
			throw new AMQPCustomException(errorMsg);
		}
		
		Channel channel = null;
		try {
			channel = createChannel();
			
			// Set the replyToQueue
			String replyQueueName = argsConfigurer.getReplyToQueue();
			if(replyQueueName == null) {
				try {
					replyQueueName = channel.queueDeclare().getQueue();
				} catch (Throwable e) {
					Log4j.traceErrorException(AMQPService.class, e, e.getMessage());
					throw new RuntimeCustomException(AMQPResourceBundle.getMessage("error_AMQP018"), e);
				}
			}
			
			// Set the correlationId
			String correlationId = argsConfigurer.getCorrelationId();
			if(correlationId == null)
				correlationId = UUID.randomUUID().toString();

			/* Replace argsConfigurer with a new one with replyQueueName and correlationId 
			 * if one of them not exist before
			 */
			if(argsConfigurer.getReplyToQueue() == null || argsConfigurer.getCorrelationId() == null) {
				argsConfigurer = new ProducerConfigurer.
						Builder(argsConfigurer).
						withReplyToQueue(replyQueueName).
						withCorrelationId(correlationId).build();
			}
			
			// push the message
			push(channel, argsConfigurer, headers, msgObj);
			
			// Register a consumer listener to wait for the response
			return waitingForReply(channel, 
					replyQueueName, 
					correlationId, 
					returnClass, 
					waitForReplyTimeout);
		} catch(AMQPCustomException | 
				JAXBCustomException | 
				JSONCustomException |
				TimeoutCustomException ex) {
			Log4j.traceErrorException(AMQPService.class, ex, ex.getMessage());
			throw ex;
		} catch(Throwable ex) {
			Log4j.traceErrorException(AMQPService.class, ex, ex.getMessage());
			throw new AMQPCustomException(ex.getMessage(), ex);
		} finally {
			closeChannel(channel);
		}
	}

	/**
	 * <p>Sends a message to an exchange.</p>
	 * 
	 * Note: if {@link ProducerConfigurer#getReplyToQueue()} is not empty, there'll be a 
	 * response to the specified queue when message get consumed
	 * 
	 * @category Producer			
	 * @param channel				the channel to publish the message on
	 * @param argsConfigurer		the producer channel configurations
	 * @param headers				the headers to be send
	 * @param msgObj				the message object to be send
     * @return the messageId which is a unique identifier for the message
	 * @throws AMQPCustomException	there are a problem occurred during the sending of the message 
	 * @throws JAXBCustomException	there are a problem during marshal msgObj to XML if 
	 * 								msgContentType is {@link ContentTypeEnum#TEXT_XML}
	 * @throws JSONCustomException	there are a problem during marshal msgObj to JSON if 
	 * 								msgContentType is {@link ContentTypeEnum#TEXT_JSON}
	 */
	private <E> String push(Channel channel,
			ProducerConfigurer argsConfigurer,
			Map<String, Object> headers, 
			E msgObj) throws AMQPCustomException, JAXBCustomException, JSONCustomException {
		
		confirmSelect(channel);
		
		// Message Headers
		headers = enrichPublishHeaders(headers,
				argsConfigurer.getExhange(),
				argsConfigurer.getRoutingKey());
		
		// Message Properties
		String messageId = UUID.randomUUID().toString();
		BasicProperties messageProperties = new BasicProperties.Builder()
				.contentType(argsConfigurer.getMessageContentType().value())
				.contentEncoding(UTF_8.name())
				.headers(headers)
				.deliveryMode(DeliveryModeEnum.PERSISTENT.value())
				.messageId(messageId)
				.timestamp(new Date())
				.type(msgObj.getClass().getTypeName())
				.userId(this.connectionConfigurer.getUsername())
				.appId(this.connectionConfigurer.getApplicationName())
				.replyTo(argsConfigurer.getReplyToQueue())
				.correlationId(argsConfigurer.getCorrelationId())
				.build();
		
		// Message Body
		String message = null;
		if(argsConfigurer.getMessageContentType().equals(ContentTypeEnum.TEXT_XML)) {
			message = XmlFormatter.marshalObjectToXML(msgObj);
		} else if(argsConfigurer.getMessageContentType().equals(ContentTypeEnum.TEXT_JSON)) {
			message = JSONFormatter.marshalObjectToJSON(msgObj);
		} else {
			message = (String) msgObj;
		}
		
		// Publish the message
		basicPublish(channel, 
				argsConfigurer.getExhange(), 
				argsConfigurer.getRoutingKey(), 
				messageProperties, message.getBytes(UTF_8));
		
		waitForConfirmsOrDie(channel);
		
		return messageId;
	}
	
	/**
	 * Waiting for a reply synchronous after publish a message. the reply message must be in
	 *  a JSON format.
	 * 
	 * @category Producer
	 * @param channel				the channel to register the listener
	 * @param replyQueueName		the replyToQueue to wait for the response
	 * @param correlationId			the message correlationId to look for it in the message
	 * @param returnClass			the expected response class type
     * @param waitForReplyTimeout	how long to wait before giving up in seconds. 
     * 				waitForReplyTimeout <= 0, it will wait {@value #DEFAULT_WAIT_FOR_REPLY} seconds.
	 * @return	null				if reply empty
	 * 			otherwise			return the object from parameter returnClass type
	 * @throws AMQPCustomException if problem occurred as 
	 * 									a. register the consumer	
	 * 									b. queue deleted after start consuming
	 * 									c. the reply object is null
	 * @throws TimeoutCustomException when timeout elapsed
	 * @see this function used by {@link #pushWithReplySync(ProducerConfigurer, Map, Object, Class, long)}
	 * 		to add a consumer listener on a queue for the response.
	 */
	private <R> R waitingForReply(Channel channel, 
			String replyQueueName, 
			String correlationId, 
			Class<R> returnClass, 
			long waitForReplyTimeout) throws AMQPCustomException, TimeoutCustomException {

		// Used to provide the response
		final BlockingQueue<Object> response = new ArrayBlockingQueue<Object>(1);
		
		/*
		 *  Used to stop the consumption when the message received or something went wrong
		 *  and make sure that response will offered only once.
		 */
		final AtomicBoolean stillWaiting = new AtomicBoolean(true);

		try {
			// receive only one message per time
			basicQos(channel, 1);
			
			// register and start the consume handler
			basicConsume(channel, replyQueueName, false, new DefaultConsumer(channel) {
				
				@Override
				public void handleDelivery(String consumerTag,
						Envelope envelope, 
						AMQP.BasicProperties properties,
						byte[] body) throws IOException {
					
					/**
					 * stop consuming message as response already provided by throwing
					 * a runtime exception to prevent cycle iteration
					 */
					if(!stillWaiting.get())
						throw new RuntimeCustomException(AMQPResourceBundle.getMessage("error_AMQP032"));
					
					// Response object to offer
					Object responseObj = null;
					
					// Check is it the right reply
					if (properties.getCorrelationId() != null
							&& properties.getCorrelationId().equals(correlationId)) { 
						
						/*
						 *  parse the object to supposed returnClass and offer it to stop blocking
						 *  if a problem occurs during parsing the response, the offered response
						 *  with be the message as is. Any other problem will offer an exception
						 *  with error details 
						 */
						if(body == null || body.length == 0) {
							responseObj = "null";
						} else {
							// Get the message
							String message = new String(body, UTF_8);
							
							try {
								responseObj = JSONFormatter.unmarshalJSONToObject(message, returnClass);
							} catch(JSONCustomException e) {
								responseObj = new AMQPCustomException(
										AMQPResourceBundle.getMessage("error_AMQP032") + message, 
										e);
							} catch(Throwable e) {
								responseObj = new AMQPCustomException(e.getMessage(), e);
							}
						}

						 // send an acknowledge to current queue to remove the message
						this.getChannel().basicAck(envelope.getDeliveryTag(), false);
						
					 } else { // if not the right reply
						 /**
						  * push it back to the queue using a new channel to
						  * prevent publishing problems from reflecting to our
						  * current channel
						  */
						 Channel newChannel = null;
						 try {
							 newChannel = createChannel();
							
							 // Define the confirm on the channel before publishing the message.
							 confirmSelect(newChannel);
								
							 // publish the message again
							 newChannel.basicPublish("", replyQueueName, properties, body);
							 
							 //  wait for a confirmation that all messages published successfully.
							 waitForConfirmsOrDie(newChannel);
								
							 // send an acknowledge to current queue to remove the message
							 this.getChannel().basicAck(envelope.getDeliveryTag(), false);
						 } catch(Exception e) {
							 Log4j.traceErrorException(AMQPService.class, e, e.getMessage());
							 
							 /*
							  * if problem occurs, keep the message in the queue and stop
							  * to prevent cycle iteration on same message
							  */

							 this.getChannel().basicReject(envelope.getDeliveryTag(), true);
							 responseObj = new AMQPCustomException(AMQPResourceBundle.getMessage("error_AMQP026"));
						 } finally {
							 closeChannel(newChannel);
						 }
					 }
					
					/**
					 * if response available and no response offered before,
					 * offer the response
					 */
					
					if(responseObj != null 
							&& stillWaiting.compareAndSet(true, false)) {
						response.offer(responseObj);
					}
				}
				
				/**
				 * there are various reasons which could cause the consumption to stop. One of 
				 * these is obviously if the client issues a basic.cancel on the same channel, 
				 * which will cause the consumer to be cancelled and the server replies with 
				 * a basic.cancel-ok. Other events, such as the queue being deleted, or in a 
				 * clustered scenario, the node on which the queue is located failing, will 
				 * cause the consumption to be cancelled, but the client channel will not 
				 * be informed
				 */
				@Override
				public void handleCancel(String consumerTag) throws IOException {
					if(stillWaiting.compareAndSet(true, false))
						response.offer(new AMQPCustomException(AMQPResourceBundle.getMessage("error_AMQP024")));
				}
			});
		} catch (AMQPCustomException e) { 
			/*
			 *  if a problem occurs during register the consumer, exception details will be
			 *  offered
			 */
			response.offer(e);
		}
        
		try {
			// Set waitForReplyTimeout to DEFAULT_WAIT_FOR_REPLY seconds if invalid value provided.
			if(waitForReplyTimeout <= 0) {  
				waitForReplyTimeout = DEFAULT_WAIT_FOR_REPLY;
			}
			
			// block with timeout for the response
			Object responseObj = response.poll(waitForReplyTimeout, TimeUnit.SECONDS);
			
			//return the offered response or throw the offered exception
			if(responseObj == null) { //if timeout elapsed
				stillWaiting.set(false);
				throw new TimeoutCustomException(AMQPResourceBundle.getMessage("error_AMQP029"));
			} else if(responseObj instanceof AMQPCustomException) { // if something went wrong.
				throw (AMQPCustomException) responseObj;
			} else if(responseObj instanceof String 
					&& ((String)responseObj).equals("null")) { // if reply is empty 'null'
				return null;
			} else { // if there a reply
				return returnClass.cast(responseObj);
			}
		} catch (InterruptedException e) {
			//Re-interrupt the thread to set the interrupt flag
			Thread.currentThread().interrupt();

			throw new AMQPCustomException(AMQPResourceBundle.getMessage("error_AMQP025"));
		}
	}
	
	/**
	 * <p>Add receive message listener that parse message based on value of content_type 
	 * message property. The xml/json messages in a single queue expected to be from the 
	 *same class type.</p> 
	 * 
	 * @category Consumer
	 * @param argsConfigurer    represents the consumer configurations
	 * @param handler			handling business code
	 * @param msgObjClass		represent the object that message will un-marshal to. 
	 * 		
	 * @throws AMQPCustomException	if problem happened during registering the consumer
	 */
	public <E, R> void setReceiveMessageListener(final ConsumerConfigurer argsConfigurer,
			final MessageHandler<E, R> handler,
			final Class<E> msgObjClass) throws AMQPCustomException {
		
		//  msgObjClass mandatory
		if(msgObjClass == null) {
			String errorMsg = AMQPResourceBundle.getMessage("error_AMQP030");
			Log4j.traceError(AMQPService.class, errorMsg);
			throw new AMQPCustomException(errorMsg);
		}
		
		for(int i = 0; i < argsConfigurer.getNoOfConumers(); i++) {
			try {
				final Channel channel = createChannel();
				
				basicQos(channel, argsConfigurer.getPrefetchCount());
				
				basicConsumeWithRetryRecovery(channel, 
						argsConfigurer, 
						new RabbitConsumer(channel, this, argsConfigurer) {
					
					@Override
					public void handleDelivery(String consumerTag, 
							Envelope envelope, 
							AMQP.BasicProperties properties, 
							byte[] body) throws IOException {
						Map<String, Object> headers = buildHeadersFromMessageProperties(properties);
						String msgText = new String(body, UTF_8);

						Object replyToQueue = headers.remove(MessageHeaderEnum.REPLY_TO.name());
						Object correlationId = headers.remove(MessageHeaderEnum.CORRELATION_ID.name());
						Object messageContentType = headers.remove(MessageHeaderEnum.CONTENT_TYPE.name());
						
						/* 
						 * if message content_type property is empty, use the consumer 
						 * contentType configuration if exist
						 */
						if(messageContentType == null && argsConfigurer.getContentType() != null) {
							messageContentType = argsConfigurer.getContentType().value();
						}
						
						try {
							// Validate the content type property
							if(msgObjClass != String.class 
									&& (messageContentType == null 
									|| messageContentType.equals(ContentTypeEnum.TEXT_PLAIN.value())))
								throw new AMQPCustomException(AMQPResourceBundle.getMessage("error_AMQP023"));
							
							// Un-marshal the message according to content type property in the message
							E messageUnmarshal = null;
							if(messageContentType != null 
									&& messageContentType.equals(ContentTypeEnum.TEXT_XML.value())) {
								messageUnmarshal = XmlFormatter.unmarshalXMLToObject(msgText, msgObjClass);
							} else if(messageContentType != null 
									&& messageContentType.equals(ContentTypeEnum.TEXT_JSON.value())) {
								messageUnmarshal = JSONFormatter.unmarshalJSONToObject(msgText, msgObjClass);
							} else {
								messageUnmarshal = msgObjClass.cast(msgText);
							}
							
							R returnObj = handler.handleDelivery(messageUnmarshal, headers);
							
							// If reply-to property exist, send the reply
							if(replyToQueue != null && !((String) replyToQueue).isEmpty())
								pushReply(this.getConsumerConfigurer().getQueueName(),
										((String) replyToQueue),
										((String) correlationId) ,
										returnObj);

							// Acknowledge success to remove message from the queue
							if(!this.getConsumerConfigurer().isAutoAck()) {
								this.getChannel().basicAck(envelope.getDeliveryTag(), false);
							}
						} catch(Throwable e) {
							// If reply-to property exist, send empty reply
							if(replyToQueue != null && !((String) replyToQueue).isEmpty()) {
								// Try to stop producer from waiting for a response
								pushReply(this.getConsumerConfigurer().getQueueName(), 
										((String) replyToQueue), 
										((String) correlationId) ,
										null);

								// Acknowledge success to remove message from the queue
								if(!this.getConsumerConfigurer().isAutoAck()) {
									this.getChannel().basicAck(envelope.getDeliveryTag(), false);
								}
							} else {
								// Acknowledge failure to keep message in the queue or move to dead-letter
								if(!this.getConsumerConfigurer().isAutoAck()) {
									this.getChannel().basicReject(envelope.getDeliveryTag(), false);
								}
							}

							Object messageId = headers.get(MessageHeaderEnum.MESSAGE_ID.name());
							if(messageId != null)
								Log4j.traceErrorException(AMQPService.class, e,
										"Exception during handling the message: " + messageId);
							else
								Log4j.traceErrorException(AMQPService.class, e, e.getMessage());
							
						}
					}
				});
			} catch(AMQPCustomException ex) {
				Log4j.traceErrorException(AMQPService.class, ex, ex.getMessage());
				throw ex;
			} catch(Throwable ex) {
				Log4j.traceErrorException(AMQPService.class, ex, ex.getMessage());
				throw new AMQPCustomException(ex.getMessage(), ex);
			} finally {
				/* 
				 * Don't close the channel as it will be closed explicitly when the connection 
				 * closed or when exception fired on the channel
				 */
			}
		}
	}
	
	/**
	 * Handle the reply-to the publisher queue.
	 * 
	 * @param responeFromQueue	the source queue 
	 * @param replyToQueue		the queue to send the reply.
	 * @param correlationId		message identifier
	 * @param returnObj			
	 * 
	 * @see this function used by {@link #setReceiveMessageListener(ConsumerConfigurer, MessageHandler, Class...)}
	 * 		to respond on a message with replyTo header
	 */
	private <R> void pushReply(String responeFromQueue, 
			String replyToQueue, 
			String correlationId, 
			R returnObj) {
		
		Channel replyChannel = null;
		try {
			replyChannel = createChannel();
			confirmSelect(replyChannel);

			Map<String, Object> responseHeaders = enrichPublishHeaders(null, "", responeFromQueue);
			
			byte[] messageBody = new byte[] {};
			if(returnObj != null)
				messageBody = JSONFormatter.marshalObjectToJSON(returnObj).getBytes(UTF_8);
			

			// Message Properties
			BasicProperties messageProperties = new BasicProperties.Builder()
					.contentType(ContentTypeEnum.TEXT_JSON.value())
					.contentEncoding(UTF_8.name())
					.headers(responseHeaders)
					.deliveryMode(DeliveryModeEnum.PERSISTENT.value())
					.messageId(UUID.randomUUID().toString())
					.timestamp(new Date())
					.type(returnObj!= null &&  returnObj.getClass() != null? 
							returnObj.getClass().getTypeName() 
							: null)
					.userId(this.connectionConfigurer.getUsername())
					.appId(this.connectionConfigurer.getApplicationName())
					.correlationId(correlationId)
					.build();
			
			basicPublish(replyChannel, "", ((String) replyToQueue), messageProperties, messageBody);
			
			waitForConfirmsOrDie(replyChannel);
		} catch (Throwable ignoreEx) {
			Log4j.traceErrorException(AMQPService.class, ignoreEx, ignoreEx.getMessage());
		} finally {
			closeChannel(replyChannel);
		}
	}
	/////////////////////////////////////////// Encapsulate Channel Functions /////////////////////
	/**
	 * publish message to the broker through a channel
	 * 
	 * @category Producer
	 * @param channel		channel to publish the message to
	 * @param exchange 		exchange to publish the message to
	 * @param routingKey	the routing key
	 * @param props			other properties for the message - routing headers etc
	 * @param message		the message body
	 * @throws AMQPCustomException if an error is encountered
	 */
	private void basicPublish(Channel channel,
			String exchange, 
			String routingKey, 
			BasicProperties props, 
			byte[] message) throws AMQPCustomException {
		try {
			channel.basicPublish(exchange, routingKey, props, message);
		} catch (IOException e) {
			throw new AMQPCustomException(AMQPResourceBundle.getMessage("error_AMQP014"), e);
		}
	}
	
	/**
	 * define the confirm on the channel before publishing the message to the broker
	 * 
	 * @category Producer
	 * @param channel the channel to confirmSelect
	 * @throws AMQPCustomException if an error is encountered
	 */
	private void confirmSelect(Channel channel) throws AMQPCustomException {
		try {
			channel.confirmSelect();
		} catch (IOException e) {
			throw new AMQPCustomException(AMQPResourceBundle.getMessage("error_AMQP010"), e);
		}
	}
	
	/**
	 * wait for a confirmation that all messages published successfully to the broker
	 * 
	 * @category Producer
	 * @param channel the channel to waitForConfirmsOrDie
	 * @throws AMQPCustomException if an error is encountered
	 */
	private void waitForConfirmsOrDie(Channel channel) throws AMQPCustomException {
		try {
			channel.waitForConfirmsOrDie();
		} catch (IOException e) {
			throw new AMQPCustomException(AMQPResourceBundle.getMessage("error_AMQP011"), e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new AMQPCustomException(AMQPResourceBundle.getMessage("error_AMQP012"), e);
		}
	}
	
	/**
	 * wait for a confirmation that all messages published successfully to the broker or 
	 * exceed the timeout
	 * 
	 * @category Producer
	 * @param channel the channel to waitForConfirmsOrDie
	 * @param timeout timeout to wait
	 * @throws AMQPCustomException if an error is encountered
	 */
	@SuppressWarnings(value="unused")
	private void waitForConfirmsOrDie(Channel channel, long timeout) throws AMQPCustomException {
		try {
			channel.waitForConfirmsOrDie(timeout);
		} catch (IOException e) {
			throw new AMQPCustomException(AMQPResourceBundle.getMessage("error_AMQP011"), e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new AMQPCustomException(AMQPResourceBundle.getMessage("error_AMQP012"), e);
		} catch (TimeoutException e) {
			throw new AMQPCustomException(AMQPResourceBundle.getMessage("error_AMQP013"), e);
		}
	}
	
	/**
     * Request a specific prefetchCount "quality of service" settings
     * for this channel.
     * 
	 * @category Consumer
	 * @param channel		the channel to set the prefetchCount
	 * @param prefetchCount	the fetch count of messages 
	 * @throws AMQPCustomException if an error is encountered
	 */
	private void basicQos(Channel channel, int prefetchCount) throws AMQPCustomException {
		try {
			channel.basicQos(prefetchCount);
		} catch (IOException e) {
			throw new AMQPCustomException(AMQPResourceBundle.getMessage("error_AMQP015"), e);
		}
	}
	
	/**
	 * Basic consumer with retry recovery each {@link ConnectionConfigurer#getNetworkRecoveryInterval()}
	 * 
	 * @category Consumer
	 * @param channel		the channel to register the consumer to
	 * @param prefetchCount	the fetch count of messages 
	 * @param queueName		the name of the queue
	 * @param autoAck		true if the server should consider messages acknowledged once delivered; 
	 * 						false if the server should expect explicit acknowledgments
	 * @param consumer		the consumed message handle
	 * @see #basicConsume(Channel, String, boolean, RabbitConsumer) 
	 */
	void basicConsumeWithRetryRecovery(Channel channel, 
			ConsumerConfigurer consumerConfigure, 
			RabbitConsumer consumer) {
		
		try {
			basicConsume(channel, consumerConfigure.getQueueName(), consumerConfigure.isAutoAck(), consumer);
		} catch (AMQPCustomException e) {
			Log4j.traceErrorException(AMQPService.class, e, AMQPResourceBundle.getMessage("error_AMQP016"));
		
			StartConsumerCallable startConsumerCallable 
				= new StartConsumerCallable(this.connectionConfigurer.getNetworkRecoveryInterval(), 
						consumerConfigure,
						consumer);
	        executorService.submit(startConsumerCallable);
		}
	}

	/**
	 * Start a basic consumer
	 * 
	 * @category Consumer
	 * @param channel	the channel to register the consumer to.
	 * @param queueName	the name of the queue
	 * @param autoAck	true if the server should consider messages acknowledged once delivered
	 * 					false if the server should expect explicit acknowledgments
	 * @param consumer  the consumed message handle
	 * @throws Exception  if an error is encountered
	 */
	private void basicConsume(Channel channel, 
			String queueName, 
			boolean autoAck, 
			Consumer consumer) throws AMQPCustomException {
		try {
			channel.basicConsume(queueName, autoAck, consumer);
			
		} catch (Exception e) {
			throw new AMQPCustomException(AMQPResourceBundle.getMessage("error_AMQP028"), e);
		}
	}
	
	/**
	 * close the channel
	 * 
	 * @category Common
	 * @param channel	the channel to be closed
	 * @throws AMQPCustomException  if an error is encountered
	 */
	private void closeChannel(Channel channel) {
		try {
			if(channel != null && channel.isOpen())
				channel.close();
		} catch (IOException | TimeoutException e) {
			Log4j.traceErrorException(AMQPService.class, e, e.getMessage());
			
		} finally {
			channel = null;
		}
	}
	
	///////////////////////////////////////// Encapsulate Connection Functions ////////////////////
	/**
	 * create a channel from the defined connection
	 * 
	 * @category Common
	 * 
     * Create a new channel, using an internally allocated channel number.
     * If automatic connection recovery is enabled, 
     * the channel returned by this method will be recoverable.
     *
	 * @return	a new channel descriptor, or null if none is available
	 * @throws AMQPCustomException if an I/O problem is encountered
	 */
	private Channel createChannel() throws AMQPCustomException {
		try {
			return connection.createChannel();
		} catch (IOException e) {
			throw new AMQPCustomException(AMQPResourceBundle.getMessage("error_AMQP008"), e);
		}
	}

	/**
	 * Close the connection to the broker and the executer services (threads)
	 * 
	 * @category Common
	 */
	public void close() {	
		//Close the connection and therefore the opened channels will be receive a shutdown signal
		try {
			if(connection != null && connection.isOpen())
				connection.close(connectionConfigurer.getConnectionTimeout());
		} catch (IOException e) {
			// Do nothing
			Log4j.traceErrorException(AMQPService.class, e, e.getMessage());
		}

		//Close the executer services to interrupt the current active threads.
		if (executorService != null) {
			executorService.shutdownNow();
			 try {
				 if(!executorService.awaitTermination(2, TimeUnit.SECONDS)) {
					 Log4j.traceError(AMQPService.class, "Not all thread closed properly in the executor thread.");
				 }
			} catch (InterruptedException e) {
				// Do nothing
				Thread.currentThread().interrupt();
			}
        }
	}
	
	////////////////////////////////////////
	/**
     * Task in charge of opening connection and adding listener when consumer is
     * started and queue is not available or queue deleted during listening to it.
     * 
     * 
	 * @category Consumer
     */
    private class StartConsumerCallable implements Callable<Void> {
    	// Retry interval in milliseconds
        private final long _connectionRetryInterval;
        // Consumer queue configuration
        private final ConsumerConfigurer _consumerConfigure;
        // Consumer
        private final RabbitConsumer _consumer;

        /**
         * Initialize the local variables
         * 
         * @param connectionRetryInterval	Retry interval in milliseconds
         * @param consumerConfigure			Consumer queue configuration
         * @param consumer					Consumer handler
         */
        StartConsumerCallable(long connectionRetryInterval, 
        		ConsumerConfigurer consumerConfigure, 
        		RabbitConsumer consumer) {
            this._connectionRetryInterval = connectionRetryInterval;
            this._consumerConfigure = consumerConfigure;
            this._consumer = consumer;
        }

        @Override
        public Void call() {
            boolean connectionFailed = true;
            try {
				// first sleep before start retrying
            	Thread.sleep(_connectionRetryInterval);
				
	            // Reconnection loop
	            while (connectionFailed && !Thread.currentThread().isInterrupted()) {
	                try {
	                	//open a new channel instead of last channel because its closed explicitly due to the thrown exception
	                	Channel openedChannel = createChannel();
	            		basicQos(openedChannel, _consumerConfigure.getPrefetchCount());
	            		_consumer.setChannel(openedChannel);
	            		
	                    //reconnect
	        			basicConsume(openedChannel, 
	        					_consumerConfigure.getQueueName(), 
	        					_consumerConfigure.isAutoAck(), 
	        					_consumer);
	        			
	        			// stop retry if reconnect success
	                    connectionFailed = false;
	                } catch (Exception e) { //retry failed
	                	Log4j.traceErrorException(StartConsumerCallable.class, e, 
	                			"Connection failed, will retry in " + _connectionRetryInterval + "ms");
	                    Thread.sleep(_connectionRetryInterval);
	                }
	            }
			} catch (InterruptedException ex) {
				Log4j.traceErrorException(StartConsumerCallable.class, ex, "Connection retry for queue " 
								+ this._consumerConfigure.getQueueName() 
								+" failed due to thread interrupted.");
				
            	// re-interrupt the thread again because catching InterruptedException will clear interrupted flag
            	Thread.currentThread().interrupt();
			}
            return null;
        }
    }
}
