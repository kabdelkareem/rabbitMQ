package com.karim.examples.rabbitmq.mock.publisher;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.karim.examples.rabbitmq.common.enums.ContentTypeEnum;
import com.karim.examples.rabbitmq.connector.AMQPService;
import com.karim.examples.rabbitmq.connector.configures.ConnectionConfigurer;
import com.karim.examples.rabbitmq.connector.configures.ProducerConfigurer;
import com.karim.examples.rabbitmq.connector.exceptions.AMQPCustomException;
import com.karim.examples.rabbitmq.connector.exceptions.JAXBCustomException;
import com.karim.examples.rabbitmq.connector.exceptions.JSONCustomException;

public class Publisher {

	public static void main(String[] args) throws InterruptedException {
		// publishing a message every 2 seconds for 9 seconds so it'll be 5 messages.
		ScheduledExecutorService executor = null;
		
		try {
			executor = Executors.newSingleThreadScheduledExecutor();
			// Configure & Start the executer task
			executor.scheduleAtFixedRate(() -> {
				AMQPService amqpService = null;
				try {
					// Connection Configuration
					ConnectionConfigurer connectionConfigurer = 
							new ConnectionConfigurer.Builder("app1", 
									"localhost",
									"Test",
									"app1",
									"app1").build();
					
					// Start the connection
					amqpService = new AMQPService(connectionConfigurer);
					
					// Publisher Configuration
					ProducerConfigurer producerConfigurer = 
							new ProducerConfigurer.Builder("companyName.ex.app2", "app1.AddItem").
									withMessageContentType(ContentTypeEnum.TEXT_JSON).
									build(); 
					
					// Publish the message
					DateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss"); 
					amqpService.push(producerConfigurer, null, "Time now: " + df.format(new Date()));
				} catch (AMQPCustomException e) {
					// General Exception during publishing the message
					e.printStackTrace();
				} catch (JAXBCustomException | JSONCustomException e) {
					// Parsing Exception
					e.printStackTrace();
				} finally {
					// Close the connection
					if(amqpService != null)
						amqpService.close();
				}
			}, 0, 2, TimeUnit.SECONDS);
			
			// Sleep for 9 seconds to let executor publish 5 messages
			Thread.sleep(TimeUnit.SECONDS.toMillis(9));
		} finally {
			// Shutdown the executer
			if(executor != null ) {
				executor.shutdown();
				
				// Wait for running task to finish
			    if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
			        System.out.println("Still waiting...");
					executor.shutdownNow();
			        System.exit(0);
			    }
			    System.out.println("Exiting normally...");
			}
		}
	}

}
