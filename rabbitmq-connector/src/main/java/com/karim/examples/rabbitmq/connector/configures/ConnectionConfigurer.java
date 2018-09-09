package com.karim.examples.rabbitmq.connector.configures;

import java.util.function.Consumer;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import com.karim.examples.rabbitmq.connector.extended.Address;
import com.rabbitmq.client.AMQP;

public final class ConnectionConfigurer {
	// Mandatory
	private final String _applicationName;
	private final String _virtualHost;
	private final String _username;
	private final String _password;
	
	//Optional
	private final String _host;
	private final Address[] _addresses;
	
	private final Integer _port;
	private final Boolean _useSSL;
	private final String _protocol;
	private final TrustManager _trustManager;
	private final SSLContext _sslContext;
	
	private final Boolean _automaticRecoveryEnabled;
	private final Integer _networkRecoveryInterval;
	private final Integer _requestedHeartbeatTimeout;
	private final Integer _connectionTimeout;
	
	// DEFAULTS
	private static final int DEFAULT_PORT = AMQP.PROTOCOL.PORT;
	private static final boolean DEFAULT_USE_SSL = false;
	private static final boolean DEFAULT_AUTOMATIC_RECOVERY_ENABLED = true;
	private static final int DEFAULT_AUTOMATIC_RECOVERY_INTERVAL = 2 * 60 * 1000; // 2 minutes
	private static final int DEFAULT_HEARTBEAT_TIMEOUT = 1 * 60 * 1000; // 1 minute
	//Connection close timeout (10 seconds)
    private static final int DEFAULT_CONNECTION_TIMEOUT = 10 * 1000;
	
	private ConnectionConfigurer(final Builder builder) {
		this._applicationName = builder._applicationName;
		this._host = builder._host;
		this._addresses = builder._addresses;
		this._virtualHost = builder._virtualHost;
		this._username = builder._username;
		this._password = builder._password;
		
		this._port = builder._port;
		this._useSSL = builder._useSSL;
		this._protocol = builder._protocol;
		this._trustManager = builder._trustManager;
		this._sslContext = builder._sslContext;
		

		this._automaticRecoveryEnabled = builder._automaticRecoveryEnabled;
		this._networkRecoveryInterval = builder._networkRecoveryInterval;
		this._requestedHeartbeatTimeout = builder._requestedHeartbeatTimeout;
		this._connectionTimeout = builder._connectionTimeout;
	}
	
	// Getters
	public String getApplicationName() {
		return this._applicationName;
	}

	public String getVirtualHost() {
		return this._virtualHost;
	}

	public String getUsername() {
		return this._username;
	}

	public String getPassword() {
		return this._password;
	}

	public String getHost() {
		return this._host;
	}

	public Address[] getAddresses() {
		return this._addresses;
	}

	public int getPort() {
		return this._port == null? DEFAULT_PORT : this._port;
	}

	public boolean isUseSSL() {
		return this._useSSL == null? DEFAULT_USE_SSL : this._useSSL;
	}

	public String getProtocol() {
		return this._protocol;
	}

	public TrustManager getTrustManager() {
		return this._trustManager;
	}

	public SSLContext getSslContext() {
		return this._sslContext;
	}

	public boolean isAutomaticRecoveryEnabled() {
		return this._automaticRecoveryEnabled == null? 
				DEFAULT_AUTOMATIC_RECOVERY_ENABLED 
				: this._automaticRecoveryEnabled;
	}

	public int getNetworkRecoveryInterval() {
		return this._networkRecoveryInterval == null? 
				DEFAULT_AUTOMATIC_RECOVERY_INTERVAL 
				: this._networkRecoveryInterval;
	}

	public int getRequestedHeartbeatTimeout() {
		return this._requestedHeartbeatTimeout == null? 
				DEFAULT_HEARTBEAT_TIMEOUT 
				: this._requestedHeartbeatTimeout;
	}
	
	public int getConnectionTimeout() {
		return this._connectionTimeout == null? 
				DEFAULT_CONNECTION_TIMEOUT 
				: this._connectionTimeout;
	}
	
	


	// Builder Class
	public static final class Builder {
		// Mandatory
		private String _applicationName;
		private String _virtualHost;
		private String _username;
		private String _password;
		
		//Optional
		private String _host;
		private Address[] _addresses;
		
		public Integer _port;
		public Boolean _useSSL;
		public String _protocol;
		public TrustManager _trustManager;
		public SSLContext _sslContext;
		
		public Boolean _automaticRecoveryEnabled;
		public Integer _networkRecoveryInterval;
		public Integer _requestedHeartbeatTimeout;
		public Integer _connectionTimeout;
		
		
		public Builder(final String applicationName,
				final String host,
				final String virtualHost,
				final String username, 
				final String password) {
			this._applicationName = applicationName;
			this._host = host;
			this._virtualHost = virtualHost;
			this._username = username;
			this._password = password;
		}
		
		public Builder(final String applicationName, 
				final Address[] addresses, 
				final String virtualHost, 
				final String username, 
				final String password) {
			this._applicationName = applicationName;
			this._addresses = addresses;
			this._virtualHost = virtualHost;
			this._username = username;
			this._password = password;
		}
		
		public Builder with(Consumer<Builder> builderFunction) {
	        builderFunction.accept(this);
	        return this;
	    }
		
		/**
		 * Set the connection port, default set to {@link  ConnectionConfigurer#DEFAULT_PORT}.
		 * 
		 * @param port the value to be specified
		 * @return current object (this).
		 * @see ConnectionConfigurer#_port
		 */
		public Builder withPort(final Integer port) {
			this._port = port;
			return this; 
		}

		/**
		 * Is it SSL or default TCP connection, default set to {@link ConnectionConfigurer#DEFAULT_USE_SSL}.
		 * 
		 * @param useSSL the value to be specified
		 * @return current object (this).
		 * @see ConnectionConfigurer#_useSSL
		 */
		public Builder withUseSSL(final Boolean useSSL) {
			this._useSSL = useSSL;
			return this; 
		}

		/**
		 * Define the protocol.
		 * 
		 * @param protocol the value to be specified
		 * @return current object (this).
		 * @see ConnectionConfigurer#_protocol
		 */
		public Builder withProtocol(final String protocol) {
			this._protocol = protocol;
			return this; 
		}

		/**
		 * Define the trustManager.
		 * 
		 * @param trustManager the value to be specified
		 * @return current object (this).
		 * @see ConnectionConfigurer#_trustManager
		 */
		public Builder withTrustManager(final TrustManager trustManager) {
			this._trustManager = trustManager;
			return this; 
		}

		/**
		 * Define the sslContext.
		 * 
		 * @param sslContext the value to be specified
		 * @return current object (this).
		 * @see ConnectionConfigurer#_sslContext
		 */
		public Builder withSslContext(final SSLContext sslContext) {
			this._sslContext = sslContext;
			return this; 
		}

		/**
		 * Is it automatic connection recovery enabled or not, default 
		 * set to {@link ConnectionConfigurer#DEFAULT_AUTOMATIC_RECOVERY_ENABLED}.
		 * 
		 * @param automaticRecoveryEnabled the value to be specified
		 * @return current object (this).
		 * @see ConnectionConfigurer#_automaticRecoveryEnabled
		 */
		public Builder withAutomaticRecoveryEnabled(final Boolean automaticRecoveryEnabled) {
			this._automaticRecoveryEnabled = automaticRecoveryEnabled;
			return this; 
		}

		/**
		 * Sets automatic connection recovery interval, default 
		 * set to {@link ConnectionConfigurer#DEFAULT_AUTOMATIC_RECOVERY_INTERVAL}.
		 * 
		 * @param networkRecoveryInterval the value to be specified
		 * @return current object (this).
		 * @see ConnectionConfigurer#_networkRecoveryInterval
		 */
		public Builder withNetworkRecoveryInterval(final Integer networkRecoveryInterval) {
			this._networkRecoveryInterval = networkRecoveryInterval;
			return this; 
		}

		/**
		 * Sets heart beat timeout, default 
		 * set to {@link ConnectionConfigurer#DEFAULT_HEARTBEAT_TIMEOUT}.
		 * 
		 * @param requestedHeartbeatTimeout the value to be specified
		 * @return current object (this).
		 * @see ConnectionConfigurer#_requestedHeartbeatTimeout
		 */
		public Builder withRequestedHeartbeatTimeout(final Integer requestedHeartbeatTimeout) {
			this._requestedHeartbeatTimeout = requestedHeartbeatTimeout;
			return this; 
		}

		/**
		 * Sets connection timeout, default 
		 * set to {@link ConnectionConfigurer#DEFAULT_CONNECTION_TIMEOUT}.
		 * 
		 * @param connectionTimeout the value to be specified
		 * @return current object (this).
		 * @see ConnectionConfigurer#_connectionTimeout
		 */
		public Builder withConnectionTimeout(final Integer connectionTimeout) {
			this._connectionTimeout = connectionTimeout;
			return this; 
		}
		
		/**
		 * Use defined properties in the builder to initialize a new ConnectionConfigurer Object.
		 * 
		 * @return {@link ConnectionConfigurer#ConnectionConfigurer(Builder)}
		 */
		public ConnectionConfigurer build() { 
			return new ConnectionConfigurer(this); 
		}
	}
}
