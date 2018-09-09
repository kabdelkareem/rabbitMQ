package com.karim.examples.rabbitmq.installer;

import java.util.ArrayList;
import java.util.List;


public class Main {

	public static void main(String[] args) {
		if(args.length == 0) {
			System.err.println("No argument specified");
			System.exit(0);
		}
		
		if(args.length < 7) {
			System.err.println("Input invalid.");
			System.err.println("Paramters: host port username password vHost includeAutoUsersAndPermissionsDefinition (integrationDefinition)+");
			System.exit(0);
		}
		
		String host = args[0];
		String port = args[1];
		
		String username = args[2];
		String password = args[3];
		
		String vHost = args[4];
		

		String includeUsersAndPermissionsDefinition = args[5];
		
		List<String> configFiles = new ArrayList<String>();
		for(int i = 6; i < args.length; i++) {
			configFiles.add(args[i]);
		}
		String baseUrl = "http://" + host + ":" + port + "/api/";
		
		AMQPConfiguration AMQPConfiguration = new AMQPConfiguration(baseUrl, username, password, vHost, includeUsersAndPermissionsDefinition, configFiles);
		AMQPConfiguration.install();	
	}
}
