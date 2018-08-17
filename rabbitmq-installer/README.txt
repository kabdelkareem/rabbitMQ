Purpose
-------
Install GRP Integration Services


Explanation
-----------
Install integration services between different GRP projects like
AMQP queues.


Prerequisites for Running the Project
-------------------------------------
1. You must have the following installed on your machine:
   - JDK 1.8 or higher
   - Maven 3.5.2 or higher

  For more information, see the README in the top-level examples
  directory.


Usage
------
1. Open Terminal or Command Prompt
2. Use the following command to install the required services
		java -jar /path/to/this/project.jar OperationType <parameters>		
	where OperationType include:
		AMQP
	
	AMQP parameters (space separated):
		host									the IP address of the RabbitMQ server
		port									the port to access the RabbitMQ server
		username								the username with necessary privilege 
		password								the password for the username
		virtualHost								the virtual host that will be used to install the integrations (if not exist, will be created)
		includeUsersAndPermissionsDefinition	if sets to 'true', a default users ,with same module name, and permission defined based on  specified files in 'integrationsToInstall'. 
		integrationsToInstall					integrations to be installed. it could be multiple file with space separated
												Available integrations:
													finance-budget
													warehouse-navy
													warehouse-setup
													navy-setup
													
	Examples:
		java -jar /path/to/this/project.jar AMQP localhost 15672 user pass vhost true finance-budget
			will install finance-budget integrations including users (finance/finance & budget/budget) and permissions.
			
		java -jar /path/to/this/project.jar AMQP localhost 15672 user pass vhost true warehouse-setup warehouse-navy
			will install warehouse-setup warehouse-navy integrations including users (warehouse/warehouse, navy/navy, and setup/setup) and their permissions.
			
		java -jar /path/to/this/project.jar AMQP localhost 15672 user pass vhost false finance-budget
			will install finance-budget integrations only.
			
			
		 
