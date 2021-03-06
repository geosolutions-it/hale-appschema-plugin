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

package it.geosolutions.hale.io.appschema.writer.internal;

import static it.geosolutions.hale.io.appschema.AppSchemaIO.isHrefClientPropertyCompatible;
import static it.geosolutions.hale.io.appschema.writer.AppSchemaMappingUtils.QNAME_XSI_NIL;
import static it.geosolutions.hale.io.appschema.writer.AppSchemaMappingUtils.XSI_PREFIX;
import static it.geosolutions.hale.io.appschema.writer.AppSchemaMappingUtils.XSI_URI;
import static it.geosolutions.hale.io.appschema.writer.AppSchemaMappingUtils.findOwningType;
import static it.geosolutions.hale.io.appschema.writer.AppSchemaMappingUtils.getGeometryPropertyEntity;
import static it.geosolutions.hale.io.appschema.writer.AppSchemaMappingUtils.getTargetProperty;
import static it.geosolutions.hale.io.appschema.writer.AppSchemaMappingUtils.getTargetType;
import static it.geosolutions.hale.io.appschema.writer.AppSchemaMappingUtils.isGeometryType;
import static it.geosolutions.hale.io.appschema.writer.AppSchemaMappingUtils.isGmlId;
import static it.geosolutions.hale.io.appschema.writer.AppSchemaMappingUtils.isNested;
import static it.geosolutions.hale.io.appschema.writer.AppSchemaMappingUtils.isNilReason;
import static it.geosolutions.hale.io.appschema.writer.AppSchemaMappingUtils.isNillable;
import static it.geosolutions.hale.io.appschema.writer.AppSchemaMappingUtils.isXmlAttribute;
import static java.util.Optional.ofNullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.xml.namespace.QName;

import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Literal;

import com.google.common.base.Strings;

import de.fhg.igd.slf4jplus.ALogger;
import de.fhg.igd.slf4jplus.ALoggerFactory;
import eu.esdihumboldt.hale.common.align.model.AlignmentUtil;
import eu.esdihumboldt.hale.common.align.model.Cell;
import eu.esdihumboldt.hale.common.align.model.ChildContext;
import eu.esdihumboldt.hale.common.align.model.Condition;
import eu.esdihumboldt.hale.common.align.model.Entity;
import eu.esdihumboldt.hale.common.align.model.EntityDefinition;
import eu.esdihumboldt.hale.common.align.model.ParameterValue;
import eu.esdihumboldt.hale.common.align.model.Property;
import eu.esdihumboldt.hale.common.align.model.functions.join.JoinParameter;
import eu.esdihumboldt.hale.common.align.model.functions.join.JoinParameter.JoinCondition;
import eu.esdihumboldt.hale.common.align.model.impl.PropertyEntityDefinition;
import eu.esdihumboldt.hale.common.schema.model.ChildDefinition;
import eu.esdihumboldt.hale.common.schema.model.Definition;
import eu.esdihumboldt.hale.common.schema.model.GroupPropertyDefinition;
import eu.esdihumboldt.hale.common.schema.model.PropertyDefinition;
import eu.esdihumboldt.hale.common.schema.model.TypeDefinition;
import eu.esdihumboldt.hale.common.schema.model.constraint.property.Cardinality;
import eu.esdihumboldt.hale.io.xsd.constraint.XmlAttributeFlag;
import eu.esdihumboldt.hale.io.xsd.reader.internal.AnonymousXmlType;
import it.geosolutions.hale.io.appschema.AppSchemaIO;
import it.geosolutions.hale.io.appschema.impl.internal.generated.app_schema.AnonymousAttributeType;
import it.geosolutions.hale.io.appschema.impl.internal.generated.app_schema.AttributeExpressionMappingType;
import it.geosolutions.hale.io.appschema.impl.internal.generated.app_schema.AttributeMappingType;
import it.geosolutions.hale.io.appschema.impl.internal.generated.app_schema.AttributeMappingType.ClientProperty;
import it.geosolutions.hale.io.appschema.impl.internal.generated.app_schema.JdbcMultiValueType;
import it.geosolutions.hale.io.appschema.impl.internal.generated.app_schema.NamespacesPropertyType.Namespace;
import it.geosolutions.hale.io.appschema.impl.internal.generated.app_schema.TypeMappingsPropertyType.FeatureTypeMapping;
import it.geosolutions.hale.io.appschema.model.ChainConfiguration;
import it.geosolutions.hale.io.appschema.model.FeatureChaining;
import it.geosolutions.hale.io.appschema.writer.AppSchemaMappingUtils;
import it.geosolutions.hale.io.appschema.writer.internal.mapping.AppSchemaMappingContext;
import it.geosolutions.hale.io.appschema.writer.internal.mapping.MappingWrapper;

/**
 * Base class for property transformation handlers.
 * 
 * @author Stefano Costa, GeoSolutions
 */
public abstract class AbstractPropertyTransformationHandler
		implements PropertyTransformationHandler {

	private static final ALogger LOG = ALoggerFactory
			.getLogger(AbstractPropertyTransformationHandler.class);

	/**
	 * The app-schema mapping configuration under construction.
	 */
	protected MappingWrapper mapping;
	/**
	 * The type cell owning the property cell to handle.
	 */
	protected Cell typeCell;
	/**
	 * The property cell to handle.
	 */
	protected Cell propertyCell;
	/**
	 * The target property.
	 */
	protected Property targetProperty;

	/**
	 * The feature type mapping which is the parent of the attribute mapping
	 * generated by this handler.
	 */
	protected FeatureTypeMapping featureTypeMapping;
	/**
	 * The attribute mapping generated by this handler.
	 */
	protected AttributeMappingType attributeMapping;

	private HandlerCommons handlerCommons = getDefaultHandlerCommons();

	/**
	 * @see it.geosolutions.hale.io.appschema.writer.internal.PropertyTransformationHandler#handlePropertyTransformation(eu.esdihumboldt.hale.common.align.model.Cell,
	 *      eu.esdihumboldt.hale.common.align.model.Cell,
	 *      it.geosolutions.hale.io.appschema.writer.internal.mapping.AppSchemaMappingContext)
	 */
	@Override
	public AttributeMappingType handlePropertyTransformation(Cell typeCell, Cell propertyCell,
			AppSchemaMappingContext context) {
		this.mapping = context.getMappingWrapper();
		this.typeCell = typeCell;
		this.propertyCell = propertyCell;
		// TODO: does this hold for any transformation function?
		this.targetProperty = getTargetProperty(propertyCell);
		LOG.debug(
				"Starting property transformation for: \n type cell = {} \n property cell = {} \n target property = {}",
				this.typeCell, this.propertyCell, this.targetProperty);
		PropertyEntityDefinition targetPropertyEntityDef = targetProperty.getDefinition();
		PropertyDefinition targetPropertyDef = targetPropertyEntityDef.getDefinition();

		TypeDefinition featureType = null;
		String mappingName = null;
		if (AppSchemaMappingUtils.isJoin(typeCell)) {
			if (context.getFeatureChaining() != null) {
				ChainConfiguration chainConf = findChainConfiguration(context);
				LOG.debug("Chain configuration found: {0}", chainConf);
				if (chainConf != null) {
					// check if is a ReferenceType with a linked proper type
					featureType = chainConf.getReferenceLinkedType() == null
							? chainConf.getNestedTypeTargetType()
							: chainConf.getReferenceLinkedType();
					mappingName = chainConf.getMappingName();
				}
			}
			else {
				// this is just a best effort attempt to determine the target
				// feature type, may result in incorrect mappings
				featureType = findOwningType(targetPropertyEntityDef,
						context.getRelevantTargetTypes());
			}
		}
		if (featureType == null) {
			LOG.debug("feature type not found from join setup");
			featureType = getTargetType(typeCell).getDefinition().getType();
		}
		LOG.debug("feature type = {}", featureType);

		// double check: don't map properties that belong to a feature
		// chaining configuration other than the current one
		if (context.getFeatureChaining() != null) {
			for (String joinId : context.getFeatureChaining().getJoins().keySet()) {
				List<ChainConfiguration> chains = context.getFeatureChaining().getChains(joinId);
				ChainConfiguration chainConf = findLongestNestedPath(
						targetPropertyEntityDef.getPropertyPath(), chains);
				if (chainConf != null && !isHrefLinkedAttribute(propertyCell, featureType)
						&& !chainConf.getNestedTypeTargetType().equals(featureType)) {
					// don't translate mapping, will do it (or have done it)
					// elsewhere!
					featureType = null;
					if (LOG.isDebugEnabled())
						LOG.debug("Don't map properties that belong to a feature chaining "
								+ "configuration other than the current one. \n" + "chainConf = {}",
								chainConf);
					break;
				}
			}
		}

		if (featureType != null) {
			// fetch FeatureTypeMapping from mapping configuration
			if (isValidFeatureType(featureType))
				this.featureTypeMapping = context.getOrCreateFeatureTypeMapping(featureType,
						mappingName);

			// TODO: verify source property (if any) belongs to mapped source
			// type

			// fetch AttributeMappingType from mapping
			if (isXmlAttribute(targetPropertyDef)) {
				// gml:id attribute requires special handling, i.e. an
				// <idExpression> tag must be added to the attribute mapping for
				// target feature types and geometry types
				TypeDefinition parentType = targetPropertyDef.getParentType();
				if (isGmlId(targetPropertyDef)) {
					// TODO: handle gml:id for geometry types
					if (featureType.equals(parentType)) {
						handleAsFeatureGmlId(featureType, mappingName, context);
					}
					else if (isGeometryType(parentType)) {
						handleAsGeometryGmlId(featureType, mappingName, context);
					}
					else {
						handleAsXmlAttribute(featureType, mappingName, context);
					}
				}
				else {
					handleAsXmlAttribute(featureType, mappingName, context);
				}
			}
			else {
				handleAsXmlElement(featureType, mappingName, context);
			}
		}

		return attributeMapping;
	}

	private boolean isHrefLinkedAttribute(Cell propertyCell, TypeDefinition featureType) {
		Optional<EntityDefinition> definitionOpt = propertyCell.getTarget().values().stream()
				.findFirst().map(x -> x.getDefinition());
		final TypeDefinition ftypeFinal = featureType;
		boolean isSameParentType = definitionOpt.map(x -> x.getType()).map(x -> x.getName())
				.filter(x -> x.equals(ftypeFinal.getName())).isPresent();
		Optional<QName> childName = definitionOpt.map(x -> x.getLastPathElement())
				.map(x -> x.getChild()).map(x -> x.getName());
		boolean isHrefAttribute = childName.filter(x -> "href".equals(x.getLocalPart()))
				.isPresent();
		return isHrefClientPropertyCompatible(propertyCell) && isHrefAttribute && isSameParentType;
	}

	static boolean isValidFeatureType(TypeDefinition featureType) {
		return !AppSchemaIO.isReferenceType(featureType)
				&& !AppSchemaIO.isUnboundedSequence(featureType)
				&& !(featureType instanceof AnonymousXmlType);
	}

	/**
	 * @param context the mapping context
	 * @return the chain configuration that applies to the current property
	 *         mapping
	 */
	private ChainConfiguration findChainConfiguration(AppSchemaMappingContext context) {
		ChainConfiguration chainConf = null;

		PropertyEntityDefinition targetPropertyEntityDef = targetProperty.getDefinition();
		FeatureChaining featureChaining = context.getFeatureChaining();
		if (featureChaining != null) {
			List<ChildContext> targetPropertyPath = targetPropertyEntityDef.getPropertyPath();
			List<ChainConfiguration> chains = featureChaining.getChains(typeCell.getId());
			chainConf = findLongestNestedPath(targetPropertyPath, chains);
		}

		return chainConf;
	}

	private ChainConfiguration findLongestNestedPath(List<ChildContext> targetPropertyPath,
			List<ChainConfiguration> chains) {
		ChainConfiguration chainConf = null;

		if (chains != null && chains.size() > 0 && targetPropertyPath != null
				&& targetPropertyPath.size() > 0) {
			int maxPathLength = 0;
			for (ChainConfiguration chain : chains) {
				List<ChildContext> nestedTargetPath = chain.getNestedTypeTarget().getPropertyPath();

				boolean isNested = isNested(nestedTargetPath, targetPropertyPath);

				if (isNested && maxPathLength < nestedTargetPath.size()) {
					maxPathLength = nestedTargetPath.size();
					chainConf = chain;
				}
			}
		}

		return chainConf;
	}

	/**
	 * This method is invoked when the target type is the feature type owning
	 * this attribute mapping, and the target property is <code>gml:id</code>,
	 * which needs special handling.
	 * 
	 * <p>
	 * In practice, this means that <code>&lt;idExpression&gt;</code> is used in
	 * place of:
	 * 
	 * <pre>
	 *   &lt;ClientProperty&gt;
	 *     &lt;name&gt;...&lt;/name&gt;
	 *     &lt;value&gt;...&lt;/value&gt;
	 *   &lt;/ClientProperty&gt;
	 * </pre>
	 * 
	 * and that the target attribute is set to the mapped feature type name.
	 * 
	 * </p>
	 * 
	 * @param featureType the target feature type
	 * @param mappingName the target feature type's mapping name (may be
	 *            <code>null</code>)
	 * @param context the app-schema mapping context
	 */
	protected void handleAsFeatureGmlId(TypeDefinition featureType, String mappingName,
			AppSchemaMappingContext context) {
		PropertyEntityDefinition targetPropertyEntityDef = targetProperty.getDefinition();
		List<ChildContext> gmlIdPath = targetPropertyEntityDef.getPropertyPath();

		attributeMapping = context.getOrCreateAttributeMapping(featureType, mappingName, gmlIdPath);
		// set targetAttribute to feature type qualified name
		attributeMapping.setTargetAttribute(featureTypeMapping.getTargetElement());
		// set id expression
		AttributeExpressionMappingType idExpression = new AttributeExpressionMappingType();
		idExpression.setOCQL(getSourceExpressionAsCQL());
		// TODO: not sure whether any CQL expression can be used here
		attributeMapping.setIdExpression(idExpression);
	}

	/**
	 * This method is invoked when the target property's parent is a geometry
	 * and the target property is <code>gml:id</code> (which needs special
	 * handling).
	 * 
	 * <p>
	 * In practice, this means that <code>&lt;idExpression&gt;</code> is used in
	 * place of:
	 * 
	 * <pre>
	 *   &lt;ClientProperty&gt;
	 *     &lt;name&gt;...&lt;/name&gt;
	 *     &lt;value&gt;...&lt;/value&gt;
	 *   &lt;/ClientProperty&gt;
	 * </pre>
	 * 
	 * @param featureType the target feature type
	 * @param mappingName the target feature type's mapping name (may be
	 *            <code>null</code>)
	 * @param context the app-schema mapping context
	 */
	protected void handleAsGeometryGmlId(TypeDefinition featureType, String mappingName,
			AppSchemaMappingContext context) {
		PropertyEntityDefinition targetPropertyEntityDef = targetProperty.getDefinition();
		PropertyEntityDefinition geometry = (PropertyEntityDefinition) AlignmentUtil
				.getParent(targetPropertyEntityDef);

		createGeometryAttributeMapping(featureType, mappingName, geometry, context);

		// set id expression
		AttributeExpressionMappingType idExpression = new AttributeExpressionMappingType();
		idExpression.setOCQL(getSourceExpressionAsCQL());
		// TODO: not sure whether any CQL expression can be used here
		attributeMapping.setIdExpression(idExpression);
	}

	/**
	 * This method is invoked when the target property is an XML attribute (
	 * {@link XmlAttributeFlag} constraint is set).
	 * 
	 * <p>
	 * The property transformation is translated to:
	 * 
	 * <pre>
	 *   <code>&lt;ClientProperty&gt;
	 *     &lt;name&gt;[target property name]&lt;/name&gt;
	 *     &lt;value&gt;[CQL expression]&lt;/value&gt;
	 *   &lt;/ClientProperty&gt;</code>
	 * </pre>
	 * 
	 * and added to the attribute mapping generated for the XML element owning
	 * the attribute.
	 * </p>
	 * 
	 * @param featureType the target feature type
	 * @param mappingName the target feature type's mapping name (may be
	 *            <code>null</code>)
	 * @param context the app-schema mapping context
	 */
	protected void handleAsXmlAttribute(TypeDefinition featureType, String mappingName,
			AppSchemaMappingContext context) {
		LOG.debug("Handling as xml sttribute: \n fetureType = {} \n mappingName = {}", featureType,
				mappingName);
		// if it's an anonymous type
		if (featureType instanceof AnonymousXmlType) {
			featureType = propertyCell.getTarget().values().stream().map(Entity::getDefinition)
					.map(EntityDefinition::getType).findFirst()
					.orElseThrow(() -> new IllegalArgumentException("No target type available"));
		}

		PropertyEntityDefinition targetPropertyEntityDef = targetProperty.getDefinition();
		PropertyDefinition targetPropertyDef = targetPropertyEntityDef.getDefinition();

		// fetch attribute mapping for parent property
		EntityDefinition parentDef = AlignmentUtil.getParent(targetPropertyEntityDef);
		if (parentDef != null) {
			List<ChildContext> parentPropertyPath = parentDef.getPropertyPath();
			PropertyDefinition parentPropertyDef = getParentPropertyDef(parentPropertyPath);
			if (parentPropertyDef != null) {
				attributeMapping = context.getOrCreateAttributeMapping(featureType, mappingName,
						parentPropertyPath);
				// set targetAttribute if empty
				if (attributeMapping.getTargetAttribute() == null
						|| attributeMapping.getTargetAttribute().isEmpty()) {
					attributeMapping.setTargetAttribute(
							mapping.buildAttributeXPath(featureType, parentPropertyPath));
				}

				Namespace targetPropNS = getTargetPropertyNamespace(targetPropertyDef, context,
						mapping);
				String unqualifiedName = targetPropertyDef.getName().getLocalPart();
				boolean isQualified = targetPropNS != null
						&& !Strings.isNullOrEmpty(targetPropNS.getPrefix());

				// encode attribute as <ClientProperty>
				ClientProperty clientProperty = new ClientProperty();
				@SuppressWarnings("null")
				String clientPropName = (isQualified)
						? targetPropNS.getPrefix() + ":" + unqualifiedName
						: unqualifiedName;
				clientProperty.setName(clientPropName);
				clientProperty.setValue(getSourceExpressionAsCQL());
				setEncodeIfEmpty(clientProperty);

				// don't add client property if it already exists
				if (!hasClientProperty(clientProperty.getName())) {
					attributeMapping.getClientProperty().add(clientProperty);

					// if mapping nilReason, parent property is nillable and no
					// sourceExpression has been set yet, add xsi:nil attribute
					// following the same encoding logic of nilReason (i.e. null
					// when nilReason is null and viceversa)
					if (isNilReason(targetPropertyDef) && isNillable(parentPropertyDef)
							&& attributeMapping.getSourceExpression() == null) {
						addOrReplaceXsiNilAttribute(clientProperty.getValue(), true, context);
					}
				}
			}
		}
	}

	private Namespace getTargetPropertyNamespace(PropertyDefinition targetPropertyDef,
			AppSchemaMappingContext context, MappingWrapper mappingWrapper) {
		return handlerCommons.getTargetPropertyNamespace(targetPropertyDef, context,
				mappingWrapper);
	}

	private PropertyDefinition getParentPropertyDef(List<ChildContext> parentPropertyPath) {
		return handlerCommons.getParentPropertyDef(parentPropertyPath);
	}

	/**
	 * This method is invoked when the target property is a regular XML element.
	 * 
	 * @param featureType the target feature type
	 * @param mappingName the target feature type's mapping name (may be
	 *            <code>null</code>)
	 * @param context the app-schema mapping context
	 */
	protected void handleAsXmlElement(TypeDefinition featureType, String mappingName,
			AppSchemaMappingContext context) {
		LOG.debug("Handling as xml element: \n featureType = {}, \n mappingName = {}", featureType,
				mappingName);
		PropertyEntityDefinition targetPropertyEntityDef = targetProperty.getDefinition();
		PropertyDefinition targetPropertyDef = targetPropertyEntityDef.getDefinition();
		TypeDefinition targetPropertyType = targetPropertyDef.getPropertyType();

		if (isGeometryType(targetPropertyType)) {
			handleXmlElementAsGeometryType(featureType, mappingName, context);
		}
		else if (handlerCommons.isAnonymousType(featureType)) {
			attributeMapping = processAnonymousType(featureType, context);
			return;
		}
		else if (handlerCommons.isSequenceElement(featureType, typeCell, propertyCell,
				targetProperty)) {
			attributeMapping = processSequenceElement(featureType, context);
			return;
		}
		else {
			attributeMapping = context.getOrCreateAttributeMapping(featureType, mappingName,
					targetPropertyEntityDef.getPropertyPath());
			List<ChildContext> targetPropertyPath = targetPropertyEntityDef.getPropertyPath();
			// set target attribute
			attributeMapping.setTargetAttribute(
					mapping.buildAttributeXPath(featureType, targetPropertyPath));
		}

		// set source expression
		AttributeExpressionMappingType sourceExpression = new AttributeExpressionMappingType();
		// TODO: is this general enough?
		sourceExpression.setOCQL(getSourceExpressionAsCQL());
		attributeMapping.setSourceExpression(sourceExpression);
		if (AppSchemaMappingUtils.isMultiple(targetPropertyDef)) {
			attributeMapping.setIsMultiple(true);
		}
		// if element is nillable, add xsi:nil attribute with inverted logic
		// (i.e. null if source expression is NOT null, and viceversa)
		if (isNillable(targetPropertyDef)) {
			addOrReplaceXsiNilAttribute(attributeMapping.getSourceExpression().getOCQL(), false,
					context);
		}
		// TODO: isList?
		// TODO: targetAttributeNode?
		// TODO: encodeIfEmpty?
	}

	/**
	 * This method is invoked when the target property is a GML geometry type.
	 * 
	 * <p>
	 * The target attribute is set to <code>gml:AbstractGeometry</code> and the
	 * concrete geometry type is specified in a
	 * <code>&lt;targetAttributeNode&gt;</code> tag.
	 * </p>
	 * 
	 * @param featureType the target feature type
	 * @param mappingName the target feature type's mapping name (may be
	 *            <code>null</code>)
	 * @param context the app-schema mapping context
	 */
	protected void handleXmlElementAsGeometryType(TypeDefinition featureType, String mappingName,
			AppSchemaMappingContext context) {
		PropertyEntityDefinition geometry = targetProperty.getDefinition();

		createGeometryAttributeMapping(featureType, mappingName, geometry, context);

		// GeometryTypes require special handling
		TypeDefinition geometryType = geometry.getDefinition().getPropertyType();
		QName geomTypeName = geometryType.getName();
		Namespace geomNS = context.getOrCreateNamespace(geomTypeName.getNamespaceURI(),
				geomTypeName.getPrefix());
		attributeMapping
				.setTargetAttributeNode(geomNS.getPrefix() + ":" + geomTypeName.getLocalPart());

		// set target attribute to parent (should be gml:AbstractGeometry)
		// TODO: this is really ugly, but I don't see a better way to do it
		// since HALE renames
		// {http://www.opengis.net/gml/3.2}AbstractGeometry element
		// to
		// {http://www.opengis.net/gml/3.2/AbstractGeometry}choice
		EntityDefinition parentEntityDef = AlignmentUtil.getParent(geometry);
		Definition<?> parentDef = parentEntityDef.getDefinition();
		String parentQName = geomNS.getPrefix() + ":" + parentDef.getDisplayName();
		List<ChildContext> targetPropertyPath = parentEntityDef.getPropertyPath();
		attributeMapping.setTargetAttribute(
				mapping.buildAttributeXPath(featureType, targetPropertyPath) + "/" + parentQName);
	}

	/**
	 * Wraps the provided CQL expression in a conditional expression, based on
	 * the filter defined on the property.
	 * 
	 * <p>
	 * TODO: current implementation is broken, don't use it (first argument of
	 * if_then_else must be an expression, cannot be a filter (i.e. cannot
	 * contain '=' sign))!
	 * </p>
	 * 
	 * @param propertyEntityDef the property definition defining the condition
	 * @param cql the CQL expression to wrap
	 * @return a conditional expression wrapping the provided CQL expression
	 */
	protected static String getConditionalExpression(PropertyEntityDefinition propertyEntityDef,
			String cql) {
		if (propertyEntityDef != null) {
			String propertyName = propertyEntityDef.getDefinition().getName().getLocalPart();
			List<ChildContext> propertyPath = propertyEntityDef.getPropertyPath();
			// TODO: conditions are supported only on simple (not nested)
			// properties
			if (propertyPath.size() == 1) {
				Condition condition = propertyPath.get(0).getCondition();
				if (condition != null) {
					String fitlerText = AlignmentUtil.getFilterText(condition.getFilter());
					// remove "parent" references
					fitlerText = fitlerText.replace("parent.", "");
					// replace "value" references with the local name of the
					// property itself
					fitlerText = fitlerText.replace("value", propertyName);

					return String.format("if_then_else(%s, %s, Expression.NIL)", fitlerText, cql);
				}
			}
		}

		return cql;
	}

	/**
	 * Template method to be implemented by subclasses.
	 * 
	 * <p>
	 * This is where the translation logic should go. Basically, the propety
	 * transformation must be converted to a CQL expression producing the same
	 * result.
	 * </p>
	 * 
	 * @return a CQL expression producing the same result as the HALE
	 *         transformation
	 */
	protected abstract String getSourceExpressionAsCQL();

	private void createGeometryAttributeMapping(TypeDefinition featureType, String mappingName,
			PropertyEntityDefinition geometry, AppSchemaMappingContext context) {
		EntityDefinition geometryProperty = getGeometryPropertyEntity(geometry);

		// use geometry property path to create / retrieve attribute mapping
		attributeMapping = context.getOrCreateAttributeMapping(featureType, mappingName,
				geometryProperty.getPropertyPath());
	}

	/**
	 * If client property is set to a constant expression, add
	 * &lt;encodeIfEmpty&gt;true&lt;/encodeIfEmpty&gt; to the attribute mapping
	 * to make sure the element is encoded also if it has no value.
	 * 
	 * @param clientProperty the client property to test
	 */
	private void setEncodeIfEmpty(ClientProperty clientProperty) {
		try {
			Expression expr = CQL.toExpression(getSourceExpressionAsCQL());
			if (expr != null && expr instanceof Literal) {
				attributeMapping.setEncodeIfEmpty(true);
			}
		} catch (CQLException e) {
			LOG.warn("Skipping exception: Cannot set encodeIfEmpty value. Reason: "
					+ e.getMessage());
		}
	}

	/**
	 * Adds an {@code xsi:nil} client property to the attribute mapping, or
	 * updates the existing one.
	 * 
	 * <p>
	 * The CQL expression for the property's value is derived from the provided
	 * source expression in this way:
	 * 
	 * <ul>
	 * <li>{@code sameLogic=true} -->
	 * {@code if_then_else(isNull([sourceExpression]), Expression.NIL, 'true')}</li>
	 * <li>{@code sameLogic=false} -->
	 * {@code if_then_else(isNull([sourceExpression]), 'true', Expression.NIL)}</li>
	 * </ul>
	 * </p>
	 * 
	 * @param sourceExpression the source expression
	 * @param sameLogic if {@code true}, {@code xsi:nil} will be {@code null}
	 *            when {@code sourceExpression} is and {@code 'true'} when it
	 *            isn't, if {@code false} the opposite applies
	 * @param context the app-schema mapping context
	 */
	private void addOrReplaceXsiNilAttribute(String sourceExpression, boolean sameLogic,
			AppSchemaMappingContext context) {
		final String sameLogicPattern = "if_then_else(isNull(%s), Expression.NIL, 'true')";
		final String invertedLogicPattern = "if_then_else(isNull(%s), 'true', Expression.NIL)";
		final String pattern = sameLogic ? sameLogicPattern : invertedLogicPattern;

		// make sure xsi namespace is included in the mapping
		context.getOrCreateNamespace(XSI_URI, XSI_PREFIX);

		String xsiNilQName = QNAME_XSI_NIL.getPrefix() + ":" + QNAME_XSI_NIL.getLocalPart();

		ClientProperty xsiNil = getClientProperty(xsiNilQName);
		if (xsiNil == null) {
			xsiNil = new ClientProperty();
			// add xsi:nil attribute
			attributeMapping.getClientProperty().add(xsiNil);
		}
		xsiNil.setName(xsiNilQName);
		xsiNil.setValue(String.format(pattern, sourceExpression));
	}

	private boolean hasClientProperty(String name) {
		return getClientProperty(name) != null;
	}

	private ClientProperty getClientProperty(String name) {
		for (ClientProperty existentProperty : attributeMapping.getClientProperty()) {
			if (name.equals(existentProperty.getName())) {
				return existentProperty;
			}
		}
		return null;
	}

	/**
	 * @param handlerCommons the handlerCommons to set
	 */
	public void setHandlerCommons(HandlerCommons handlerCommons) {
		if (handlerCommons == null)
			throw new IllegalArgumentException("handlerCommons is not nullable");
		this.handlerCommons = handlerCommons;
	}

	private AttributeMappingType processSequenceElement(TypeDefinition typeDefinition,
			AppSchemaMappingContext context) {
		final PropertyEntityDefinition targetPropertyEntityDef = targetProperty.getDefinition();
		final QName clientPropertyName = ofNullable(targetPropertyEntityDef)
				.map(PropertyEntityDefinition::getDefinition).map(PropertyDefinition::getName)
				.orElseThrow(() -> new IllegalArgumentException("No target name available"));
		LOG.debug("Processing sequence element typeDefinition = {},\n clientPropertyName = {}",
				typeDefinition, clientPropertyName);
		// get the real root type
		final TypeDefinition rootTargetType = propertyCell.getTarget().values().stream()
				.map(Entity::getDefinition).map(EntityDefinition::getType).findFirst()
				.orElseThrow(() -> new IllegalArgumentException("No target type available"));
		// for ChildContext list with only the first path
		List<ChildContext> childContextList = targetPropertyEntityDef.getPropertyPath();
		attributeMapping = context.getOrCreateAttributeMapping(rootTargetType, null,
				childContextList);
		// set target attribute
		attributeMapping
				.setTargetAttribute(mapping.buildAttributeXPath(rootTargetType, childContextList));
		// create jdbcMultiValue object if is null
		if (attributeMapping.getJdbcMultipleValue() == null) {
			generateJdbcMultiValue();
		}
		attributeMapping.getJdbcMultipleValue().setTargetValue(getSourceExpressionAsCQL());
		// targetValue
		return attributeMapping;
	}

	private AttributeMappingType processAnonymousType(TypeDefinition typeDefinition,
			AppSchemaMappingContext context) {
		// currently we only support unbounded multi value anonymous types
		if (isUnboundedAnonymousMultivalueSequence(typeDefinition)) {
			final PropertyEntityDefinition targetPropertyEntityDef = targetProperty.getDefinition();
			final QName clientPropertyName = ofNullable(targetPropertyEntityDef)
					.map(PropertyEntityDefinition::getDefinition).map(PropertyDefinition::getName)
					.orElseThrow(() -> new IllegalArgumentException("No target name available"));
			// get the real root type
			final TypeDefinition rootTargetType = propertyCell.getTarget().values().stream()
					.map(Entity::getDefinition).map(EntityDefinition::getType).findFirst()
					.orElseThrow(() -> new IllegalArgumentException("No target type available"));
			// fir ChildContext list with only the first path
			List<ChildContext> childContextList = Arrays
					.asList(targetPropertyEntityDef.getPropertyPath().get(0));
			attributeMapping = context.getOrCreateAttributeMapping(rootTargetType, null,
					childContextList);
			// set target attribute
			attributeMapping.setTargetAttribute(
					mapping.buildAttributeXPath(rootTargetType, childContextList));
			// create jdbcMultiValue object if is null
			if (attributeMapping.getJdbcMultipleValue() == null) {
				generateJdbcMultiValue();
			}
			// add anonymousAttribute property for this attribute
			final String attrStepName = mapping.prefixedPathStep(clientPropertyName);
			if (!hasAnonymousAttribute(attrStepName)) {
				AnonymousAttributeType anonAttr = new AnonymousAttributeType();
				anonAttr.setName(attrStepName);
				anonAttr.setValue(getSourceExpressionAsCQL());
				attributeMapping.getAnonymousAttribute().add(anonAttr);
			}
			return attributeMapping;
		}
		return null;
	}

	private AttributeMappingType processAnonymousTypeAttribute(TypeDefinition typeDefinition,
			AppSchemaMappingContext context) {
		PropertyEntityDefinition targetPropertyEntityDef = targetProperty.getDefinition();
		TypeDefinition rootTargetType = propertyCell.getTarget().values().stream()
				.map(Entity::getDefinition).map(EntityDefinition::getType).findFirst()
				.orElseThrow(() -> new IllegalArgumentException("No target type available"));
		// we need to remove the last childContext because it is the attribute
		// name
		List<ChildContext> childContextList = new ArrayList<ChildContext>(
				targetPropertyEntityDef.getPropertyPath());
		childContextList.remove(childContextList.size() - 1);
		attributeMapping = context.getOrCreateAttributeMapping(rootTargetType, null,
				childContextList);

		return attributeMapping;
	}

	private boolean hasAnonymousAttribute(String attrStepName) {
		return attributeMapping.getAnonymousAttribute().stream()
				.anyMatch(a -> Objects.equals(a.getName(), attrStepName));
	}

	private void generateJdbcMultiValue() {
		Optional<EntityDefinition> sourceEntityDefinition = propertyCell.getSource().values()
				.stream().map(Entity::getDefinition).findFirst();
		Optional<String> sourceTypeLocalName = sourceEntityDefinition.map(EntityDefinition::getType)
				.map(TypeDefinition::getName).map(QName::getLocalPart);
		if (!sourceTypeLocalName.isPresent()) {
			return;
		}
		List<ParameterValue> parameters = typeCell.getTransformationParameters().get("join");
		if (parameters.isEmpty())
			return;
		final Set<JoinCondition> joinConditions = parameters.stream()
				.filter(p -> "default".equals(p.getType())).findFirst()
				.map(ParameterValue::getValue).filter(o -> o instanceof JoinParameter)
				.map(o -> (JoinParameter) o).map(JoinParameter::getConditions)
				.orElse(Collections.emptySet());
		if (joinConditions.isEmpty())
			return;
		JoinCondition joinCondition = joinConditions.stream().findFirst().get();
		String baseColumn = joinCondition.baseProperty.getLastPathElement().getChild().getName()
				.getLocalPart();
		String joinColumn = joinCondition.joinProperty.getLastPathElement().getChild().getName()
				.getLocalPart();
		JdbcMultiValueType jdbcValue = new JdbcMultiValueType();
		jdbcValue.setTargetTable(sourceTypeLocalName.get());
		jdbcValue.setSourceColumn(baseColumn);
		jdbcValue.setTargetColumn(joinColumn);
		attributeMapping.setJdbcMultipleValue(jdbcValue);
	}

	private boolean isUnboundedAnonymousMultivalueSequence(final TypeDefinition typeDefinition) {
		if (!ofNullable(propertyCell).map(Cell::getSource).map(x -> x.values()).isPresent()) {
			return false;
		}
		// check for join source table
		// get property source qname
		final Optional<QName> sourceQname = propertyCell.getSource().values().stream()
				.map(Entity::getDefinition).map(EntityDefinition::getType)
				.map(TypeDefinition::getName).findFirst();
		// get join source qnames
		final boolean isJoin = "eu.esdihumboldt.hale.align.join"
				.equals(typeCell.getTransformationIdentifier());
		if (!sourceQname.isPresent() || !isJoin) {
			return false;
		}
		// has our join a source with same Qname?
		@SuppressWarnings({ "unchecked", "rawtypes" })
		final Map<String, Collection<Entity>> sourceMap = (Map) ofNullable(typeCell)
				.map(Cell::getSource).map(m -> m.asMap()).orElse(Collections.emptyMap());
		final Optional<QName> joinQname = sourceMap.entrySet().stream().map(e -> e.getValue())
				.flatMap(c -> c.stream()).map(Entity::getDefinition).map(EntityDefinition::getType)
				.map(TypeDefinition::getName).filter(n -> n.equals(sourceQname.get())).findFirst();
		if (!joinQname.isPresent()) {
			return false;
		}
		// check if anonymous sequence is unbounded
		final Optional<GroupPropertyDefinition> gpropDefinition = typeDefinition
				.getDeclaredChildren().stream().map(ChildDefinition::asGroup).findFirst();
		final Long maxOccurs = gpropDefinition.map(d -> d.getConstraint(Cardinality.class))
				.map(Cardinality::getMaxOccurs).orElse(null);
		return Objects.equals(maxOccurs, -1L);
	}

	public static interface HandlerCommons {

		PropertyDefinition getParentPropertyDef(List<ChildContext> parentPropertyPath);

		Namespace getTargetPropertyNamespace(PropertyDefinition targetPropertyDef,
				AppSchemaMappingContext context, MappingWrapper mappingWrapper);

		boolean isAnonymousType(TypeDefinition typeDefinition);

		boolean isSequenceElement(TypeDefinition typeDefinition, Cell typeCell, Cell propertyCell,
				Property targetProperty);

	}

	private static class DefaultHandlerCommons implements HandlerCommons {

		/**
		 * @see it.geosolutions.hale.io.appschema.writer.internal.AbstractPropertyTransformationHandler.HandlerCommons#getParentPropertyDef(java.util.List)
		 */
		@Override
		public PropertyDefinition getParentPropertyDef(List<ChildContext> parentPropertyPath) {
			return parentPropertyPath.get(parentPropertyPath.size() - 1).getChild().asProperty();
		}

		@Override
		public Namespace getTargetPropertyNamespace(PropertyDefinition targetPropertyDef,
				AppSchemaMappingContext context, MappingWrapper mappingWrapper) {
			return context.getOrCreateNamespace(targetPropertyDef.getName().getNamespaceURI(),
					targetPropertyDef.getName().getPrefix());
		}

		@Override
		public boolean isAnonymousType(TypeDefinition typeDefinition) {
			final Optional<String> localName = ofNullable(typeDefinition)
					.map(TypeDefinition::getName).map(QName::getLocalPart);
			return "AnonymousType".equals(localName.orElse(null));
		}

		@Override
		public boolean isSequenceElement(TypeDefinition typeDefinition, Cell typeCell,
				Cell propertyCell, Property targetProperty) {
			if (typeCell == null || targetProperty == null)
				return false;
			final PropertyEntityDefinition targetPropertyEntityDef = targetProperty.getDefinition();
			if (targetPropertyEntityDef == null || !ofNullable(propertyCell).map(Cell::getSource)
					.map(x -> x.values()).isPresent())
				return false;
			final Optional<QName> sourceQname = propertyCell.getSource().values().stream()
					.map(Entity::getDefinition).map(EntityDefinition::getType)
					.map(TypeDefinition::getName).findFirst();
			// get join source qnames
			final boolean isJoin = sourceQname.isPresent() && "eu.esdihumboldt.hale.align.join"
					.equals(typeCell.getTransformationIdentifier());
			return AppSchemaIO.isUnboundedElement(targetPropertyEntityDef) && isJoin;
		}

	}

	private static final HandlerCommons DEFAULT_HANDLER_COMMONS = new DefaultHandlerCommons();

	private static HandlerCommons getDefaultHandlerCommons() {
		return DEFAULT_HANDLER_COMMONS;
	}

}
