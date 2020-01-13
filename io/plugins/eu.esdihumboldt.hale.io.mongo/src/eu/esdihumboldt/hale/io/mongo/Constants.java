package eu.esdihumboldt.hale.io.mongo;

import java.net.URI;

public final class Constants {
	
	public static final String USER = "MONGO_USER";
	public static final String PASSWORD = "MONGO_USER";
	public static final String AUTHENTICATION_DATABASE = "MONGO_AUTHENTICATION_DATABASE";
	public static final String COLLECTION_NAME = "MONGO_COLLECTION_NAME";
	public static final String MAX_ELEMENTS = "MAX_ELEMENTS";
	public static final String SPECIFIC_ELEMENTS = "SPECIFIC_ELEMENTS";
	
	public static final URI MOCK_URI = buildMockUri();
	
	private Constants() {
	}
	
	private static URI buildMockUri() {
		try {
			return new URI("");
		} catch (Exception exception) {
			throw new RuntimeException("Error building mock URI.", exception);
		}
	}
}
