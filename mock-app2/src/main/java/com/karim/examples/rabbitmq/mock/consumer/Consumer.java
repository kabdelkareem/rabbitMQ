package com.karim.examples.rabbitmq.mock.consumer;

import java.util.concurrent.TimeUnit;

import com.karim.examples.rabbitmq.connector.AMQPService;
import com.karim.examples.rabbitmq.connector.configures.ConnectionConfigurer;
import com.karim.examples.rabbitmq.connector.configures.ConsumerConfigurer;
import com.karim.examples.rabbitmq.connector.exceptions.AMQPCustomException;

public class Consumer {

	public static void main(String[] args) throws InterruptedException {
		// Listening to a queue for 9 seconds.
		AMQPService amqpService = null;

		try {
			// Connection Configuration
			ConnectionConfigurer connectionConfigurer =
					new ConnectionConfigurer.Builder("app2",
							"localhost",
							"Test",
							"app2",
							"app2").build();
		
			
			// Start the connection
			amqpService = new AMQPService(connectionConfigurer);
			
			// Consumer Configuration
			ConsumerConfigurer ConsumerConfigurer = 
					new ConsumerConfigurer.Builder("companyName.qu.app2.AddItem").build();
			
			// Register & Start the consumer
			amqpService.setReceiveMessageListener(ConsumerConfigurer,
					(msg, headers) -> {
						System.out.println(msg);
						return null;
					},
					String.class);
			
			// Sleep for 9 seconds before enter finally clause
			Thread.sleep(TimeUnit.SECONDS.toMillis(9));
		} catch (AMQPCustomException e) {
			// Print exception message
			e.printStackTrace();
			
		} finally {		
			// Close the connection and opened channels.
			if(amqpService != null)
				amqpService.close();
		}
			
				
	}

}
