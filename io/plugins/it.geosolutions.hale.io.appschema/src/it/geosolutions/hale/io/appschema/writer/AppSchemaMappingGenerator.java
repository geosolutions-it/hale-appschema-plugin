/*
 * Copyright (c) 2015 Data Harmonisation Panel
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
 *     Data Harmonisation Panel <http://www.dhpanel.eu>
 */

package it.geosolutions.hale.io.appschema.writer;

import static it.geosolutions.hale.io.appschema.AppSchemaIO.isHrefClientPropertyCompatible;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.lang.StringUtils;

import com.google.common.collect.ListMultimap;

import de.fhg.igd.slf4jplus.ALogger;
import de.fhg.igd.slf4jplus.ALoggerFactory;
import eu.esdihumboldt.hale.common.align.model.Alignment;
import eu.esdihumboldt.hale.common.align.model.Cell;
import eu.esdihumboldt.hale.common.align.model.ChildContext;
import eu.esdihumboldt.hale.common.align.model.Entity;
import eu.esdihumboldt.hale.common.align.model.EntityDefinition;
import eu.esdihumboldt.hale.common.core.io.report.IOReporter;
import eu.esdihumboldt.hale.common.core.io.report.impl.IOMessageImpl;
import eu.esdihumboldt.hale.common.schema.model.ChildDefinition;
import eu.esdihumboldt.hale.common.schema.model.DefinitionGroup;
import eu.esdihumboldt.hale.common.schema.model.PropertyDefinition;
import eu.esdihumboldt.hale.common.schema.model.Schema;
import eu.esdihumboldt.hale.common.schema.model.SchemaSpace;
import eu.esdihumboldt.hale.common.schema.model.impl.AbstractPropertyDecorator;
import eu.esdihumboldt.hale.common.schema.model.impl.DefaultPropertyDefinition;
import eu.esdihumboldt.hale.common.schema.model.impl.DefaultTypeDefinition;
import it.geosolutions.hale.io.appschema.AppSchemaIO;
import it.geosolutions.hale.io.appschema.impl.internal.generated.app_schema.AppSchemaDataAccessType;
import it.geosolutions.hale.io.appschema.impl.internal.generated.app_schema.NamespacesPropertyType.Namespace;
import it.geosolutions.hale.io.appschema.impl.internal.generated.app_schema.ObjectFactory;
import it.geosolutions.hale.io.appschema.impl.internal.generated.app_schema.SourceDataStoresPropertyType.DataStore;
import it.geosolutions.hale.io.appschema.impl.internal.generated.app_schema.SourceDataStoresPropertyType.DataStore.Parameters.Parameter;
import it.geosolutions.hale.io.appschema.impl.internal.generated.app_schema.TypeMappingsPropertyType.FeatureTypeMapping;
import it.geosolutions.hale.io.appschema.model.FeatureChaining;
import it.geosolutions.hale.io.appschema.model.WorkspaceConfiguration;
import it.geosolutions.hale.io.appschema.writer.internal.PropertyTransformationHandler;
import it.geosolutions.hale.io.appschema.writer.internal.PropertyTransformationHandlerFactory;
import it.geosolutions.hale.io.appschema.writer.internal.TypeTransformationHandler;
import it.geosolutions.hale.io.appschema.writer.internal.TypeTransformationHandlerFactory;
import it.geosolutions.hale.io.appschema.writer.internal.UnsupportedTransformationException;
import it.geosolutions.hale.io.appschema.writer.internal.mapping.AppSchemaMappingContext;
import it.geosolutions.hale.io.appschema.writer.internal.mapping.AppSchemaMappingWrapper;
import it.geosolutions.hale.io.appschema.writer.internal.mapping.MappingWrapper;
import it.geosolutions.hale.io.geoserver.AppSchemaDataStore;
import it.geosolutions.hale.io.geoserver.FeatureType;
import it.geosolutions.hale.io.geoserver.Layer;
import it.geosolutions.hale.io.geoserver.ResourceBuilder;
import it.geosolutions.hale.io.geoserver.Workspace;

/**
 * Translates a HALE alignment to an app-schema mapping configuration.
 * 
 * @author Stefano Costa, GeoSolutions
 */
public class AppSchemaMappingGenerator implements MappingGenerator {

	private static final ALogger log = ALoggerFactory.getLogger(AppSchemaMappingGenerator.class);

	private static final String NET_OPENGIS_OGC_CONTEXT = "it.geosolutions.hale.io.appschema.impl.internal.generated.net_opengis_ogc";
	private static final String APP_SCHEMA_CONTEXT = "it.geosolutions.hale.io.appschema.impl.internal.generated.app_schema";

	protected final Alignment alignment;
	protected final SchemaSpace targetSchemaSpace;
	protected final Schema targetSchema;
	private final DataStore dataStore;
	protected final FeatureChaining chainingConf;
	protected final WorkspaceConfiguration workspaceConf;
	protected MappingWrapper mappingWrapper;
	protected AppSchemaMappingContext context;
	protected AppSchemaDataAccessType mainMapping;
	protected AppSchemaDataAccessType includedTypesMapping;

	/**
	 * Constructor.
	 * 
	 * @param alignment the alignment to translate
	 * @param targetSchemaSpace the target schema space
	 * @param dataStore the DataStore configuration to use
	 * @param chainingConf the feature chaining configuration
	 * @param workspaceConf the workspace configuration
	 */
	public AppSchemaMappingGenerator(Alignment alignment, SchemaSpace targetSchemaSpace,
			DataStore dataStore, FeatureChaining chainingConf,
			WorkspaceConfiguration workspaceConf) {
		this.alignment = alignment;
		this.targetSchemaSpace = targetSchemaSpace;
		// pick the target schemas from which interpolation variables will be
		// derived
		this.targetSchema = pickTargetSchema();
		this.dataStore = dataStore;
		this.chainingConf = chainingConf;
		this.workspaceConf = workspaceConf;
	}

	/**
	 * @see it.geosolutions.hale.io.appschema.writer.MappingGenerator#generateMapping(eu.esdihumboldt.hale.common.core.io.report.IOReporter)
	 */
	@Override
	public MappingWrapper generateMapping(IOReporter reporter) throws IOException {
		// reset wrapper
		resetMappingState();

		try {
			AppSchemaDataAccessType mapping = loadMappingTemplate();

			mappingWrapper = new AppSchemaMappingWrapper(mapping);
			context = new AppSchemaMappingContext(mappingWrapper, alignment,
					targetSchema.getMappingRelevantTypes(), chainingConf, workspaceConf);

			// create namespace objects for all target types / properties
			// TODO: this removes all namespaces that were defined in the
			// template file, add code to cope with pre-configured namespaces
			// instead
			mapping.getNamespaces().getNamespace().clear();
			createNamespaces();

			// apply datastore configuration, if any
			// TODO: for now, only a single datastore is supported
			applyDataStoreConfig();

			// populate targetTypes element
			createTargetTypes();

			// populate typeMappings element
			createTypeMappings(context, reporter);

			// cache mainMapping and includedTypesMapping for performance
			mainMapping = mappingWrapper.getMainMapping();
			includedTypesMapping = mappingWrapper.getIncludedTypesMapping();

			return mappingWrapper;
		} catch (Exception e) {
			// making sure state is reset in case an exception is thrown
			resetMappingState();
			throw e;
		}
	}

	protected void resetMappingState() {
		mappingWrapper = null;
		mainMapping = null;
		includedTypesMapping = null;
	}

	/**
	 * @see it.geosolutions.hale.io.appschema.writer.MappingGenerator#getGeneratedMapping()
	 */
	@Override
	public MappingWrapper getGeneratedMapping() {
		checkMappingGenerated();

		return mappingWrapper;
	}

	/**
	 * @see it.geosolutions.hale.io.appschema.writer.MappingGenerator#generateMapping(java.io.OutputStream,
	 *      eu.esdihumboldt.hale.common.core.io.report.IOReporter)
	 */
	@Override
	public void generateMapping(OutputStream output, IOReporter reporter) throws IOException {
		generateMapping(reporter);

		writeMappingConf(output);
	}

	/**
	 * @see it.geosolutions.hale.io.appschema.writer.MappingGenerator#generateIncludedTypesMapping(java.io.OutputStream,
	 *      eu.esdihumboldt.hale.common.core.io.report.IOReporter)
	 */
	@Override
	public void generateIncludedTypesMapping(OutputStream output, IOReporter reporter)
			throws IOException {
		generateMapping(reporter);

		writeIncludedTypesMappingConf(output);
	}

	/**
	 * @see it.geosolutions.hale.io.appschema.writer.MappingGenerator#updateSchemaURI(java.lang.String,
	 *      java.lang.String)
	 */
	@Override
	public void updateSchemaURI(String oldSchemaURI, String newSchemaURI) {
		checkMappingGenerated();

		mappingWrapper.updateSchemaURI(oldSchemaURI, newSchemaURI);
		// regenerate cached mappings
		mainMapping = mappingWrapper.getMainMapping();
		includedTypesMapping = mappingWrapper.getIncludedTypesMapping();
	}

	/**
	 * @see it.geosolutions.hale.io.appschema.writer.MappingGenerator#getAppSchemaDataStore()
	 */
	@Override
	public it.geosolutions.hale.io.geoserver.DataStore getAppSchemaDataStore() {
		checkMappingGenerated();
		checkTargetSchemaAvailable();

		it.geosolutions.hale.io.geoserver.Namespace ns = getMainNamespace();
		Workspace ws = getMainWorkspace();

		String workspaceId = (String) ws.getAttribute(Workspace.ID);
		String dataStoreName = extractSchemaName(targetSchema.getLocation());
		String dataStoreId = dataStoreName + "_datastore";
		String mappingFileName = dataStoreName + ".xml";
		Map<String, String> connectionParameters = new HashMap<String, String>();
		connectionParameters.put("uri",
				(String) ns.getAttribute(it.geosolutions.hale.io.geoserver.Namespace.URI));
		connectionParameters.put("workspaceName", ws.name());
		connectionParameters.put("mappingFileName", mappingFileName);

		return ResourceBuilder.dataStore(dataStoreName, AppSchemaDataStore.class)
				.setAttribute(it.geosolutions.hale.io.geoserver.DataStore.ID, dataStoreId)
				.setAttribute(it.geosolutions.hale.io.geoserver.DataStore.WORKSPACE_ID, workspaceId)
				.setAttribute(it.geosolutions.hale.io.geoserver.DataStore.CONNECTION_PARAMS,
						connectionParameters)
				.build();
	}

	/**
	 * @see it.geosolutions.hale.io.appschema.writer.MappingGenerator#getMainWorkspace()
	 */
	@Override
	public Workspace getMainWorkspace() {
		checkMappingGenerated();
		checkTargetSchemaAvailable();

		Namespace ns = context.getOrCreateNamespace(targetSchema.getNamespace(), null);
		Workspace ws = getWorkspace(ns.getPrefix(), ns.getUri());

		return ws;
	}

	/**
	 * @see it.geosolutions.hale.io.appschema.writer.MappingGenerator#getMainNamespace()
	 */
	@Override
	public it.geosolutions.hale.io.geoserver.Namespace getMainNamespace() {
		checkMappingGenerated();
		checkTargetSchemaAvailable();

		Namespace ns = context.getOrCreateNamespace(targetSchema.getNamespace(), null);
		return getNamespace(ns);
	}

	/**
	 * @see it.geosolutions.hale.io.appschema.writer.MappingGenerator#getSecondaryNamespaces()
	 */
	@Override
	public List<it.geosolutions.hale.io.geoserver.Namespace> getSecondaryNamespaces() {
		checkMappingGenerated();
		checkTargetSchemaAvailable();

		List<it.geosolutions.hale.io.geoserver.Namespace> secondaryNamespaces = new ArrayList<it.geosolutions.hale.io.geoserver.Namespace>();
//		for (Namespace ns : mappingWrapper.getAppSchemaMapping().getNamespaces().getNamespace()) {
		for (Namespace ns : mainMapping.getNamespaces().getNamespace()) {
			if (!ns.getUri().equals(targetSchema.getNamespace())) {
				secondaryNamespaces.add(getNamespace(ns));
			}
		}

		return secondaryNamespaces;
	}

	/**
	 * @see it.geosolutions.hale.io.appschema.writer.MappingGenerator#getWorkspace(it.geosolutions.hale.io.geoserver.Namespace)
	 */
	@Override
	public Workspace getWorkspace(it.geosolutions.hale.io.geoserver.Namespace ns) {
		Object namespaceUri = ns.getAttribute(it.geosolutions.hale.io.geoserver.Namespace.URI);
		Workspace ws = getWorkspace(ns.name(), String.valueOf(namespaceUri));

		return ws;
	}

	private it.geosolutions.hale.io.geoserver.Namespace getNamespace(Namespace ns) {
		String prefix = ns.getPrefix();
		String uri = ns.getUri();
		String namespaceId = prefix + "_namespace";

		return ResourceBuilder.namespace(prefix)
				.setAttribute(it.geosolutions.hale.io.geoserver.Namespace.ID, namespaceId)
				.setAttribute(it.geosolutions.hale.io.geoserver.Namespace.URI, uri)
				.setAttribute(it.geosolutions.hale.io.geoserver.Namespace.ISOLATED, isIsolated(uri))
				.build();
	}

	private Workspace getWorkspace(String nsPrefix, String nsUri) {
		String workspaceId = nsPrefix + "_workspace";
		String workspaceName = nsPrefix;

		return ResourceBuilder.workspace(workspaceName).setAttribute(Workspace.ID, workspaceId)
				.setAttribute(Workspace.ISOLATED, isIsolated(nsUri)).build();
	}

	/**
	 * @see it.geosolutions.hale.io.appschema.writer.MappingGenerator#getFeatureTypes()
	 */
	@Override
	public List<FeatureType> getFeatureTypes() {
		checkMappingGenerated();

		it.geosolutions.hale.io.geoserver.DataStore dataStore = getAppSchemaDataStore();

		List<FeatureType> featureTypes = new ArrayList<FeatureType>();
//		for (FeatureTypeMapping ftMapping : mappingWrapper.getAppSchemaMapping().getTypeMappings()
//				.getFeatureTypeMapping()) {
		for (FeatureTypeMapping ftMapping : mainMapping.getTypeMappings().getFeatureTypeMapping()) {
			featureTypes.add(getFeatureType(dataStore, ftMapping));
		}

		return featureTypes;
	}

	private FeatureType getFeatureType(it.geosolutions.hale.io.geoserver.DataStore dataStore,
			FeatureTypeMapping ftMapping) {
		String featureTypeName = stripPrefix(ftMapping.getTargetElement());
		String featureTypeId = featureTypeName + "_featureType";
		String dataStoreId = (String) dataStore
				.getAttribute(it.geosolutions.hale.io.geoserver.DataStore.ID);
		it.geosolutions.hale.io.geoserver.Namespace ns = getMainNamespace();

		return ResourceBuilder.featureType(featureTypeName)
				.setAttribute(FeatureType.ID, featureTypeId)
				.setAttribute(FeatureType.DATASTORE_ID, dataStoreId)
				.setAttribute(FeatureType.NAMESPACE_ID,
						ns.getAttribute(it.geosolutions.hale.io.geoserver.Namespace.ID))
				.build();
	}

	/**
	 * @see it.geosolutions.hale.io.appschema.writer.MappingGenerator#getLayer(it.geosolutions.hale.io.geoserver.FeatureType)
	 */
	@Override
	public Layer getLayer(FeatureType featureType) {
		String featureTypeName = featureType.name();
		String featureTypeId = (String) featureType.getAttribute(FeatureType.ID);
		String layerName = featureTypeName;
		String layerId = layerName + "_layer";

		return ResourceBuilder.layer(layerName).setAttribute(Layer.ID, layerId)
				.setAttribute(Layer.FEATURE_TYPE_ID, featureTypeId).build();
	}

	protected void checkMappingGenerated() {
		if (mappingWrapper == null || mainMapping == null
				|| (includedTypesMapping == null && mappingWrapper.requiresMultipleFiles())) {
			throw new IllegalStateException("No mapping has been generated yet");
		}
	}

	protected void checkTargetSchemaAvailable() {
		if (targetSchema == null) {
			throw new IllegalStateException("Target schema not available");
		}
	}

	private Schema pickTargetSchema() {
		if (this.targetSchemaSpace == null) {
			return null;
		}

		return this.targetSchemaSpace.getSchemas().iterator().next();
	}

	protected String extractSchemaName(URI schemaLocation) {
		String path = schemaLocation.getPath();
		String fragment = schemaLocation.getFragment();
		if (fragment != null && !fragment.isEmpty()) {
			path = path.replace(fragment, "");
		}
		int lastSlashIdx = path.lastIndexOf('/');
		int lastDotIdx = path.lastIndexOf('.');
		if (lastSlashIdx >= 0) {
			if (lastDotIdx >= 0) {
				return path.substring(lastSlashIdx + 1, lastDotIdx);
			}
			else {
				// no dot
				return path.substring(lastSlashIdx + 1);
			}
		}
		else {
			// no slash, no dot
			return path;
		}
	}

	private String stripPrefix(String qualifiedName) {
		if (qualifiedName == null) {
			return null;
		}

		String[] prefixAndName = qualifiedName.split(":");
		if (prefixAndName.length == 2) {
			return prefixAndName[1];
		}
		else {
			return null;
		}
	}

	protected void applyDataStoreConfig() {
		if (dataStore != null && dataStore.getParameters() != null) {
			DataStore targetDS = mappingWrapper.getDefaultDataStore();

			List<Parameter> inputParameters = dataStore.getParameters().getParameter();
			List<Parameter> targetParameters = targetDS.getParameters().getParameter();
			// update destination parameters
			for (Parameter inputParam : inputParameters) {
				boolean updated = false;
				for (Parameter targetParam : targetParameters) {
					if (inputParam.getName().equals(targetParam.getName())) {
						targetParam.setValue(inputParam.getValue());
						updated = true;
						break;
					}
				}

				if (!updated) {
					// parameter was not already present: add it to the list
					targetParameters.add(inputParam);
				}
			}
		}
	}

	protected void createNamespaces() {
		Collection<? extends Cell> typeCells = alignment.getTypeCells();
		for (Cell typeCell : typeCells) {
			ListMultimap<String, ? extends Entity> targetEntities = typeCell.getTarget();
			if (targetEntities != null) {
				for (Entity entity : targetEntities.values()) {
					createNamespaceForEntity(entity);
				}
			}

			Collection<? extends Cell> propertyCells = alignment.getPropertyCells(typeCell);
			for (Cell propCell : propertyCells) {
				Collection<? extends Entity> targetProperties = propCell.getTarget().values();
				if (targetProperties != null) {
					for (Entity property : targetProperties) {
						createNamespaceForEntity(property);
					}
				}
			}
		}
	}

	private void createNamespaceForEntity(Entity entity) {
		QName typeName = entity.getDefinition().getType().getName();
		String namespaceURI = typeName.getNamespaceURI();
		String prefix = typeName.getPrefix();

		context.getOrCreateNamespace(namespaceURI, prefix);

		List<ChildContext> propertyPath = entity.getDefinition().getPropertyPath();
		createNamespacesForPath(propertyPath);
	}

	private void createNamespacesForPath(List<ChildContext> propertyPath) {
		if (propertyPath != null) {
			for (ChildContext childContext : propertyPath) {
				PropertyDefinition child = childContext.getChild().asProperty();
				if (child != null) {
					// if provided prefix is blank, try to infer prefix from
					// parent type
					QName inferedQName = tryInferNamespacePrefix(child);
					context.getOrCreateNamespace(inferedQName.getNamespaceURI(),
							inferedQName.getPrefix());
				}
			}
		}
	}

	static QName tryInferNamespacePrefix(PropertyDefinition child) {
		String namespaceURI = child.getName().getNamespaceURI();
		String prefix = child.getName().getPrefix();
		// if provided prefix is blank, try to infer prefix from
		// parent type
		if (StringUtils.isBlank(prefix) && child instanceof AbstractPropertyDecorator) {
			PropertyDefinition dpd = ((AbstractPropertyDecorator) child).getDecoratedProperty();
			if (dpd instanceof DefaultPropertyDefinition) {
				DefinitionGroup dg = ((DefaultPropertyDefinition) dpd).getDeclaringGroup();
				if (dg instanceof DefaultTypeDefinition) {
					DefaultTypeDefinition defaultType = (DefaultTypeDefinition) dg;
					if (defaultType.getName() != null && Objects.equals(namespaceURI,
							defaultType.getName().getNamespaceURI()))
						prefix = defaultType.getName().getPrefix();
				}
			}
		}
		return new QName(namespaceURI, "", prefix);
	}

	private boolean isIsolated(String namespaceUri) {
		boolean isIsolated = false;

		if (workspaceConf != null && workspaceConf.hasWorkspace(namespaceUri)) {
			return workspaceConf.getWorkspace(namespaceUri).isIsolated();
		}

		return isIsolated;
	}

	protected void createTargetTypes() {
		Iterable<? extends Schema> targetSchemas = targetSchemaSpace.getSchemas();
		if (targetSchemas != null) {
			for (Schema targetSchema : targetSchemas) {
				mappingWrapper.addSchemaURI(targetSchema.getLocation().toString());
			}
		}
	}

	protected void createTypeMappings(AppSchemaMappingContext context, IOReporter reporter) {
		Collection<? extends Cell> typeCells = alignment.getTypeCells();
		for (Cell typeCell : typeCells) {
			String typeTransformId = typeCell.getTransformationIdentifier();
			TypeTransformationHandler typeTransformHandler = null;

			try {
				typeTransformHandler = TypeTransformationHandlerFactory.getInstance()
						.createTypeTransformationHandler(typeTransformId);
				FeatureTypeMapping ftMapping = typeTransformHandler
						.handleTypeTransformation(typeCell, context);

				if (ftMapping != null) {
					Collection<? extends Cell> propertyCells = getPropertyCells(typeCell);
					for (Cell propertyCell : propertyCells) {
						String propertyTransformId = propertyCell.getTransformationIdentifier();
						PropertyTransformationHandler propertyTransformHandler = null;

						try {
							propertyTransformHandler = PropertyTransformationHandlerFactory
									.getInstance()
									.createPropertyTransformationHandler(propertyTransformId);
							propertyTransformHandler.handlePropertyTransformation(typeCell,
									propertyCell, context);
						} catch (UnsupportedTransformationException e) {
							String errMsg = MessageFormat.format(
									"Error processing property cell {0}", propertyCell.getId());
							log.warn(errMsg, e);
							if (reporter != null) {
								reporter.warn(new IOMessageImpl(errMsg, e));
							}
						}
					}
				}
			} catch (UnsupportedTransformationException e) {
				String errMsg = MessageFormat.format("Error processing type cell{0}",
						typeCell.getId());
				log.warn(errMsg, e);
				if (reporter != null) {
					reporter.warn(new IOMessageImpl(errMsg, e));
				}
			}
		}
	}

	private Collection<? extends Cell> getPropertyCells(Cell typeCell) {
		// get property cells from alignment
		Collection<? extends Cell> propertyCells = alignment.getPropertyCells(typeCell);
		// now check if there are property cells for Nested ReferenceType
		// chainingConf.
		Collection<? extends Cell> extraCells = alignment.getCells().stream().filter(c -> {
			Optional<EntityDefinition> definitionOpt = c.getTarget().values().stream().findFirst()
					.map(e -> e.getDefinition());
			Optional<? extends Entity> typeCellEntityOpt = typeCell.getTarget().values().stream()
					.findFirst();
			Optional<QName> typeCellQnameOpt = typeCellEntityOpt.map(x -> x.getDefinition())
					.map(x -> x.getType()).map(x -> x.getName());
			if (!definitionOpt.isPresent() || !typeCellEntityOpt.isPresent())
				return false;
			return isHrefClientPropertyCompatible(c)
					&& definitionOpt.map(x -> x.getLastPathElement()).map(x -> x.getChild())
							.filter(x -> isHref(x)).isPresent()
					&& definitionOpt.map(x -> x.getType()).map(x -> x.getName())
							.filter(x -> Objects.equals(x, typeCellQnameOpt.get())).isPresent();
		}).collect(Collectors.toList());
		List<Cell> resultList = new ArrayList<>(propertyCells);
		resultList.addAll(extraCells);
		return resultList;
	}

	private boolean isHref(ChildDefinition<?> childDefinition) {
		if (childDefinition.getName() == null)
			return false;
		return Objects.equals(childDefinition.getName().getLocalPart(), "href") && Objects.equals(
				"http://www.w3.org/1999/xlink", childDefinition.getName().getNamespaceURI());
	}

	protected AppSchemaDataAccessType loadMappingTemplate() throws IOException {
		InputStream is = getClass().getResourceAsStream(AppSchemaIO.MAPPING_TEMPLATE);

		JAXBElement<AppSchemaDataAccessType> templateElement = null;
		try {
			JAXBContext context = createJaxbContext();
			Unmarshaller unmarshaller = context.createUnmarshaller();

			templateElement = unmarshaller.unmarshal(new StreamSource(is),
					AppSchemaDataAccessType.class);
		} catch (JAXBException e) {
			throw new IOException(e);
		}

		return templateElement.getValue();
	}

	/**
	 * @see it.geosolutions.hale.io.appschema.writer.MappingGenerator#writeMappingConf(java.io.OutputStream)
	 */
	@Override
	public void writeMappingConf(OutputStream out) throws IOException {
		checkMappingGenerated();

		try {
			writeMapping(out, mainMapping);
		} catch (JAXBException e) {
			throw new IOException(e);
		}

	}

	/**
	 * @see it.geosolutions.hale.io.appschema.writer.MappingGenerator#writeIncludedTypesMappingConf(java.io.OutputStream)
	 */
	@Override
	public void writeIncludedTypesMappingConf(OutputStream out) throws IOException {
		checkMappingGenerated();

		if (!mappingWrapper.requiresMultipleFiles()) {
			throw new IllegalStateException(
					"No included types configuration is available for the generated mapping");
		}

		try {
			writeMapping(out, includedTypesMapping);
		} catch (JAXBException e) {
			throw new IOException(e);
		}

	}

	static void writeMapping(OutputStream out, AppSchemaDataAccessType mapping)
			throws JAXBException {
		JAXBContext context = createJaxbContext();

		Marshaller marshaller = context.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

		JAXBElement<AppSchemaDataAccessType> mappingConfElement = new ObjectFactory()
				.createAppSchemaDataAccess(mapping);

		marshaller.marshal(mappingConfElement, out);
	}

	private static JAXBContext createJaxbContext() throws JAXBException {
		JAXBContext context = JAXBContext
				.newInstance(NET_OPENGIS_OGC_CONTEXT + ":" + APP_SCHEMA_CONTEXT);

		return context;
	}
}
