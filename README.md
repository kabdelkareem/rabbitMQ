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


[This diagram explained what this project will do ](project description diagram.jpg)


Prerequisites for Running the Project
-------------------------------------
1. You must have the following installed on your machine:
   - JDK 1.8 or higher
   - Maven 3.5.2 or higher

  For more information, see the README in the top-level examples
  directory.
