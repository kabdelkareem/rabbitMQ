# RabbitMQ Integration 

Purpose
-------
Contains the rabbitMQ integration services and connectors.


Explanation
-----------
This project found to help others that use RabbitMQ services and stuck in the
same problem as me that there are no general re-delivery policy in rabbitMQ server.

Re-delivery policy means that failed messages during delivery have to be delayed
before next try. Also there must be a limit for re-delivers and when message exceeds
the limit, it should be considered as a dead-letter message.

Dead-letter messages should be logged or notified to origin system administrator to start
communication with consumer system administrator to solve the problem.

This diagram explained what this project will do
![ScreenShot](project%20description%20diagram.jpg)

There 'll be 5 independent modules in this project:
---------------------------------------------------
- rabbitmq-installer: project to install project exchanges, queues, policies and users
- rabbitmq-connector: project used as upper layer of amqp-client provided by rabbitMQ to make publishing & consumming easier.
- app1: sample publisher application using rabbitmq-connector
- app2: sample consummer application using rabbitmq-connector
- esb: Enterprise Service Bus layer responsible for requeue, deadletter, and log messages.


Prerequisites for Running the Project
-------------------------------------
1. You must have the following installed on your machine:
   - JDK 1.8 or higher
   - Maven
