RabbitMQ Services Installer

Purpose
-------
Configure RabbitMQ Integration Services


Explanation
-----------
Configure rabbitmq integration services to be used in communication between
different applications. installer used to configure exchanges, queues, binds,
policies, and optionally could configure default user/password for each
application and permissions for each of them.


Prerequisites for Running the Project
-------------------------------------
1. You must have the following installed on your machine:
   - JDK 1.6 or higher
   - Maven


Usage
------
1. Open Terminal or Command Prompt
2. Use the following command to install the required services
		java -jar /path/to/this/project.jar <parameters>		
	
	AMQP parameters (space separated):
		host									the IP address of the RabbitMQ server
		port									the port to access the RabbitMQ server
		username								the username with necessary privilege 
		password								the password for the username
		virtualHost								the virtual host that will be used to install the integrations (if not exist, will be created)
		includeUsersAndPermissionsDefinition	if sets to 'true', a default users ,with same module name, and permission defined based on  specified files in 'integrationsToInstall'. 
		integrationsToInstall					integrations to be installed. it could be multiple file with space separated
												Available integrations:
													app1-app2
													
	Examples:
			
		java -jar /path/to/this/project.jar AMQP localhost 15672 user pass vhost true app1-app2
			will install app1-app2 integrations including users (app1/app1 & app2/app2) and permissions.
			
		java -jar /path/to/this/project.jar AMQP localhost 15672 user pass vhost false app1-app2
			will install app1-app2 integrations only without users and permissions.
			
			
		 
