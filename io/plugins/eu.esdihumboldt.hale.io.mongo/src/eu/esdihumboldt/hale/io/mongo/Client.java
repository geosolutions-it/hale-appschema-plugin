package eu.esdihumboldt.hale.io.mongo;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.bson.Document;
import org.bson.types.ObjectId;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;

public final class Client implements AutoCloseable {

	private final MongoClient mongoClient;

	private final String databaseName;

	Client(List<ServerAddress> serverAddresses, List<MongoCredential> credentials, MongoClientOptions options,
			String databaseName) {
		// instantiate MongoDB client
		mongoClient = new MongoClient(serverAddresses, credentials, options);
		// check that we can actually connect to MongoDB
		try {
			mongoClient.getAddress();
		} catch (Exception exception) {
			// not able to connect to the MongoDB
			throw new RuntimeException("Could not connect to MongoDB.", exception);
		}
		// set the database to use
		this.databaseName = databaseName;
	}

	public List<String> getCollectionNames() {
		MongoDatabase database = mongoClient.getDatabase(databaseName);
		return toList(database.listCollectionNames());
	}

	public List<Document> getElements(String collectionName, int limit) {
		MongoDatabase database = mongoClient.getDatabase(databaseName);
		return toList(database.getCollection(collectionName).find().limit(limit));
	}
	
	public Document getElement(String collectionName, String id) {
		MongoDatabase database = mongoClient.getDatabase(databaseName);
		BasicDBObject query = new BasicDBObject();
	    query.put("_id", new ObjectId(id));
	    return database.getCollection(collectionName).find(query).first();
	}

	@Override
	public void close() {
		// close MongoDB connection
		mongoClient.close();
	}

	private final <T> List<T> toList(MongoIterable<T> cursor) {
		List<T> list = new ArrayList<>();
		cursor.forEach((Consumer<T>) list::add);
		return list;
	}
}
