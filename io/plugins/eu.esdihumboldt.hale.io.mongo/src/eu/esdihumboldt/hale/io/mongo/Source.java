package eu.esdihumboldt.hale.io.mongo;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import com.mongodb.ServerAddress;

import eu.esdihumboldt.hale.common.core.io.supplier.LocatableInputSupplier;

public class Source implements LocatableInputSupplier<InputStream> {

	private final String host;
	private final String port;
	private final String database;
	
	private final URI uri;

	public Source(String host, String port, String database) {
		this.host = withDefault(host, ServerAddress.defaultHost());
		this.port = withDefault(port, ServerAddress.defaultPort());
		this.database = database;
		try {
			uri = new URI(String.format("mongodb://%s:%s/%s", this.host, this.port, database));
		} catch (URISyntaxException exception) {
			throw new RuntimeException(String.format(
					"Error building URI for: [host=%s, port=%s, database=%s]", 
					host, port, database));
		}
	}

	@Override
	public URI getLocation() {
		return uri;
	}

	@Override
	public URI getUsedLocation() {
		return uri;
	}

	@Override
	public InputStream getInput() throws IOException {
		return null;
	}
	
	private String withDefault(String value, Object defaultValue) {
		Object finalValue = value == null || value.isEmpty() ? defaultValue : value;
		return String.valueOf(finalValue);
	}
}