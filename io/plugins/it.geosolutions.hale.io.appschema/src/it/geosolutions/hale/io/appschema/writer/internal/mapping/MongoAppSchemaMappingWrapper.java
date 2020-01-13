/*
 * Copyright (c) 2019 wetransform GmbH
 * 
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Contributors:
 *     wetransform GmbH <http://www.wetransform.to>
 */

package it.geosolutions.hale.io.appschema.writer.internal.mapping;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import eu.esdihumboldt.hale.common.align.model.ChildContext;
import eu.esdihumboldt.hale.common.schema.model.TypeDefinition;
import it.geosolutions.hale.io.appschema.impl.internal.generated.app_schema.AppSchemaDataAccessType;
import it.geosolutions.hale.io.appschema.impl.internal.generated.app_schema.TypeMappingsPropertyType.FeatureTypeMapping;
import it.geosolutions.hale.io.appschema.impl.internal.generated.app_schema.TypeMappingsPropertyType.FeatureTypeMapping.AttributeMappings;
import it.geosolutions.hale.io.appschema.writer.AppSchemaMappingUtils;

/**
 * App-schema mapping configuration wrapper for MongoDB use case.
 * 
 * <p>
 * Holds the state associated to the same mapping configuration and provides
 * utility methods to mutate it.
 * </p>
 */
public class MongoAppSchemaMappingWrapper extends AppSchemaMappingWrapperBase {

	/**
	 * Constructor.
	 * 
	 * @param appSchemaMapping the app-schema mapping to wrap
	 */
	public MongoAppSchemaMappingWrapper(AppSchemaDataAccessType appSchemaMapping) {
		super(appSchemaMapping);
	}

	/**
	 * @see it.geosolutions.hale.io.appschema.writer.internal.mapping.AppSchemaMappingWrapperBase#getOrCreateFeatureTypeMapping(eu.esdihumboldt.hale.common.schema.model.TypeDefinition,
	 *      java.lang.String, boolean)
	 */
	@Override
	public FeatureTypeMapping getOrCreateFeatureTypeMapping(TypeDefinition targetType,
			String mappingName, boolean secondary) {
		final Optional<String> prefix = getMappingPrefix();
		if (prefix.isPresent()) {
			mappingName = prefix.get() + "-" + targetType.getDisplayName();
		}
		Integer hashKey = getFeatureTypeMappingHashKey(targetType, mappingName);
		if (!featureTypeMappings.containsKey(hashKey)) {
			// create
			FeatureTypeMapping featureTypeMapping = new FeatureTypeMapping();
			// initialize attribute mappings member
			featureTypeMapping.setAttributeMappings(new AttributeMappings());
			// TODO: how do I know the datasource from which data will be read?
			featureTypeMapping.setSourceDataStore(getDefaultDataStore().getId());
			featureTypeMapping.setTargetElement(getTargetElementName(targetType));
			if (mappingName != null && !mappingName.isEmpty()) {
				featureTypeMapping.setMappingName(mappingName);
			}

			appSchemaMapping.getTypeMappings().getFeatureTypeMapping().add(featureTypeMapping);
			featureTypeMappings.put(hashKey, featureTypeMapping);
			addToFeatureTypeMappings(targetType, featureTypeMapping, secondary);
		}
		return featureTypeMappings.get(hashKey);
	}

	private void addToFeatureTypeMappings(TypeDefinition targetType, FeatureTypeMapping typeMapping,
			boolean secondary) {
		Map<String, Set<FeatureTypeMapping>> mappingsByTargetElement = null;
		if (AppSchemaMappingUtils.isFeatureType(targetType) && !secondary) {
			mappingsByTargetElement = featureTypesByTargetElement;
		}
		else {
			mappingsByTargetElement = nonFeatureTypesByTargetElement;
		}

		if (!mappingsByTargetElement.containsKey(typeMapping.getTargetElement())) {
			mappingsByTargetElement.put(typeMapping.getTargetElement(),
					new HashSet<FeatureTypeMapping>());
		}
		mappingsByTargetElement.get(typeMapping.getTargetElement()).add(typeMapping);
	}

	/**
	 * @see it.geosolutions.hale.io.appschema.writer.internal.mapping.AppSchemaMappingWrapperBase#validPropertyPath(java.util.List)
	 */
	@Override
	protected boolean validPropertyPath(List<ChildContext> propertyPath) {
		return true;
	}

	@Override
	protected Integer getAttruteMappingHashKey(TypeDefinition owningType,
			List<ChildContext> propertyPath) {
		final String SEPARATOR = "__";
		StringBuilder pathBuilder = new StringBuilder();

		if (owningType != null) {
			pathBuilder.append(owningType.getName().toString()).append(SEPARATOR);
			if (propertyPath == null || propertyPath.isEmpty()) {
				pathBuilder.append(UUID.randomUUID().toString());
			}
			else {
				for (ChildContext childContext : propertyPath) {
					pathBuilder.append(childContext.getChild().getName().toString());
					if (childContext.getContextName() != null) {
						pathBuilder.append(childContext.getContextName());
					}
					pathBuilder.append(SEPARATOR);
				}
			}
		}
		else {
			throw new IllegalArgumentException("Could not find feature type owning property");
		}

		return pathBuilder.toString().hashCode();
	}
}
