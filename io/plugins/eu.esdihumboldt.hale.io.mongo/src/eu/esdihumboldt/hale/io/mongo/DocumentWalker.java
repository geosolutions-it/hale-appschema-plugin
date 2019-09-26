package eu.esdihumboldt.hale.io.mongo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.bson.Document;

import com.mongodb.client.model.geojson.Geometry;

import eu.esdihumboldt.hale.common.schema.model.TypeDefinition;
import eu.esdihumboldt.hale.common.schema.model.constraint.type.Binding;
import eu.esdihumboldt.hale.common.schema.model.constraint.type.HasValueFlag;
import eu.esdihumboldt.hale.common.schema.model.constraint.type.MappableFlag;
import eu.esdihumboldt.hale.common.schema.model.constraint.type.MappingRelevantFlag;
import eu.esdihumboldt.hale.common.schema.model.impl.DefaultPropertyDefinition;
import eu.esdihumboldt.hale.common.schema.model.impl.DefaultSchema;
import eu.esdihumboldt.hale.common.schema.model.impl.DefaultTypeDefinition;

public final class DocumentWalker {

	private final DefaultSchema schema;
	private final String rootTypeName;
	private final Document rootDocument;

	public DocumentWalker(DefaultSchema schema, String rootTypeName, Document rootDocument) {
		this.schema = schema;
		this.rootTypeName = rootTypeName;
		this.rootDocument = rootDocument;
	}

	private Map<String, Integer> collectionsTypes = new HashMap<>();

	public void walk() {
		JsonPathConstraint jsonPathConstraint = new JsonPathConstraint(rootTypeName, "", "",
				JsonPathConstraint.Type.COMPLEX);
		DefaultTypeDefinition collectionType = createTypeDefinition(new QName(rootTypeName), jsonPathConstraint);
		walkDocument(collectionType, rootDocument, "");
	}

	private void walkDocument(TypeDefinition type, Document document, String jsonPath) {
		document.entrySet().stream().forEach(entry -> handleProperty(type, jsonPath, entry.getKey(), entry.getValue()));
	}

	private void walkList(TypeDefinition type, String collectionName, List<?> values, String jsonPath) {
		values.stream().forEach(value -> handleProperty(type, jsonPath, collectionName, value));
	}

	private String getCollectionTypeName(String name) {
		Integer count = collectionsTypes.get(name);
		if (count == null) {
			count = 1;
		}
		collectionsTypes.put(name, count + 1);
		return name + count;
	}

	private void handleProperty(TypeDefinition type, String jsonPath, String key, Object value) {
		// we cannot infer the type of null values
		if (value == null) {
			return;
		}
		// create type name
		QName name = new QName(key);
		// set the current JSON path
		String currentJsonPath = concatPath(jsonPath, key);
		// explicitly handle documents, collection and geometries
		if (value instanceof Document && isGeometry((Document) value)) {
			// set geometry type
			JsonPathConstraint jsonPathConstraint = new JsonPathConstraint(rootTypeName, key, currentJsonPath,
					JsonPathConstraint.Type.SIMPLE);
			createTypeProperty(type, name, Geometry.class, jsonPathConstraint);
		} else if (value instanceof Document) {
			// handle a document
			JsonPathConstraint jsonPathConstraint = new JsonPathConstraint(rootTypeName, key, currentJsonPath,
					JsonPathConstraint.Type.COMPLEX);
			DefaultPropertyDefinition property = createTypeProperty(type, name, value.getClass(), jsonPathConstraint);
			walkDocument(property.getPropertyType(), (Document) value, currentJsonPath);
		} else if (value instanceof List) {
			// handle a collection of values
			JsonPathConstraint jsonPathConstraint = new JsonPathConstraint(rootTypeName, key, currentJsonPath,
					JsonPathConstraint.Type.COLLECTION);
			String collectionTypeName = getCollectionTypeName(name.getLocalPart());
			DefaultPropertyDefinition property = createTypeProperty(type, name, value.getClass(), jsonPathConstraint);
			walkList(property.getPropertyType(), key, (List<?>) value, "");
		} else {
			// add a simple property
			JsonPathConstraint jsonPathConstraint = new JsonPathConstraint(rootTypeName, key, currentJsonPath,
					JsonPathConstraint.Type.SIMPLE);
			createTypeProperty(type, name, value.getClass(), jsonPathConstraint);
		}
	}

	private String concatPath(String jsonPath, String newPath) {
		if (jsonPath == null || jsonPath.isEmpty()) {
			return newPath;
		}
		return jsonPath + "." + newPath;
	}

	/**
	 * Helper method that creates a property definition and is associated type.
	 */
	private DefaultPropertyDefinition createTypeProperty(TypeDefinition parentType, QName typeName,
			Class<?> valueBinding, JsonPathConstraint jsonPath) {
		DefaultTypeDefinition propertyType = createTypeDefinition(typeName, jsonPath);
		DefaultPropertyDefinition property = (DefaultPropertyDefinition) parentType.getChild(typeName);
		if (property != null) {
			return property;
		}
		propertyType.setConstraint(Binding.get(valueBinding));
		propertyType.setConstraint(jsonPath);
		property = new DefaultPropertyDefinition(typeName, parentType, propertyType);
		property.setConstraint(jsonPath);
		return property;
	}

	/**
	 * Helper method that creates a type using the provided name and register it
	 * in the provided schema. Note that if a type with the same name already
	 * exists it will be returned.
	 */
	private DefaultTypeDefinition createTypeDefinition(QName typeName, JsonPathConstraint jsonPath) {
		// check if the type already exists
		DefaultTypeDefinition type = (DefaultTypeDefinition) schema.getType(typeName);
		if (type != null) {
			// the type already exists
			return type;
		}
		// this a new type we need to create it
		type = new DefaultTypeDefinition(typeName);
		type.setConstraint(MappableFlag.ENABLED);
		type.setConstraint(MappingRelevantFlag.ENABLED);
		type.setConstraint(HasValueFlag.ENABLED);
		type.setConstraint(jsonPath);
		// add the type to the schema
		schema.addType(type);
		return type;
	}

	/**
	 * Helper method that checks if a document is a geometry.
	 */
	private final boolean isGeometry(Document document) {
		// get geometry defining attributes
		Object typeValue = document.get("type");
		Object coordinatesValue = document.get("coordinates");
		if (typeValue == null || coordinatesValue == null) {
			// not a geometry
			return false;
		}
		// is a geometry
		return true;
	}
}
