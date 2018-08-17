package com.karim.examples.rabbitmq.installer;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.karim.examples.rabbitmq.installer.model.Binding;
import com.karim.examples.rabbitmq.installer.model.Exchange;
import com.karim.examples.rabbitmq.installer.model.Policy;
import com.karim.examples.rabbitmq.installer.model.Queue;


public class AMQPProperties {
	private static final String BASE_DIR = "";
	
	private Set<Exchange> exchanges;
	private Set<Queue> queues;
	private Set<Binding> bindings;
	private Set<Policy> policies;
	private Map<String, Set<String>> users;
	
	public AMQPProperties() {
		exchanges = new HashSet<Exchange>();
		queues = new HashSet<Queue>();
		bindings = new HashSet<Binding>();
		policies = new HashSet<Policy>();
		users = new HashMap<String, Set<String>>();
	}
	
	public void load(String fileName) throws IOException {
		// Validating file name
		String[] apps = fileName.split("-");
		if(apps.length != 2) 
			throw new IOException("Wrong file name " + fileName);
		
		
		//Loading properties
		ClassLoader classLoader = AMQPProperties.class.getClassLoader();
		Properties prop = new Properties();
		InputStream input = null;
		try {
			input = classLoader.getResourceAsStream(BASE_DIR + fileName + ".properties");
			if (input == null) {
				//Try swap application names
				fileName = apps[1]+"-"+apps[0];
				
				input = classLoader.getResourceAsStream(BASE_DIR + fileName + ".properties");
				if(input == null) 
					throw new IOException("Unable to find " + fileName);
			}
			prop.load(input);
			
			//Add users with default name 
			users.putIfAbsent(apps[0], new HashSet<String>());
			users.putIfAbsent(apps[1], new HashSet<String>());
			
			//Load common configuration files
			if(prop.getProperty("useConfigs") != null) {
				String[] files = prop.getProperty("useConfigs").split(",");
				for(int i = 0; i < files.length; i++) {
					Properties configProperties = new Properties();
					configProperties.load(classLoader.getResourceAsStream(files[i]+".properties"));
					prop.putAll(configProperties);
				}
			}
			
			//Load configurations
			Enumeration<Object> keySet = prop.keys();
			while (keySet.hasMoreElements()) {
				String keyName = (String) keySet.nextElement();
				String keyValue = prop.getProperty(keyName);

				if (keyName.matches("exchange/.*")) {
					String exchangeName = keyName.substring(keyName.lastIndexOf("/")+1);
					exchanges.add(new Exchange(exchangeName, keyValue));
				}
				if (keyName.matches("queue/.*")) {
					String queueName = keyName.substring(keyName.lastIndexOf("/")+1);
					queues.add(new Queue(queueName, keyValue));
				}
				if (keyName.matches("bind/.*")) {
					String[] bindingInformation = keyName.split("/");
					bindings.add(new Binding(bindingInformation[1], bindingInformation[2], keyValue));
					
					// Add the privilege to each user
					if(bindingInformation[1].startsWith(AMQPConfiguration.companyName+".ex.") &&
							bindingInformation[1].split("\\.").length == 3) { //avoid global configuration file
						String userToAccess = keyValue.split("\\.")[0];
						String exchangeToAccess = bindingInformation[1];
						
						users.get(userToAccess).add(exchangeToAccess);
					}
				}
				
				if (keyName.matches("policy/.*")) {
					policies.add(new Policy(keyName.split("/")[1], keyValue));
				}

			}
			
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
				}
			}
		}
	}

	public Set<Exchange> getExchanges() {
		return exchanges;
	}

	public Set<Queue> getQueues() {
		return queues;
	}

	public Set<Binding> getBindings() {
		return bindings;
	}

	public Set<Policy> getPolicies() {
		return policies;
	}
	
	public Map<String, Set<String>> getUsers() {
		return users;
	}

}