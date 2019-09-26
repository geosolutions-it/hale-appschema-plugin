package eu.esdihumboldt.hale.io.mongo;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;

import eu.esdihumboldt.hale.common.core.io.ImportProvider;
import eu.esdihumboldt.hale.common.core.io.Value;

public final class ClientBuilder {

	private final List<ServerAddress> serverAddresses = new ArrayList<>();
	private final List<MongoCredential> credentials = new ArrayList<>();
	private final MongoClientOptions.Builder mongoOptions = new MongoClientOptions.Builder();

	private String databaseName;

	public ClientBuilder withProvider(ImportProvider provider) {
		// configure the host and port, using default values where needed
		withAddress(provider.getSource().getLocation());
		// set the database name to use
		databaseName = provider.getSource().getLocation().getPath();
		if (databaseName != null || databaseName.length() > 1) {
			databaseName = databaseName.substring(1);
		}
		// configure authentication if provided
		Value userValue = provider.getParameter(Constants.USER);
		if (userValue != null) {
			// no authentication information provided
			return this;
		}
		Value passwordValue = provider.getParameter(Constants.PASSWORD);
		Value databaseValue = provider.getParameter(Constants.AUTHENTICATION_DATABASE);
		return withCredencial(value(userValue), value(passwordValue), value(databaseValue));
	}

	public ClientBuilder withAddress(URI location) {
		String host = location.getHost();
		int port = Integer.valueOf(location.getPort());
		withAddress(host, port);
		return this;
	}

	public ClientBuilder withAddress(String host, int port) {
		serverAddresses.add(new ServerAddress(host, port));
		return this;
	}

	public ClientBuilder withCredencial(String user, String password, String database) {
		database = database == null || database.isEmpty() ? "admin" : database;
		credentials.add(MongoCredential.createCredential(user, database, password.toCharArray()));
		return this;
	}

	public Client build() {
		if (databaseName == null) {
			throw new RuntimeException("No database name was provided.");
		}
		if (serverAddresses.isEmpty()) {
			serverAddresses.add(new ServerAddress());
		}
		return new Client(serverAddresses, credentials, mongoOptions.build(), databaseName);
	}

	private String value(Value value) {
		return value == null ? null : value.getStringRepresentation();
	}
}