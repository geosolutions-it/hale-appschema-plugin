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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.xml.namespace.QName;

import com.google.common.base.Joiner;

import eu.esdihumboldt.hale.common.align.model.ChildContext;
import eu.esdihumboldt.hale.common.align.model.impl.PropertyEntityDefinition;
import eu.esdihumboldt.hale.common.schema.model.ChildDefinition;
import eu.esdihumboldt.hale.common.schema.model.TypeDefinition;
import eu.esdihumboldt.hale.io.xsd.constraint.XmlElements;
import eu.esdihumboldt.hale.io.xsd.model.XmlElement;
import it.geosolutions.hale.io.appschema.AppSchemaIO;
import it.geosolutions.hale.io.appschema.impl.internal.generated.app_schema.AppSchemaDataAccessType;
import it.geosolutions.hale.io.appschema.impl.internal.generated.app_schema.AttributeExpressionMappingType;
import it.geosolutions.hale.io.appschema.impl.internal.generated.app_schema.AttributeExpressionMappingType.Expression;
import it.geosolutions.hale.io.appschema.impl.internal.generated.app_schema.AttributeMappingType;
import it.geosolutions.hale.io.appschema.impl.internal.generated.app_schema.AttributeMappingType.ClientProperty;
import it.geosolutions.hale.io.appschema.impl.internal.generated.app_schema.IncludesPropertyType;
import it.geosolutions.hale.io.appschema.impl.internal.generated.app_schema.NamespacesPropertyType;
import it.geosolutions.hale.io.appschema.impl.internal.generated.app_schema.NamespacesPropertyType.Namespace;
import it.geosolutions.hale.io.appschema.impl.internal.generated.app_schema.SourceDataStoresPropertyType;
import it.geosolutions.hale.io.appschema.impl.internal.generated.app_schema.SourceDataStoresPropertyType.DataStore;
import it.geosolutions.hale.io.appschema.impl.internal.generated.app_schema.SourceDataStoresPropertyType.DataStore.Parameters;
import it.geosolutions.hale.io.appschema.impl.internal.generated.app_schema.SourceDataStoresPropertyType.DataStore.Parameters.Parameter;
import it.geosolutions.hale.io.appschema.impl.internal.generated.app_schema.TargetTypesPropertyType;
import it.geosolutions.hale.io.appschema.impl.internal.generated.app_schema.TargetTypesPropertyType.FeatureType;
import it.geosolutions.hale.io.appschema.impl.internal.generated.app_schema.TypeMappingsPropertyType;
import it.geosolutions.hale.io.appschema.impl.internal.generated.app_schema.TypeMappingsPropertyType.FeatureTypeMapping;
import it.geosolutions.hale.io.appschema.impl.internal.generated.app_schema.TypeMappingsPropertyType.FeatureTypeMapping.AttributeMappings;
import it.geosolutions.hale.io.appschema.writer.AppSchemaMappingUtils;

/**
 * App-schema mapping configuration wrapper base implementation class.
 * 
 * <p>
 * Holds the state associated to the same mapping configuration and provides
 * utility methods to mutate it.
 * </p>
 */
public abstract class AppSchemaMappingWrapperBase implements MappingWrapper {

	private final String defaultPrefix = "nns__";
	private int prefixCounter = 1;
	private final Map<String, Namespace> namespaceUriMap;
	private final Map<String, Namespace> namespacePrefixMap;
	protected final Map<Integer, FeatureTypeMapping> featureTypeMappings;
	private final Map<Integer, Integer> featureLinkCounter;
	private final Map<Integer, AttributeMappingType> attributeMappings;

	protected final Map<String, Set<FeatureTypeMapping>> featureTypesByTargetElement;
	protected final Map<String, Set<FeatureTypeMapping>> nonFeatureTypesByTargetElement;

	protected final AppSchemaDataAccessType appSchemaMapping;

	private String mappingPrefix;

	/**
	 * Constructor.
	 * 
	 * @param appSchemaMapping the app-schema mapping to wrap
	 */
	public AppSchemaMappingWrapperBase(AppSchemaDataAccessType appSchemaMapping) {
		this.appSchemaMapping = appSchemaMapping;

		initMapping(this.appSchemaMapping);

		this.namespaceUriMap = new HashMap<String, Namespace>();
		this.namespacePrefixMap = new HashMap<String, Namespace>();
		this.featureTypeMappings = new HashMap<Integer, FeatureTypeMapping>();
		this.featureLinkCounter = new HashMap<Integer, Integer>();
		this.attributeMappings = new HashMap<Integer, AttributeMappingType>();
		this.featureTypesByTargetElement = new HashMap<String, Set<FeatureTypeMapping>>();
		this.nonFeatureTypesByTargetElement = new HashMap<String, Set<FeatureTypeMapping>>();
	}

	/**
	 * @see it.geosolutions.hale.io.appschema.writer.internal.mapping.MappingWrapper#getDefaultDataStore()
	 */
	@Override
	public DataStore getDefaultDataStore() {
		List<DataStore> dataStores = appSchemaMapping.getSourceDataStores().getDataStore();
		if (dataStores.size() == 0) {
			DataStore defaultDS = new DataStore();
			defaultDS.setId(UUID.randomUUID().toString());
			defaultDS.setParameters(new Parameters());
			dataStores.add(defaultDS);
		}

		return dataStores.get(0);
	}

	/**
	 * @see it.geosolutions.hale.io.appschema.writer.internal.mapping.MappingWrapper#getOrCreateNamespace(java.lang.String,
	 *      java.lang.String)
	 */
	@Override
	public Namespace getOrCreateNamespace(String namespaceURI, String prefix) {
		if (namespaceURI != null && !namespaceURI.isEmpty()) {
			if (!namespaceUriMap.containsKey(namespaceURI)) {
				String basePrefix, uniquePrefix;
				if (prefix == null || prefix.trim().isEmpty()) {
					basePrefix = defaultPrefix;
					uniquePrefix = basePrefix + prefixCounter;
					prefixCounter++;
				}
				else {
					basePrefix = prefix;
					uniquePrefix = basePrefix;
				}
				// make sure prefix is unique
				while (namespacePrefixMap.containsKey(uniquePrefix)) {
					uniquePrefix = basePrefix + prefixCounter;
					prefixCounter++;
				}

				Namespace ns = new Namespace();
				ns.setPrefix(uniquePrefix);
				ns.setUri(namespaceURI);

				namespaceUriMap.put(namespaceURI, ns);
				namespacePrefixMap.put(uniquePrefix, ns);

				appSchemaMapping.getNamespaces().getNamespace().add(ns);

				return ns;
			}
			else {
				// update prefix if provided prefix is not empty and currently
				// assigned prefix was made up
				Namespace ns = namespaceUriMap.get(namespaceURI);
				if (prefix != null && !prefix.isEmpty()
						&& ns.getPrefix().startsWith(defaultPrefix)) {
					// // check prefix is unique
					// if (!namespacePrefixMap.containsKey(prefix)) {
					// remove old prefix-NS mapping from namespacePrefixMap
					namespacePrefixMap.remove(ns.getPrefix());
					// add new prefix-NS mapping to namespacePrefixMap
					ns.setPrefix(prefix);
					namespacePrefixMap.put(prefix, ns);
					// }
				}
				return ns;
			}
		}
		else {
			return null;
		}
	}

	/**
	 * @see it.geosolutions.hale.io.appschema.writer.internal.mapping.MappingWrapper#addSchemaURI(java.lang.String)
	 */
	@Override
	public void addSchemaURI(String schemaURI) {
		if (schemaURI != null && !schemaURI.isEmpty()) {
			this.appSchemaMapping.getTargetTypes().getFeatureType().getSchemaUri().add(schemaURI);
		}
	}

	/**
	 * @see it.geosolutions.hale.io.appschema.writer.internal.mapping.MappingWrapper#updateSchemaURI(java.lang.String,
	 *      java.lang.String)
	 */
	@Override
	public void updateSchemaURI(String oldSchemaURI, String newSchemaURI) {
		if (oldSchemaURI != null && !oldSchemaURI.isEmpty() && newSchemaURI != null
				&& !newSchemaURI.isEmpty()) {
			List<String> uris = this.appSchemaMapping.getTargetTypes().getFeatureType()
					.getSchemaUri();
			if (uris.contains(oldSchemaURI)) {
				uris.remove(oldSchemaURI);
				uris.add(newSchemaURI);
			}
		}
	}

	/**
	 * @see it.geosolutions.hale.io.appschema.writer.internal.mapping.MappingWrapper#buildAttributeXPath(eu.esdihumboldt.hale.common.schema.model.TypeDefinition,
	 *      eu.esdihumboldt.hale.common.align.model.impl.PropertyEntityDefinition)
	 */
	@Override
	public String buildAttributeXPath(TypeDefinition owningType,
			PropertyEntityDefinition propertyEntityDef) {
		List<ChildContext> propertyPath = propertyEntityDef.getPropertyPath();

		return buildAttributeXPath(owningType, propertyPath);
	}

	/**
	 * @see it.geosolutions.hale.io.appschema.writer.internal.mapping.MappingWrapper#buildAttributeXPath(eu.esdihumboldt.hale.common.schema.model.TypeDefinition,
	 *      java.util.List)
	 */
	@Override
	public String buildAttributeXPath(TypeDefinition owningType, List<ChildContext> propertyPath) {

		List<String> pathSegments = new ArrayList<String>();
		for (int i = propertyPath.size() - 1; i >= 0; i--) {
			ChildContext childContext = propertyPath.get(i);
			// TODO: how to handle conditions?
			Integer contextId = childContext.getContextName();
			ChildDefinition<?> child = childContext.getChild();
			// only properties (not groups) are taken into account in building
			// the xpath expression
			if (child.asProperty() != null) {
				String namespaceURI = child.getName().getNamespaceURI();
				String prefix = child.getName().getPrefix();
				String name = child.getName().getLocalPart();

				Namespace ns = getOrCreateNamespace(namespaceURI, prefix);
				String path = ns.getPrefix() + ":" + name;
				if (contextId != null) {
					// XPath indices start from 1, whereas contextId starts from
					// 0 --> add 1
					path = String.format("%s[%d]", path, contextId + 1);
				}
				// insert path segment at the first position
				pathSegments.add(0, path);
			}
			if (child.getParentType() != null
					&& child.getParentType().getName().equals(owningType.getName())) {
				// I reached the owning type: stop walking the path
				break;
			}
		}

		String xPath = Joiner.on("/").join(pathSegments);

		return xPath;

	}

	/**
	 * @see it.geosolutions.hale.io.appschema.writer.internal.mapping.MappingWrapper#getOrCreateFeatureTypeMapping(eu.esdihumboldt.hale.common.schema.model.TypeDefinition)
	 */
	@Override
	public FeatureTypeMapping getOrCreateFeatureTypeMapping(TypeDefinition targetType) {
		return getOrCreateFeatureTypeMapping(targetType, false);
	}

	/**
	 * @see it.geosolutions.hale.io.appschema.writer.internal.mapping.MappingWrapper#getOrCreateFeatureTypeMapping(eu.esdihumboldt.hale.common.schema.model.TypeDefinition,
	 *      boolean)
	 */
	@Override
	public FeatureTypeMapping getOrCreateFeatureTypeMapping(TypeDefinition targetType,
			boolean secondary) {
		return getOrCreateFeatureTypeMapping(targetType, null, secondary);
	}

	/**
	 * @see it.geosolutions.hale.io.appschema.writer.internal.mapping.MappingWrapper#getOrCreateFeatureTypeMapping(eu.esdihumboldt.hale.common.schema.model.TypeDefinition,
	 *      java.lang.String)
	 */
	@Override
	public FeatureTypeMapping getOrCreateFeatureTypeMapping(TypeDefinition targetType,
			String mappingName) {
		return getOrCreateFeatureTypeMapping(targetType, mappingName, false);
	}

	/**
	 * @see it.geosolutions.hale.io.appschema.writer.internal.mapping.MappingWrapper#getOrCreateFeatureTypeMapping(eu.esdihumboldt.hale.common.schema.model.TypeDefinition,
	 *      java.lang.String, boolean)
	 */
	@Override
	public FeatureTypeMapping getOrCreateFeatureTypeMapping(TypeDefinition targetType,
			String mappingName, boolean secondary) {
		if (targetType == null) {
			return null;
		}

		Integer hashKey = getFeatureTypeMappingHashKey(targetType, mappingName);
		if (!featureTypeMappings.containsKey(hashKey)) {
			// create
			FeatureTypeMapping featureTypeMapping = new FeatureTypeMapping();
			// initialize attribute mappings member
			featureTypeMapping.setAttributeMappings(new AttributeMappings());
			// TODO: how do I know the datasource from which data will be read?
			featureTypeMapping.setSourceDataStore(getDefaultDataStore().getId());
			// Retrieve namespace this feature type belongs to and prepend its
			// prefix to the feature type name; if a namespace with the same URI
			// already existed with a valid prefix, that will be used instead of
			// the one passed here
			Namespace ns = getOrCreateNamespace(targetType.getName().getNamespaceURI(),
					targetType.getName().getPrefix());
			// TODO: I'm getting the element name with
			// targetType.getDisplayName():
			// isn't there a more elegant (and perhaps more reliable) way to
			// know which element corresponds to a type?
			featureTypeMapping.setTargetElement(ns.getPrefix() + ":" + targetType.getDisplayName());
			if (mappingName != null && !mappingName.isEmpty()) {
				featureTypeMapping.setMappingName(mappingName);
			}

			appSchemaMapping.getTypeMappings().getFeatureTypeMapping().add(featureTypeMapping);
			featureTypeMappings.put(hashKey, featureTypeMapping);
			addToFeatureTypeMappings(targetType, featureTypeMapping);
		}
		return featureTypeMappings.get(hashKey);
	}

	protected String getTargetElementName(TypeDefinition targetType) {
		// let's try first to obtain the target element name from the XML
		// element
		XmlElements xmlElements = targetType.getConstraint(XmlElements.class);
		Collection<? extends XmlElement> elements = xmlElements.getElements();
		if (elements != null && elements.size() == 1) {
			// only use the element name if it is unique
			QName name = elements.iterator().next().getName();
			return getPrefix(name) + ":" + name.getLocalPart();
		}
		// let's try to infer the target element name using the display name
		QName targetName = targetType.getName();
		return getPrefix(targetName) + ":" + targetType.getDisplayName();
	}

	private String getPrefix(QName name) {
		String prefix = name.getPrefix();
		if (prefix == null || prefix.isEmpty()) {
			Namespace ns = getOrCreateNamespace(name.getNamespaceURI(), name.getPrefix());
			prefix = ns.getPrefix();
		}
		return prefix;
	}

	protected Integer getFeatureTypeMappingHashKey(TypeDefinition targetType, String mappingName) {
		String hashBase = targetType.getName().toString();
		if (mappingName != null && !mappingName.isEmpty()) {
			hashBase += "__" + mappingName;
		}

		return hashBase.hashCode();
	}

	private void addToFeatureTypeMappings(TypeDefinition targetType,
			FeatureTypeMapping typeMapping) {
		Map<String, Set<FeatureTypeMapping>> mappingsByTargetElement = null;
		if (AppSchemaMappingUtils.isFeatureType(targetType)) {
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
	 * @see it.geosolutions.hale.io.appschema.writer.internal.mapping.MappingWrapper#getFeatureTypeElements()
	 */
	@Override
	public Set<String> getFeatureTypeElements() {
		return featureTypesByTargetElement.keySet();
	}

	/**
	 * @see it.geosolutions.hale.io.appschema.writer.internal.mapping.MappingWrapper#getNonFeatureTypeElements()
	 */
	@Override
	public Set<String> getNonFeatureTypeElements() {
		return nonFeatureTypesByTargetElement.keySet();
	}

	/**
	 * @see it.geosolutions.hale.io.appschema.writer.internal.mapping.MappingWrapper#getFeatureTypeMappings(java.lang.String)
	 */
	@Override
	public Set<FeatureTypeMapping> getFeatureTypeMappings(String featureTypeElement) {
		return getTypeMappingsByElement(featureTypesByTargetElement, featureTypeElement);
	}

	/**
	 * @see it.geosolutions.hale.io.appschema.writer.internal.mapping.MappingWrapper#getNonFeatureTypeMappings(java.lang.String)
	 */
	@Override
	public Set<FeatureTypeMapping> getNonFeatureTypeMappings(String nonFeatureTypeElement) {
		return getTypeMappingsByElement(nonFeatureTypesByTargetElement, nonFeatureTypeElement);
	}

	private Set<FeatureTypeMapping> getTypeMappingsByElement(
			Map<String, Set<FeatureTypeMapping>> mappingsByTargetElement, String typeElement) {
		Set<FeatureTypeMapping> mappings = null;
		if (mappingsByTargetElement.containsKey(typeElement)) {
			mappings = mappingsByTargetElement.get(typeElement);
		}
		else {
			mappings = Collections.emptySet();
		}
		return mappings;
	}

	/**
	 * @see it.geosolutions.hale.io.appschema.writer.internal.mapping.MappingWrapper#getUniqueFeatureLinkAttribute(eu.esdihumboldt.hale.common.schema.model.TypeDefinition,
	 *      java.lang.String)
	 */
	@Override
	public String getUniqueFeatureLinkAttribute(TypeDefinition featureType, String mappingName) {
		Integer featureTypeKey = getFeatureTypeMappingHashKey(featureType, mappingName);
		if (!featureLinkCounter.containsKey(featureTypeKey)) {
			featureLinkCounter.put(featureTypeKey, 0);
		}
		Integer counter = featureLinkCounter.get(featureTypeKey);
		// update counter
		featureLinkCounter.put(featureTypeKey, ++counter);

		return String.format("%s[%d]", FEATURE_LINK_FIELD, counter);
	}

	/**
	 * @see it.geosolutions.hale.io.appschema.writer.internal.mapping.MappingWrapper#getOrCreateAttributeMapping(eu.esdihumboldt.hale.common.schema.model.TypeDefinition,
	 *      java.lang.String, java.util.List)
	 */
	@Override
	public AttributeMappingType getOrCreateAttributeMapping(TypeDefinition owningType,
			String mappingName, List<ChildContext> propertyPath) {
		if (!validPropertyPath(propertyPath)) {
			return null;
		}

		Integer hashKey = getAttruteMappingHashKey(owningType, propertyPath);
		if (!attributeMappings.containsKey(hashKey)) {
			// create
			AttributeMappingType attrMapping = new AttributeMappingType();
			// add to owning type mapping
			FeatureTypeMapping ftMapping = getOrCreateFeatureTypeMapping(owningType, mappingName);
			ftMapping.getAttributeMappings().getAttributeMapping().add(attrMapping);
			// put into internal map
			attributeMappings.put(hashKey, attrMapping);
		}
		return attributeMappings.get(hashKey);
	}

	protected boolean validPropertyPath(List<ChildContext> propertyPath) {
		return !(propertyPath == null || propertyPath.isEmpty());
	}

	protected Integer getAttruteMappingHashKey(TypeDefinition owningType,
			List<ChildContext> propertyPath) {
		final String SEPARATOR = "__";
		StringBuilder pathBuilder = new StringBuilder();

		if (owningType != null) {
			pathBuilder.append(owningType.getName().toString()).append(SEPARATOR);
			for (ChildContext childContext : propertyPath) {
				pathBuilder.append(childContext.getChild().getName().toString());
				if (childContext.getContextName() != null) {
					pathBuilder.append(childContext.getContextName());
				}
				pathBuilder.append(SEPARATOR);
			}
		}
		else {
			throw new IllegalArgumentException("Could not find feature type owning property");
		}

		return pathBuilder.toString().hashCode();
	}

	/**
	 * @see it.geosolutions.hale.io.appschema.writer.internal.mapping.MappingWrapper#getAppSchemaMapping()
	 */
	@Override
	public AppSchemaDataAccessType getAppSchemaMapping() {
		return cloneMapping(appSchemaMapping);
	}

	/**
	 * @see it.geosolutions.hale.io.appschema.writer.internal.mapping.MappingWrapper#requiresMultipleFiles()
	 */
	@Override
	public boolean requiresMultipleFiles() {
		// if non-feature type mappings are present, return true
		if (nonFeatureTypesByTargetElement.size() > 0) {
			return true;
		}

		// check whether multiple mappings of the same feature type are present
		for (String targetElement : featureTypesByTargetElement.keySet()) {
			if (featureTypesByTargetElement.get(targetElement).size() > 1) {
				return true;
			}
		}

		// don't need multiple files
		return false;
	}

	/**
	 * @see it.geosolutions.hale.io.appschema.writer.internal.mapping.MappingWrapper#getMainMapping()
	 */
	@Override
	public AppSchemaDataAccessType getMainMapping() {
		AppSchemaDataAccessType mainMapping = cloneMapping(appSchemaMapping);

		if (requiresMultipleFiles()) {
			// add included types configuration
			mainMapping.getIncludedTypes().getInclude()
					.add(AppSchemaIO.INCLUDED_TYPES_MAPPING_FILE);

			Set<FeatureTypeMapping> toBeRemoved = new HashSet<FeatureTypeMapping>();
			Set<FeatureTypeMapping> toBeKept = new HashSet<FeatureTypeMapping>();
			groupTypeMappings(toBeKept, toBeRemoved);
			purgeTypeMappings(mainMapping, toBeRemoved);
		}

		return mainMapping;
	}

	/**
	 * @see it.geosolutions.hale.io.appschema.writer.internal.mapping.MappingWrapper#getIncludedTypesMapping()
	 */
	@Override
	public AppSchemaDataAccessType getIncludedTypesMapping() {
		if (requiresMultipleFiles()) {
			AppSchemaDataAccessType includedTypesMapping = cloneMapping(appSchemaMapping);

			Set<FeatureTypeMapping> toBeRemoved = new HashSet<FeatureTypeMapping>();
			Set<FeatureTypeMapping> toBeKept = new HashSet<FeatureTypeMapping>();
			groupTypeMappings(toBeRemoved, toBeKept);
			purgeTypeMappings(includedTypesMapping, toBeRemoved);

			return includedTypesMapping;
		}
		else {
			return null;
		}
	}

	private void groupTypeMappings(Set<FeatureTypeMapping> mainTypes,
			Set<FeatureTypeMapping> includedTypes) {
		// look for multiple mappings of the same feature type and determine
		// the top level feature type mappings
		for (Set<FeatureTypeMapping> ftMappings : featureTypesByTargetElement.values()) {
			if (ftMappings.size() > 1) {
				FeatureTypeMapping topLevelMapping = null;
				for (FeatureTypeMapping m : ftMappings) {
					if (topLevelMapping != null) {
						// top level mapping already found, drop the others
						includedTypes.add(m);
					}
					else {
						if (m.getMappingName() == null || m.getMappingName().trim().isEmpty()) {
							// use this as top level mapping
							// TODO: there's no guarantee this is the right one
							// to pick
							topLevelMapping = m;
						}
					}
				}
				if (topLevelMapping == null) {
					// pick the first one (it's pretty much a random choice)
					topLevelMapping = ftMappings.iterator().next();
				}
				mainTypes.add(topLevelMapping);
			}
			else {
				mainTypes.add(ftMappings.iterator().next());
			}
		}

		// non-feature type mappings go in the "included types" group
		for (Set<FeatureTypeMapping> ftMappings : nonFeatureTypesByTargetElement.values()) {
			includedTypes.addAll(ftMappings);
		}
	}

	private void purgeTypeMappings(AppSchemaDataAccessType mapping,
			Set<FeatureTypeMapping> toBeRemoved) {
		Set<String> usedStores = new HashSet<String>();
		Iterator<FeatureTypeMapping> featureIt = mapping.getTypeMappings().getFeatureTypeMapping()
				.iterator();
		while (featureIt.hasNext()) {
			FeatureTypeMapping ftMapping = featureIt.next();
			if (lookupTypeMapping(ftMapping, toBeRemoved) != null) {
				featureIt.remove();
			}
			else {
				usedStores.add(ftMapping.getSourceDataStore());
			}
		}

		// remove unnecessary DataStores
		Iterator<DataStore> storeIt = mapping.getSourceDataStores().getDataStore().iterator();
		while (storeIt.hasNext()) {
			if (!usedStores.contains(storeIt.next().getId())) {
				storeIt.remove();
			}
		}
	}

	private FeatureTypeMapping lookupTypeMapping(FeatureTypeMapping ftMapping,
			Set<FeatureTypeMapping> candidates) {
		for (FeatureTypeMapping candidate : candidates) {
			boolean sameElement = ftMapping.getTargetElement().equals(candidate.getTargetElement());
			boolean noMappingName = ftMapping.getMappingName() == null
					&& candidate.getMappingName() == null;
			boolean sameMappingName = false;
			if (!noMappingName) {
				sameMappingName = ftMapping.getMappingName() != null
						&& ftMapping.getMappingName().equals(candidate.getMappingName());
			}
			if (sameElement && (noMappingName || sameMappingName)) {
				return candidate;
			}
		}

		return null;
	}

	static AppSchemaDataAccessType cloneMapping(AppSchemaDataAccessType mapping) {
		AppSchemaDataAccessType clone = new AppSchemaDataAccessType();

		initMapping(clone);

		clone.setCatalog(mapping.getCatalog());
		clone.getIncludedTypes().getInclude().addAll(mapping.getIncludedTypes().getInclude());
		for (Namespace ns : mapping.getNamespaces().getNamespace()) {
			clone.getNamespaces().getNamespace().add(cloneNamespace(ns));
		}
		for (DataStore ds : mapping.getSourceDataStores().getDataStore()) {
			clone.getSourceDataStores().getDataStore().add(cloneDataStore(ds));
		}
		clone.getTargetTypes().getFeatureType().getSchemaUri()
				.addAll(mapping.getTargetTypes().getFeatureType().getSchemaUri());
		for (FeatureTypeMapping ftMapping : mapping.getTypeMappings().getFeatureTypeMapping()) {
			clone.getTypeMappings().getFeatureTypeMapping().add(cloneFeatureTypeMapping(ftMapping));
		}

		return clone;
	}

	static Namespace cloneNamespace(Namespace ns) {
		if (ns == null) {
			return null;
		}

		Namespace clone = new Namespace();
		clone.setPrefix(ns.getPrefix());
		clone.setUri(ns.getUri());

		return clone;
	}

	static DataStore cloneDataStore(DataStore ds) {
		DataStore clone = new DataStore();
		clone.setParameters(new Parameters());
		clone.setId(ds.getId());
		clone.setIdAttribute(ds.getIdAttribute());

		if (ds.getParameters() != null) {
			for (Parameter param : ds.getParameters().getParameter()) {
				Parameter paramClone = new Parameter();
				paramClone.setName(param.getName());
				paramClone.setValue(param.getValue());
				clone.getParameters().getParameter().add(paramClone);
			}
		}

		return clone;
	}

	static FeatureTypeMapping cloneFeatureTypeMapping(FeatureTypeMapping ftMapping) {
		FeatureTypeMapping clone = new FeatureTypeMapping();
		clone.setAttributeMappings(new AttributeMappings());
		if (ftMapping.getAttributeMappings() != null) {
			for (AttributeMappingType attrMapping : ftMapping.getAttributeMappings()
					.getAttributeMapping()) {
				clone.getAttributeMappings().getAttributeMapping()
						.add(cloneAttributeMapping(attrMapping));
			}
		}
		clone.setIsDenormalised(ftMapping.isIsDenormalised());
		clone.setIsXmlDataStore(ftMapping.isIsXmlDataStore());
		clone.setItemXpath(ftMapping.getItemXpath());
		clone.setMappingName(ftMapping.getMappingName());
		clone.setSourceDataStore(ftMapping.getSourceDataStore());
		clone.setSourceType(ftMapping.getSourceType());
		clone.setTargetElement(ftMapping.getTargetElement());

		return clone;
	}

	static AttributeMappingType cloneAttributeMapping(AttributeMappingType attrMapping) {
		AttributeMappingType clone = new AttributeMappingType();

		clone.setEncodeIfEmpty(attrMapping.isEncodeIfEmpty());
		clone.setIsList(attrMapping.isIsList());
		clone.setIsMultiple(attrMapping.isIsMultiple());
		for (ClientProperty clientProp : attrMapping.getClientProperty()) {
			ClientProperty clientPropClone = new ClientProperty();
			clientPropClone.setName(clientProp.getName());
			clientPropClone.setValue(clientProp.getValue());
			clone.getClientProperty().add(clientPropClone);
		}
		clone.setIdExpression(cloneAttributeExpression(attrMapping.getIdExpression()));
		clone.setInstancePath(attrMapping.getInstancePath());
		clone.setLabel(attrMapping.getLabel());
		clone.setParentLabel(attrMapping.getParentLabel());
		clone.setSourceExpression(cloneAttributeExpression(attrMapping.getSourceExpression()));
		clone.setTargetAttribute(attrMapping.getTargetAttribute());
		clone.setTargetAttributeNode(attrMapping.getTargetAttributeNode());
		clone.setTargetQueryString(attrMapping.getTargetQueryString());

		return clone;
	}

	static AttributeExpressionMappingType cloneAttributeExpression(
			AttributeExpressionMappingType attrExpression) {
		if (attrExpression == null) {
			return attrExpression;
		}

		AttributeExpressionMappingType clone = new AttributeExpressionMappingType();
		if (attrExpression.getExpression() != null) {
			clone.setExpression(new Expression());
			// TODO: Expression is xs:anyType, how can I make a copy of it?
			clone.getExpression().setExpression(attrExpression.getExpression().getExpression());
		}
		clone.setIndex(attrExpression.getIndex());
		clone.setInputAttribute(attrExpression.getInputAttribute());
		clone.setLinkElement(attrExpression.getLinkElement());
		clone.setLinkField(attrExpression.getLinkField());
		clone.setOCQL(attrExpression.getOCQL());

		return clone;
	}

	/**
	 * If necessary, initializes fields to minimize the risk of undesired NPEs.
	 * 
	 * @param mapping the mapping
	 */
	private static void initMapping(AppSchemaDataAccessType mapping) {
		if (mapping.getNamespaces() == null) {
			mapping.setNamespaces(new NamespacesPropertyType());
		}
		if (mapping.getSourceDataStores() == null) {
			mapping.setSourceDataStores(new SourceDataStoresPropertyType());
		}
		if (mapping.getIncludedTypes() == null) {
			mapping.setIncludedTypes(new IncludesPropertyType());
		}
		if (mapping.getTargetTypes() == null) {
			mapping.setTargetTypes(new TargetTypesPropertyType());
		}
		if (mapping.getTargetTypes().getFeatureType() == null) {
			mapping.getTargetTypes().setFeatureType(new FeatureType());
		}
		if (mapping.getTypeMappings() == null) {
			mapping.setTypeMappings(new TypeMappingsPropertyType());
		}
	}

	/**
	 * @see it.geosolutions.hale.io.appschema.writer.internal.mapping.MappingWrapper#setMappingPrefix(java.lang.String)
	 */
	@Override
	public void setMappingPrefix(String mappingPrefix) {
		this.mappingPrefix = mappingPrefix;
	}

	@Override
	public Optional<String> getMappingPrefix() {
		return Optional.ofNullable(mappingPrefix);
	}
}
