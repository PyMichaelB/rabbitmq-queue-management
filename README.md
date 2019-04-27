# RabbitMQ Queue Management
[![Build Status](https://travis-ci.org/gessnerfl/rabbitmq-queue-management.svg?branch=master)](https://travis-ci.org/gessnerfl/rabbitmq-queue-management)

*Application to list, delete and re-queue messages of queues.*

## Introduction

RabbitMQ does not provide tooling to re-queue or move messages out of the box. However this can by quite handy when working e.g. with dead letter queues/exchanges. This tool tries to close this gap. By offering the following features

* List queues of a RabbitMQ 
* List messages of queues
* Delete first message from queue
* Move first message from queue
* Web UI

As messages must not have a unique identifier in RabbitMQ this tools creates a checksum of the message and compares the checksum before applying move or delete operations on messages to avoid unintended changes.

To use the application the **RabbitMQ Management Plugin** has to be activated.

The application is based on Spring Boot. For more details please also consult the Spring Boot Documentation (http://docs.spring.io/spring-boot/docs/current/reference/htmlsingle).

## Operations
The following paragraphs describe the different implementations in more details.

### Delete

The delete operations applies the following steps:

- Receive first message of queue
- Ensure checksum matches
- Confirm message

In case of any error send nack with re-queuing or simply close the channel so that the message remains in the queue.

### Move operations

The following steps are applied to move a message from one queue to another exchange and routing key:

- Receive the first message of the queue
- Ensure checksum matches
- Activate Publisher Acknowledgement (https://www.rabbitmq.com/confirms.html)
- Register return listener to ensure that the message can be delivered to the target queue
- Publish message with its body and properties as mandatory to the given exchange name and routing key
- Wait for confirmation
- Ensure no basic.return was received by return listener

In case of any error send nack with re-queuing or simply close the channel so that the message remains in the queue.

# Configuration

As the application is based on Spring Boot the same rules applies to the configuration as described in the Spring Boot Documentation (http://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#boot-features-external-config).

The configuration file application.properties can be placed next to the application jar, in a sub-directory config or in any other location when specifying the location with the parameter `-Dspring.config.location=<path to config file>`.

The following paragraphs describe the application specific resp. pre-defined configuration parameters. If more configuration e.g. authentication is needed please check the Spring Boot Documentation for more detail.

## RabbitMQ Connection setup
The following snippet shows the configuration of a RabbitMQ connection with its default values.

    de.gessnerfl.rabbitmq.hostname=                   #The hostname of the rabbitmq host
    de.gessnerfl.rabbitmq.port=5672                   #AMQP Port number
    de.gessnerfl.rabbitmq.managementPort=15672        #RabbitMQ management port number
    de.gessnerfl.rabbitmq.managemnetPortSecured=false #Indicator if the management interface is accessible via http (false) or https (true)
    de.gessnerfl.rabbitmq.username=guest              #Username for connection to rabbitmq host
    de.gessnerfl.rabbitmq.password=guest              #Password for connection to rabbitmq host


## Web UI
The following snippet shows the pre-defined web application configuration

    server.port=8780     #Port of the web interface
    management.port=8781 #Port of the http management api

