package eu.esdihumboldt.hale.io.mongo;

import eu.esdihumboldt.hale.common.schema.model.Constraint;
import eu.esdihumboldt.hale.common.schema.model.PropertyConstraint;
import eu.esdihumboldt.hale.common.schema.model.TypeConstraint;

/**
 * Constraint used to hold the JSON path of this object.
 */
@Constraint(mutable = false)
public final class JsonPathConstraint implements TypeConstraint, PropertyConstraint {

	// type of the of the object
	public enum Type {
		SIMPLE, COMPLEX, COLLECTION;
	}

	private final String rootKey;
	private final String key;
	private final String jsonPath;
	private final Type type;
	private final boolean valid;

	public JsonPathConstraint() {
		this.rootKey = null;
		this.key = null;
		this.jsonPath = null;
		this.type = null;
		this.valid = false;
	}
	
	public JsonPathConstraint(String rootKey, String key, String jsonPath, Type type) {
		this.rootKey = rootKey;
		this.key = key;
		this.jsonPath = jsonPath;
		this.type = type;
		this.valid = true;
	}

	public String getJsonPath() {
		return jsonPath;
	}

	public boolean isValid() {
		return valid;
	}
	
	public String getKey() {
		return key;
	}
	
	public String getRootKey() {
		return rootKey;
	}

	@Override
	public boolean isInheritable() {
		return false;
	}
}