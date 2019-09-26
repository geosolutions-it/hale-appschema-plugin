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

import java.util.List;
import java.util.Optional;
import java.util.Set;

import eu.esdihumboldt.hale.common.align.model.ChildContext;
import eu.esdihumboldt.hale.common.align.model.impl.PropertyEntityDefinition;
import eu.esdihumboldt.hale.common.schema.model.TypeDefinition;
import it.geosolutions.hale.io.appschema.impl.internal.generated.app_schema.AppSchemaDataAccessType;
import it.geosolutions.hale.io.appschema.impl.internal.generated.app_schema.AttributeMappingType;
import it.geosolutions.hale.io.appschema.impl.internal.generated.app_schema.NamespacesPropertyType.Namespace;
import it.geosolutions.hale.io.appschema.impl.internal.generated.app_schema.SourceDataStoresPropertyType.DataStore;
import it.geosolutions.hale.io.appschema.impl.internal.generated.app_schema.TypeMappingsPropertyType.FeatureTypeMapping;

/**
 * App-schema mapping configuration wrapper.
 * 
 * <p>
 * Holds the state associated to the same mapping configuration and provides
 * utility methods to mutate it.
 * </p>
 */
public interface MappingWrapper {

	/**
	 * Base name for special attributes used for feature chaining.
	 */
	String FEATURE_LINK_FIELD = "FEATURE_LINK";

	void setMappingPrefix(String mappingPrefix);

	/**
	 * Return the configuration of the default datastore.
	 * 
	 * <p>
	 * An empty datastore configuration is created if none is available.
	 * </p>
	 * 
	 * @return the default datastore's configuration.
	 */
	DataStore getDefaultDataStore();

	/**
	 * Return a namespace object with the provided URI and prefix.
	 * 
	 * <p>
	 * If a namespace object for the same URI already exists, it is returned.
	 * Otherwise, a new one is created.
	 * </p>
	 * <p>
	 * If the prefix is empty, a non-empty prefix is automatically generated.
	 * If, in a subsequent call to this method, a non-empty prefix is provided,
	 * the user-provided prefix will replace the generated one.
	 * </p>
	 * 
	 * @param namespaceURI the namespace URI
	 * @param prefix the namespace prefix
	 * @return the created namespace object
	 */
	Namespace getOrCreateNamespace(String namespaceURI, String prefix);

	/**
	 * Add a schema URI to the list of target types.
	 * 
	 * @param schemaURI the schema URI
	 */
	void addSchemaURI(String schemaURI);

	/**
	 * Updates a schema URI in the generated mapping configuration.
	 * 
	 * @param oldSchemaURI the current schema URI
	 * @param newSchemaURI the updated schema URI
	 */
	void updateSchemaURI(String oldSchemaURI, String newSchemaURI);

	/**
	 * @see AppSchemaMappingWrapper#buildAttributeXPath(TypeDefinition, List)
	 * 
	 * @param owningType the type owning the target property
	 * @param propertyEntityDef the target property definition
	 * @return the XPath expression pointing to the target property
	 */
	String buildAttributeXPath(TypeDefinition owningType,
			PropertyEntityDefinition propertyEntityDef);

	/**
	 * Build an XPath expression to be used as &lt;targetAttribute&gt; for the
	 * provided target property definition.
	 * 
	 * <p>
	 * The algorithm to build the path is as follows:
	 * <ol>
	 * <li>the property path is traversed backwards, from end to beginning</li>
	 * <li>on each step, a new path segment is added at the top of the list, but
	 * only if the child definition describes a property and not a group</li>
	 * <li>on each step, if a non-null context name is defined on the child
	 * context, <code>[&lt;context name&gt;]</code> string is appended to the
	 * path segment</li>
	 * <li>the traversal stops when the parent type of the last visited property
	 * equals to the provided owning type</li>
	 * </ol>
	 * 
	 * @param owningType the type owning the target property
	 * @param propertyPath the target property path
	 * @return the XPath expression pointing to the target property
	 */
	String buildAttributeXPath(TypeDefinition owningType, List<ChildContext> propertyPath);

	/**
	 * Return the feature type mapping associated to the provided type.
	 * 
	 * <p>
	 * If a feature type mapping for the provided type already exists, it is
	 * returned; otherwise, a new one is created.
	 * </p>
	 * 
	 * @param targetType the target type
	 * @return the feature type mapping
	 */
	FeatureTypeMapping getOrCreateFeatureTypeMapping(TypeDefinition targetType);

	FeatureTypeMapping getOrCreateFeatureTypeMapping(TypeDefinition targetType, boolean secondary);

	FeatureTypeMapping getOrCreateFeatureTypeMapping(TypeDefinition targetType, String mappingName);

	/**
	 * Return the feature type mapping associated to the provided type and
	 * mapping name.
	 * 
	 * <p>
	 * If a feature type mapping for the provided type and mapping name already
	 * exists, it is returned; otherwise, a new one is created.
	 * </p>
	 * 
	 * @param targetType the target type
	 * @param mappingName the mapping name
	 * @return the feature type mapping
	 */
	FeatureTypeMapping getOrCreateFeatureTypeMapping(TypeDefinition targetType, String mappingName,
			boolean secondary);

	/**
	 * Returns the value of the <code>&lt;targetElement&gt;</code> tag for all
	 * feature types in the mapping configuration.
	 * 
	 * @return the set of feature type element names
	 */
	Set<String> getFeatureTypeElements();

	/**
	 * Returns the value of the <code>&lt;targetElement&gt;</code> tag for all
	 * non-feature types in the mapping configuration.
	 * 
	 * @return the set of non-feature type element names
	 */
	Set<String> getNonFeatureTypeElements();

	/**
	 * Returns all configured mappings for the provided feature type.
	 * 
	 * @param featureTypeElement the feature type's element name
	 * @return the mappings
	 */
	Set<FeatureTypeMapping> getFeatureTypeMappings(String featureTypeElement);

	/**
	 * Returns all configured mappings for the provided non-feature type.
	 * 
	 * @param nonFeatureTypeElement the non-feature type's element name
	 * @return the mappings
	 */
	Set<FeatureTypeMapping> getNonFeatureTypeMappings(String nonFeatureTypeElement);

	/**
	 * Returns the unique <code>FEATURE_LINK</code> attribute name for the
	 * specified feature type mapping.
	 * 
	 * <p>
	 * E.g. the first time the method is called, it will return
	 * <code>FEATURE_LINK[1]</code>; if it is called a second time, with the
	 * same input parameters, it will return <code>FEATURE_LINK[2]</code>, and
	 * so on.
	 * </p>
	 * 
	 * @param featureType the feature type
	 * @param mappingName the feature type's mapping name (may be
	 *            <code>null</code>)
	 * @return a unique <code>FEATURE_LINK[i]</code> attribute name
	 */
	String getUniqueFeatureLinkAttribute(TypeDefinition featureType, String mappingName);

	/**
	 * Return the attribute mapping associated to the provided property.
	 * 
	 * <p>
	 * If an attribute mapping for the provided property already exists, it is
	 * returned; otherwise, a new one is created.
	 * </p>
	 * 
	 * @param owningType the type owning the property
	 * @param mappingName the mapping name
	 * @param propertyPath the property path
	 * @return the attribute mapping
	 */
	AttributeMappingType getOrCreateAttributeMapping(TypeDefinition owningType, String mappingName,
			List<ChildContext> propertyPath);

	/**
	 * @return a copy of the wrapped app-schema mapping
	 */
	AppSchemaDataAccessType getAppSchemaMapping();

	/**
	 * Returns true if the wrapped app-schema mapping configuration must be
	 * split in multiple files.
	 * 
	 * <p>
	 * The configuration will be split in a main file containing mappings for
	 * all top-level feature types, and a second file containing mappings for
	 * non-feature types (and alternative mappings for the feature types
	 * configured in the main file).
	 * </p>
	 * 
	 * @return true if multiple files are required to store the mapping
	 *         configuration, false otherwise
	 */
	boolean requiresMultipleFiles();

	/**
	 * Returns the mapping configuration for the main mapping file.
	 * 
	 * <p>
	 * If the mapping does not require multiple files, this method is equivalent
	 * to {@link #getAppSchemaMapping()}.
	 * </p>
	 * 
	 * @return a copy of the main mapping configuration
	 */
	AppSchemaDataAccessType getMainMapping();

	/**
	 * Returns the mapping configuration for the included types mapping file.
	 * 
	 * <p>
	 * If the mapping does not require multiple files, <code>null</code> is
	 * returned.
	 * </p>
	 * 
	 * @return a copy of the included types mapping configuration, or
	 *         <code>null</code>
	 */
	AppSchemaDataAccessType getIncludedTypesMapping();

	/**
	 * Returns the Mapping prefix value.
	 * 
	 * @return Mapping prefix optional
	 */
	Optional<String> getMappingPrefix();

}