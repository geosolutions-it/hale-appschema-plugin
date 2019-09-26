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

package it.geosolutions.hale.io.appschema.writer;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ListMultimap;

import de.fhg.igd.slf4jplus.ALogger;
import de.fhg.igd.slf4jplus.ALoggerFactory;
import eu.esdihumboldt.hale.common.align.model.Alignment;
import eu.esdihumboldt.hale.common.align.model.Cell;
import eu.esdihumboldt.hale.common.align.model.Entity;
import eu.esdihumboldt.hale.common.align.model.functions.RenameFunction;
import eu.esdihumboldt.hale.common.align.model.impl.DefaultType;
import eu.esdihumboldt.hale.common.core.io.report.IOReporter;
import eu.esdihumboldt.hale.common.core.io.report.impl.IOMessageImpl;
import eu.esdihumboldt.hale.common.schema.model.SchemaSpace;
import eu.esdihumboldt.hale.common.schema.model.TypeDefinition;
import eu.esdihumboldt.hale.io.mongo.CollectionLinkFunction;
import eu.esdihumboldt.hale.io.mongo.JsonPathConstraint;
import it.geosolutions.hale.io.appschema.impl.internal.generated.app_schema.AppSchemaDataAccessType;
import it.geosolutions.hale.io.appschema.impl.internal.generated.app_schema.AttributeExpressionMappingType;
import it.geosolutions.hale.io.appschema.impl.internal.generated.app_schema.AttributeMappingType;
import it.geosolutions.hale.io.appschema.impl.internal.generated.app_schema.SourceDataStoresPropertyType.DataStore;
import it.geosolutions.hale.io.appschema.impl.internal.generated.app_schema.TypeMappingsPropertyType.FeatureTypeMapping;
import it.geosolutions.hale.io.appschema.model.FeatureChaining;
import it.geosolutions.hale.io.appschema.model.WorkspaceConfiguration;
import it.geosolutions.hale.io.appschema.mongodb.CollectionLinkHandler;
import it.geosolutions.hale.io.appschema.mongodb.MongoDBPropertyTransformationHandlerCommons;
import it.geosolutions.hale.io.appschema.mongodb.MongoRenameHandler;
import it.geosolutions.hale.io.appschema.mongodb.MongoTypeTransformationHandlerFactory;
import it.geosolutions.hale.io.appschema.mongodb.Utils;
import it.geosolutions.hale.io.appschema.writer.internal.AbstractPropertyTransformationHandler;
import it.geosolutions.hale.io.appschema.writer.internal.PropertyTransformationHandler;
import it.geosolutions.hale.io.appschema.writer.internal.PropertyTransformationHandlerFactory;
import it.geosolutions.hale.io.appschema.writer.internal.TypeTransformationHandler;
import it.geosolutions.hale.io.appschema.writer.internal.UnsupportedTransformationException;
import it.geosolutions.hale.io.appschema.writer.internal.mapping.AppSchemaMappingContext;
import it.geosolutions.hale.io.appschema.writer.internal.mapping.MappingWrapper;
import it.geosolutions.hale.io.appschema.writer.internal.mapping.MongoAppSchemaMappingWrapper;
import it.geosolutions.hale.io.geoserver.AppSchemaDataStore;
import it.geosolutions.hale.io.geoserver.ResourceBuilder;
import it.geosolutions.hale.io.geoserver.Workspace;

/**
 * MongoDB implementation of MappingGenerator.
 */
public class MongoMappingGenerator extends AppSchemaMappingGenerator {

	private static final ALogger log = ALoggerFactory.getLogger(MongoMappingGenerator.class);

	/**
	 * Constructor.
	 * 
	 * @param alignment the alignment to translate
	 * @param targetSchemaSpace the target schema space
	 * @param dataStore the DataStore configuration to use
	 * @param chainingConf the feature chaining configuration
	 * @param workspaceConf the workspace configuration
	 */
	public MongoMappingGenerator(Alignment alignment, SchemaSpace targetSchemaSpace,
			DataStore dataStore, FeatureChaining chainingConf,
			WorkspaceConfiguration workspaceConf) {
		super(alignment, targetSchemaSpace, dataStore, chainingConf, workspaceConf);
	}

	/**
	 * @see it.geosolutions.hale.io.appschema.writer.AppSchemaMappingGenerator#generateMapping(eu.esdihumboldt.hale.common.core.io.report.IOReporter)
	 */
	@Override
	public MappingWrapper generateMapping(IOReporter reporter) throws IOException {
		// reset wrapper
		resetMappingState();

		try {
			AppSchemaDataAccessType mapping = loadMappingTemplate();

			// get mapping prefix
			String mappingPrefix = null;
			try {

				ListMultimap<String, ? extends Entity> s = alignment.getTypeCells().iterator()
						.next().getSource();
				DefaultType t = (DefaultType) s.values().iterator().next();
				mappingPrefix = t.getDefinition().getType().getConstraint(JsonPathConstraint.class)
						.getRootKey();

			} catch (Exception e) {
				throw e;
			}

			mappingWrapper = new MongoAppSchemaMappingWrapper(mapping);
			context = new AppSchemaMappingContext(mappingWrapper, alignment,
					targetSchema.getMappingRelevantTypes(), chainingConf, workspaceConf);
			mappingWrapper.setMappingPrefix(mappingPrefix);

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

	/**
	 * @see it.geosolutions.hale.io.appschema.writer.AppSchemaMappingGenerator#getAppSchemaDataStore()
	 */
	@Override
	public it.geosolutions.hale.io.geoserver.DataStore getAppSchemaDataStore() {
		checkMappingGenerated();
		checkTargetSchemaAvailable();

		it.geosolutions.hale.io.geoserver.Namespace ns = getMainNamespace();
		Workspace ws = getMainWorkspace();

		String workspaceId = (String) ws.getAttribute(Workspace.ID);
		String dataStoreName = extractSchemaName(targetSchema.getLocation());
		if (mainMapping.getTypeMappings().getFeatureTypeMapping().size() == 1) {
			dataStoreName = mainMapping.getTypeMappings().getFeatureTypeMapping().get(0)
					.getSourceType();
		}
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
	 * @see it.geosolutions.hale.io.appschema.writer.AppSchemaMappingGenerator#createTypeMappings(it.geosolutions.hale.io.appschema.writer.internal.mapping.AppSchemaMappingContext,
	 *      eu.esdihumboldt.hale.common.core.io.report.IOReporter)
	 */
	@Override
	protected void createTypeMappings(AppSchemaMappingContext context, IOReporter reporter) {
		Collection<? extends Cell> typeCells = alignment.getTypeCells();
		for (Cell typeCell : typeCells) {
			handleTypeCell(context, typeCell, reporter);
		}
	}

	private void handleTypeCell(AppSchemaMappingContext context, Cell typeCell,
			IOReporter reporter) {

		// check if need to do a recursive mapping where possible
		// if (Utils.recursiveMapping(typeCell)) {
		// add mappings for properties that have the same name
		// Property source = Utils.getFirstEntity(typeCell.getSource(),
		// Utils::convertToProperty);
		// Property target = Utils.getFirstEntity(typeCell.getTarget(),
		// Utils::convertToProperty);
		// Cell cell = new DefaultCell();
		// }

		String typeTransformId = typeCell.getTransformationIdentifier();
		TypeTransformationHandler typeTransformHandler = null;

		try {
			typeTransformHandler = (new MongoTypeTransformationHandlerFactory())
					.createTypeTransformationHandler(typeTransformId);
			FeatureTypeMapping ftMapping = typeTransformHandler.handleTypeTransformation(typeCell,
					context);

			// add randomID for MongoDB types
			TypeDefinition sourceType = Utils
					.getFirstEntity(typeCell.getSource(), (entity) -> entity).getDefinition()
					.getType();
			TypeDefinition targetType = Utils
					.getFirstEntity(typeCell.getTarget(), (entity) -> entity).getDefinition()
					.getType();
			JsonPathConstraint jsonConstraint = sourceType.getConstraint(JsonPathConstraint.class);

			if (!jsonConstraint.isValid()) {
				// add collection id to the container
				AttributeMappingType attributeMapping = mappingWrapper
						.getOrCreateAttributeMapping(targetType, null, null);
				attributeMapping.setTargetAttribute(ftMapping.getTargetElement());
				// set id expression
				AttributeExpressionMappingType idExpression = new AttributeExpressionMappingType();
				idExpression.setOCQL("collectionId()");
				attributeMapping.setIdExpression(idExpression);
			}

			if (ftMapping != null) {
				Collection<? extends Cell> propertyCells = alignment.getPropertyCells(typeCell);

				for (Cell propertyCell : propertyCells) {
					String propertyTransformId = propertyCell.getTransformationIdentifier();
					try {
						if (propertyTransformId.equals(CollectionLinkFunction.ID)) {
							// handle MongoDB collection linking case
							CollectionLinkHandler handler = new CollectionLinkHandler();
							handler.handleTypeTransformation(propertyCell, context);
						}
						else {
							// handle other properties
							PropertyTransformationHandler propertyTransformHandler = getPropertyTransformationHandlerFactory(
									propertyTransformId);
							propertyTransformHandler.handlePropertyTransformation(typeCell,
									propertyCell, context);
						}
					} catch (UnsupportedTransformationException e) {
						String errMsg = MessageFormat.format("Error processing property cell {0}",
								propertyCell.getId());
						log.warn(errMsg, e);
						if (reporter != null) {
							reporter.warn(new IOMessageImpl(errMsg, e));
						}
					}
				}
			}
		} catch (UnsupportedTransformationException e) {
			String errMsg = MessageFormat.format("Error processing type cell{0}", typeCell.getId());
			log.warn(errMsg, e);
			if (reporter != null) {
				reporter.warn(new IOMessageImpl(errMsg, e));
			}
		}
	}

	private PropertyTransformationHandler getPropertyTransformationHandlerFactory(
			String propertyTransformId) throws UnsupportedTransformationException {
		PropertyTransformationHandler propertyTransformHandler;
		if (propertyTransformId.equals(RenameFunction.ID)) {
			propertyTransformHandler = new MongoRenameHandler();
		}
		else {
			propertyTransformHandler = PropertyTransformationHandlerFactory.getInstance()
					.createPropertyTransformationHandler(propertyTransformId);
		}
		if (propertyTransformHandler instanceof AbstractPropertyTransformationHandler) {
			((AbstractPropertyTransformationHandler) propertyTransformHandler)
					.setHandlerCommons(new MongoDBPropertyTransformationHandlerCommons());
		}
		return propertyTransformHandler;
	}
}
