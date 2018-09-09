package com.karim.examples.rabbitmq.connector;

import java.io.IOException;

import com.karim.examples.rabbitmq.connector.configures.ConsumerConfigurer;
import com.karim.examples.rabbitmq.connector.util.Log4j;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.ShutdownSignalException;

public class RabbitConsumer  implements Consumer {

	/** Channel that this consumer is associated with. */
    private volatile Channel _channel;
    /** Consumer tag for this consumer. */
    private volatile String _consumerTag;
    
    private final AMQPService _callerService;
    private final ConsumerConfigurer _consumerConfigure;

    /**
     * Constructs a new instance and records its association to the passed-in channel.
     * @param channel the channel to which this consumer is attached
     */
    public RabbitConsumer(Channel channel) {
        _channel = channel;
        _callerService = null;
        _consumerConfigure = null;
    }
    
    /**
     * 
    /**
     * 
     * Constructs a new instance and records its association to the passed-in channel.
     * @param channel the channel to which this consumer is attached
     * @param callerService called service
     * @param consumerConfigure
     */
    public RabbitConsumer(Channel channel,
    		AMQPService callerService, 
    		ConsumerConfigurer consumerConfigure) {
        _channel = channel;
        _callerService = callerService;
        _consumerConfigure = consumerConfigure;
    }

    /**
     * Stores the most recently passed-in consumerTag - semantically, there should be only one.
     * @see Consumer#handleConsumeOk
     */
    @Override
    public void handleConsumeOk(String consumerTag) {
        this._consumerTag = consumerTag;
    }

    /**
     * No-op implementation of {@link Consumer#handleCancelOk}.
     * @param consumerTag the defined consumer tag (client- or server-generated)
     */
    @Override
    public void handleCancelOk(String consumerTag) {
        // no work to do
    	Log4j.traceInfo(RabbitConsumer.class,  "handleCancelOk");
    }

    /**
     * No-op implementation of {@link Consumer#handleCancel(String)}
     * @param consumerTag the defined consumer tag (client- or server-generated)
     */
    @Override
    public void handleCancel(String consumerTag) throws IOException {
    	// Called when the queue was deleted from the server
    	Log4j.traceError(RabbitConsumer.class, 
    			"A RabbitMQ consumer UNEXPECTABLY stops listening to new messages...");
    	try {
	    	if(_callerService != null) { // recoverable consumer
	        	// try reconnect again to the queue on current channel
				_callerService.basicConsumeWithRetryRecovery(_channel, _consumerConfigure, this);
	    	}
    	} catch (Exception ex) {
			// Do no thing
		}

    }

    /**
     * No-op implementation of {@link Consumer#handleShutdownSignal}.
     */
    @Override
    public void handleShutdownSignal(String consumerTag, ShutdownSignalException sig) {
        // no work to do
    	if( sig.isInitiatedByApplication()) {
    		Log4j.traceErrorException(RabbitConsumer.class, sig.getCause(), 
    				"The connection to the messaging server was shut down with consumerTag: " +  consumerTag );
 
        } else if( sig.getReference() instanceof Channel ) {
            int nb = ((Channel) sig.getReference()).getChannelNumber();
            Log4j.traceErrorException(RabbitConsumer.class, sig.getCause(), 
            		"A RabbitMQ consumer was shut down. Channel #" + nb + ", consumerTag: " + consumerTag );
 
        } else {
        	Log4j.traceErrorException(RabbitConsumer.class, sig.getCause(), 
        			"A RabbitMQ consumer was shut down with consumerTag: " + consumerTag );
        }
    }

     /**
     * No-op implementation of {@link Consumer#handleRecoverOk}.
     */
    @Override
    public void handleRecoverOk(String consumerTag) {
        // no work to do
    	Log4j.traceInfo(RabbitConsumer.class, 
    			"handleRecoverOk called for consumerTag: " + consumerTag);
    }

    /**
     * No-op implementation of {@link Consumer#handleDelivery}.
     */
    @Override
    public void handleDelivery(String consumerTag,
                               Envelope envelope,
                               AMQP.BasicProperties properties,
                               byte[] body)
        throws IOException
    {
            // no work to do
    }

    /**
    *  Retrieve the channel.
     * @return the channel this consumer is attached to.
     */
    public Channel getChannel() {
        return _channel;
    }
  
    /**
     * Set the channel 
     * @param channel
     */
    public void setChannel(Channel channel) {
        this._channel = channel;
    }

    /**
    *  Retrieve the consumer tag.
     * @return the most recently notified consumer tag.
     */
    public String getConsumerTag() {
        return _consumerTag;
    }
    
    public ConsumerConfigurer getConsumerConfigurer() {
    	return this._consumerConfigure;
    }

}
