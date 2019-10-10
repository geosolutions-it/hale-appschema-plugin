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

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((jsonPath == null) ? 0 : jsonPath.hashCode());
		result = prime * result + ((key == null) ? 0 : key.hashCode());
		result = prime * result + ((rootKey == null) ? 0 : rootKey.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		result = prime * result + (valid ? 1231 : 1237);
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		JsonPathConstraint other = (JsonPathConstraint) obj;
		if (jsonPath == null) {
			if (other.jsonPath != null)
				return false;
		} else if (!jsonPath.equals(other.jsonPath))
			return false;
		if (key == null) {
			if (other.key != null)
				return false;
		} else if (!key.equals(other.key))
			return false;
		if (rootKey == null) {
			if (other.rootKey != null)
				return false;
		} else if (!rootKey.equals(other.rootKey))
			return false;
		if (type != other.type)
			return false;
		if (valid != other.valid)
			return false;
		return true;
	}
	
	
}