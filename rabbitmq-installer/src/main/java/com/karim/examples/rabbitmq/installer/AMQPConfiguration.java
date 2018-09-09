package com.karim.examples.rabbitmq.installer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.karim.examples.rabbitmq.installer.model.Binding;
import com.karim.examples.rabbitmq.installer.model.Exchange;
import com.karim.examples.rabbitmq.installer.model.Policy;
import com.karim.examples.rabbitmq.installer.model.Queue;


public class AMQPConfiguration {
	private final String baseUrl;
	private final String username;
	private final String password;
	private final String vHost;
	private final String includeUsersAndPermissionsDefinition;
	private final List<String> configurationFiles;

	public final static String companyName = "companyName";
	public AMQPConfiguration(String baseUrl, String username, String password, String vHost, String includeUsersAndPermissionsDefinition, List<String> configurationFiles) {
		this.baseUrl = baseUrl;
		this.username = username;
		this.password = password;
		this.vHost = vHost;
		this.includeUsersAndPermissionsDefinition = includeUsersAndPermissionsDefinition;
		this.configurationFiles = configurationFiles;
	}
	
	public void install() {
		AMQPProperties properties = new AMQPProperties();
		System.out.println("Start loading configurations");
		for (int i = 0; i < configurationFiles.size(); i++) {
			try {
				properties.load(configurationFiles.get(i));
			} catch (IOException e) {
				System.err.println("Problem during loading the configuration for: " + configurationFiles.get(i));
				return;
			}
		}
		System.out.println("Finish loading configurations successfully");
		
		System.out.println("Start declaring resources...");
		createVirtualHost(this.vHost);

		for (Exchange ex : properties.getExchanges()) {
			createExchange(this.vHost, ex.getName(), ex.getProperties());
		}
		for (Queue queue : properties.getQueues()) {
			createQueue(this.vHost, queue.getName(), queue.getProperties());
		}
		for (Binding binding : properties.getBindings()) {
			bindExchangeToQueue(this.vHost, binding.getExchangeName(), binding.getQueueName(), binding.getRoutingKey());
		}

		for (Policy policy : properties.getPolicies()) {
			createPolicy(this.vHost, policy.getName(), policy.getProperties());
		}
		
		// Set the default privilege if enabled
		if(includeUsersAndPermissionsDefinition.equals("true")) {
			for(Map.Entry<String, Set<String>> entry : properties.getUsers().entrySet()) {
				String user = entry.getKey();
				Set<String> exchangesToAccess = entry.getValue();
				
				// Add user
				createUser(user);
				
				// Add permissions
				Set<String> appsToAccess = new HashSet<String>();
				
				for (String exchangeName : exchangesToAccess) {
					appsToAccess.add(exchangeName.substring(exchangeName.lastIndexOf(".") + 1));
				}
				
				AssignBasicPermissionToUser(this.vHost, user, appsToAccess);
				AssignTopicPermissionToUser(this.vHost, user, exchangesToAccess);
			}
		}

		System.out.println("Finish declaring resources...");
	}

	private void createVirtualHost(String name) {
		try {
			System.out.println("Declare virtual host: " + name);
			String responseString = sendDataToServer("vhosts/" + name, "PUT", null);
			System.out.println(responseString);
		} catch (Exception e) {
			System.err.println("Error during define the virtual host: " + e.getMessage());
		}
	}

	private void createExchange(String vh, String name, String body) {
		try {
			System.out.println("Declare exchange: " + name);
			String responseString = sendDataToServer("exchanges/" + vh + "/" + name, "PUT", body);
			System.out.println(responseString);
		} catch (Exception e) {
			System.err.println("Error during define the exchange: " + e.getMessage());
		}
	}

	private void createQueue(String vh, String name, String body) {
		try {
			System.out.println("Declare queue: " + name);
			String responseString = sendDataToServer("queues/" + vh + "/" + name, "PUT", body);
			System.out.println(responseString);
		} catch (Exception e) {
			System.err.println("Error during define the queue: " + e.getMessage());
		}
	}

	private void bindExchangeToQueue(String vh, String exchangeName, String queueName, String routingKey) {
		try {
			System.out.println("Binding queue: " + queueName + " to exchange " + exchangeName);
			String responseString = null;
			if(routingKey != null) {
				responseString = sendDataToServer("bindings/" + vh + "/e/" + exchangeName + "/q/" + queueName, "POST", "{\"routing_key\":\"" + routingKey + "\"}");
			}else {
				responseString = sendDataToServer("bindings/" + vh + "/e/" + exchangeName + "/q/" + queueName, "POST", null);
			}
			System.out.println(responseString);
		} catch (Exception e) {
			System.err.println("Error during define the binding: " + e.getMessage());
		}
	}

	private void createPolicy(String vh, String name, String body) {
		try {
			System.out.println("Define policy: " + name);
			String responseString = sendDataToServer("policies/" + vh + "/" + name, "PUT", body);
			System.out.println(responseString);
		} catch (Exception e) {
			System.err.println("Error during define the policy: " + e.getMessage());
		}
	}
	

	private void createUser(String name) {
		try {
			System.out.println("Declare user: " + name);
			String responseString = sendDataToServer("users/" + name, "PUT", "{\"password\":\"" + name + "\",\"tags\":\"\"}");
			System.out.println(responseString);
		} catch (Exception e) {
			System.err.println("Error during define the user: " + e.getMessage());
		}
	}
	
	private void AssignBasicPermissionToUser(String vHost, String user, Set<String> appsToAccess) {
		try {
			System.out.println("Declare basic individual permission on vHost: " + vHost + " to user: " + user);
			
			String oldPermission = sendDataToServer("permissions/" + vHost + "/" + user, "GET", null);	
			if(!oldPermission.equals("Not Found")) {
				String writePermission = (oldPermission.split(",")[3]).split(":")[1];
				if(!writePermission.equals("\"\"")) {
					String[] permissionDetails = writePermission.split("\\.");
					
					if(permissionDetails.length != 3) {
						System.err.println("Old permission format not valid.");
						return;
					}
					writePermission = permissionDetails[2];
					writePermission = writePermission.substring(1, writePermission.length()-2);
					
					appsToAccess.addAll(Arrays.asList(writePermission.split("\\|")));
				}
			}
		
			String permission = "";
			if(appsToAccess.size() > 0) {
				for(String appToAccess : appsToAccess) {
					permission += appToAccess + "|";
				}
				
				permission = "{\"configure\":\"\",\"write\":\"" + companyName + "\\.ex\\.("+ permission.substring(0, permission.length() - 1) +")\",\"read\":\""+companyName+"\\.qu\\."+ user +"\\..+\"}";
			} else {
				permission = "{\"configure\":\"\",\"write\":\"\",\"read\":\""+companyName+"\\.qu\\."+ user +"\\\\..*\"}";
			}
			
			String responseString = sendDataToServer("permissions/" + vHost + "/" + user, "PUT", permission);
			System.out.println(responseString);
		} catch (Exception e) {
			System.err.println("Error during define the basic permission: " + e.getMessage());
		}
	}
	

	private void AssignTopicPermissionToUser(String vHost, String user, Set<String> exchangesToAccess) {
		for(String exchange : exchangesToAccess) {
			try {
				String permission = "{\"exchange\":\""+ exchange +"\",\"write\":\""+ user +"\\..+\",\"read\":\"\"}";
				System.out.println("Declare topic permission on vHost: " + vHost + " to user: " + user + " to access: " + exchange + " with permission: " + permission);
				
				String responseString = sendDataToServer("topic-permissions/" + vHost + "/" + user, "PUT", permission);
				System.out.println(responseString);
			} catch (Exception e) {
				System.err.println("Error during define the topic permission: " + e.getMessage());
			}
		}
	}
	

	private String sendDataToServer(String path, String requestMethod, String body) throws IOException {
		HttpURLConnection urlConnection = null;
		BufferedReader br = null;
		try {
			urlConnection = startHttpURLConnection(new URL(baseUrl + path));
			urlConnection.setRequestMethod(requestMethod);
			urlConnection.setRequestProperty("Content-Type", "application/json");

			// add body
			if (body != null) {
				OutputStreamWriter writer = new OutputStreamWriter(urlConnection.getOutputStream(), "UTF-8");
				writer.write(body);
				writer.close();
			}
			
			if (200 <= urlConnection.getResponseCode() && urlConnection.getResponseCode() <= 299) {
			    br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
			    String responseBody = br.readLine();
			    return (responseBody == null)? urlConnection.getResponseMessage() : responseBody;
			} 
			
			return urlConnection.getResponseMessage();
		} finally {
			if(br != null) {
				br.close();
			}
			
			if(urlConnection != null)
				urlConnection.disconnect();
		}
	}

	private HttpURLConnection startHttpURLConnection(URL url) throws IOException {
		HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
		urlConnection.setDoOutput(true);

		String authString = username + ":" + password;
		String authStringEnc = new String(Base64.getEncoder().encode(authString.getBytes()));
		urlConnection.setRequestProperty("Authorization", "Basic " + authStringEnc);
		return urlConnection;
	}
}