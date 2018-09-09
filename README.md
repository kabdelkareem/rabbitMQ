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


## Modules **_(UNDER CONSTRUCTION)_**
Module Name | Description
------------ | -----------
[rabbitmq-installer](rabbitmq-installer/README.md) | project to install project exchanges, queues, policies and users.
[rabbitmq-common](rabbitmq-common/README.md) | common enums to be used by [rabbitmq-connector](rabbitmq-connector) and [esb](esb) modules
[rabbitmq-connector](rabbitmq-connector/README.md) | project used as upper layer of amqp-client provided by rabbitMQ to make publishing & consumming easier.
[mock-app1](mock-app1/README.md) | sample publisher application using rabbitmq-connector.
[mock-app2](mock-app2/README.md) | sample consummer application using rabbitmq-connector.
[esb](esb/README.md) | Enterprise Service Bus project responsible for requeue, deadletter, and log messages.


Prerequisites for Running the Project
-------------------------------------
1. You must have the following installed on your machine:
   - JDK 1.8 or higher
   - Maven
