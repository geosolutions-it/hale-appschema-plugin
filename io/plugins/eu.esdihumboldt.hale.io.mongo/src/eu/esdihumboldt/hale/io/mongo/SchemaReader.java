package eu.esdihumboldt.hale.io.mongo;

import java.io.IOException;
import java.sql.Connection;
import java.sql.Types;
import java.util.List;

import javax.xml.namespace.QName;

import org.bson.Document;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

import eu.esdihumboldt.hale.common.core.io.IOProviderConfigurationException;
import eu.esdihumboldt.hale.common.core.io.ProgressIndicator;
import eu.esdihumboldt.hale.common.core.io.report.IOReport;
import eu.esdihumboldt.hale.common.core.io.report.IOReporter;
import eu.esdihumboldt.hale.common.core.io.report.impl.IOMessageImpl;
import eu.esdihumboldt.hale.common.schema.geometry.GeometryProperty;
import eu.esdihumboldt.hale.common.schema.io.impl.AbstractSchemaReader;
import eu.esdihumboldt.hale.common.schema.model.Schema;
import eu.esdihumboldt.hale.common.schema.model.TypeDefinition;
import eu.esdihumboldt.hale.common.schema.model.constraint.type.Binding;
import eu.esdihumboldt.hale.common.schema.model.constraint.type.GeometryType;
import eu.esdihumboldt.hale.common.schema.model.constraint.type.HasValueFlag;
import eu.esdihumboldt.hale.common.schema.model.constraint.type.MappableFlag;
import eu.esdihumboldt.hale.common.schema.model.constraint.type.MappingRelevantFlag;
import eu.esdihumboldt.hale.common.schema.model.impl.DefaultPropertyDefinition;
import eu.esdihumboldt.hale.common.schema.model.impl.DefaultSchema;
import eu.esdihumboldt.hale.common.schema.model.impl.DefaultTypeDefinition;
import eu.esdihumboldt.hale.common.schema.persist.AbstractCachedSchemaReader;

public final class SchemaReader extends AbstractSchemaReader {

	private Schema schema;

	@Override
	protected String getDefaultTypeName() {
		return "MongoDB";
	}

	private Schema buildSchema(Client client, IOReporter reporter) {
		// read collection elements
		String collectionName = getParameter(Constants.COLLECTION_NAME).getStringRepresentation();
		String maxElements = getParameter(Constants.MAX_ELEMENTS).getStringRepresentation();
		List<Document> documents = client.getElements(collectionName, Integer.valueOf(maxElements));
		// handle any specific element if any
		String specificElements = getParameter(Constants.SPECIFIC_ELEMENTS).getStringRepresentation();
		if (specificElements != null && !specificElements.isEmpty()) {
			String[] ids = specificElements.split(";");
			for (String id : ids) {
				Document document = client.getElement(collectionName, id);
				if (document != null) {
					documents.add(document);
				}
			}
		}
		// create a schema and fill it with the types
		DefaultSchema schema = new DefaultSchema("namespace", Constants.MOCK_URI);
		walkDocuments(schema, collectionName, documents);
		// report success and return the schema
		reporter.setSuccess(true);
		return schema;
	}

	private void walkDocuments(DefaultSchema schema, String collectionName, List<Document> documents) {
		documents.forEach(document -> {
			DocumentWalker walker = new DocumentWalker(schema, collectionName, document);
			walker.walk();
		});
	}

	@Override
	public Schema getSchema() {
		return schema;
	}

	@Override
	public boolean isCancelable() {
		return false;
	}

	@Override
	protected IOReport execute(ProgressIndicator progress, IOReporter reporter)
			throws IOProviderConfigurationException, IOException {
		// create MongoDB client
		try (Client client = new ClientBuilder().withProvider(this).build()) {
			// build the schema
			schema = buildSchema(client, reporter);
			reporter.info(new IOMessageImpl("Schema read from MongoDB.", null));
			reporter.setSuccess(true);
		} catch (Exception exception) {
			reporter.error(new IOMessageImpl("Error reading schema from MongoDB.", exception));
			reporter.setSuccess(false);
		}
		return reporter;
	}
}
