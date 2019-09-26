package it.geosolutions.hale.io.appschema.mongodb;

import java.util.Iterator;

import eu.esdihumboldt.hale.common.align.model.Cell;
import eu.esdihumboldt.hale.common.align.model.ChildContext;
import eu.esdihumboldt.hale.common.align.model.Property;
import eu.esdihumboldt.hale.common.schema.model.PropertyDefinition;
import eu.esdihumboldt.hale.common.schema.model.TypeDefinition;
import eu.esdihumboldt.hale.io.mongo.JsonPathConstraint;
import it.geosolutions.hale.io.appschema.impl.internal.generated.app_schema.AttributeExpressionMappingType;
import it.geosolutions.hale.io.appschema.impl.internal.generated.app_schema.AttributeMappingType;
import it.geosolutions.hale.io.appschema.impl.internal.generated.app_schema.TypeMappingsPropertyType.FeatureTypeMapping;
import it.geosolutions.hale.io.appschema.model.ChainConfiguration;
import it.geosolutions.hale.io.appschema.writer.internal.TypeTransformationHandler;
import it.geosolutions.hale.io.appschema.writer.internal.mapping.AppSchemaMappingContext;
import it.geosolutions.hale.io.appschema.writer.internal.mapping.MappingWrapper;

public class CollectionLinkHandler implements TypeTransformationHandler {

	@Override
	public FeatureTypeMapping handleTypeTransformation(Cell typeCell,
			AppSchemaMappingContext context) {
		MappingWrapper mapping = context.getMappingWrapper();
		Property source = Utils.getFirstEntity(typeCell.getSource(), Utils::convertToProperty);
		// get parent JSON path
		Iterator<ChildContext> it = source.getDefinition().getPropertyPath().iterator();
		// System.out.println("##### JSON PATHS ########");
		StringBuilder jsonPathb = new StringBuilder();
		while (it.hasNext()) {
			ChildContext c = it.next();
			PropertyDefinition cp = c.getChild().asProperty();
			JsonPathConstraint constraint = cp.getConstraint(JsonPathConstraint.class);
			// System.out.println(constraint.getJsonPath());
			// System.out.println(cp.getDisplayName());
			jsonPathb.append(cp.getDisplayName()).append(".");
		}
		jsonPathb.deleteCharAt(jsonPathb.length() - 1);
		// System.out.println("##### JSON PATHS ########");
		/////
		Property target = Utils.getFirstEntity(typeCell.getTarget(), Utils::convertToProperty);
		TypeDefinition targetType = Utils.getXmlPropertyType(target);
		// String jsonPath = Utils.getRelativeJsonPath(source);
		String jsonPath = jsonPathb.toString();
		// System.out.println("HSON PATH: " + jsonPath);
		boolean secondary = !Utils.getRootType(source)
				.equals(source.getDefinition().getType().getDisplayName());
		String mappingNameNested = Utils.getRootType(source) + "-" + targetType.getDisplayName();
		// mappingName = null;
		FeatureTypeMapping nested = mapping.getOrCreateFeatureTypeMapping(targetType,
				mappingNameNested, true);
		nested.setSourceType(Utils.getRootType(source));
		// nested.setMappingName(jsonPath.getJsonPath());
		String mappingNameContainer = Utils.getRootType(source) + "-"
				+ target.getDefinition().getType().getDisplayName();
		AttributeMappingType containerJoinMapping = mapping.getOrCreateAttributeMapping(
				target.getDefinition().getType(), mappingNameContainer,
				target.getDefinition().getPropertyPath());
		containerJoinMapping.setTargetAttribute(mapping.buildAttributeXPath(
				source.getDefinition().getDefinition().getPropertyType(),
				target.getDefinition().getPropertyPath()));
		containerJoinMapping.setIsMultiple(true);
		ChainConfiguration cg = new ChainConfiguration();
		cg.setNestedTypeTarget(target.getDefinition());
		context.getFeatureChaining().putChain(jsonPath, 0, cg);
		AttributeExpressionMappingType containerSourceExpr = new AttributeExpressionMappingType();
		// join column extracted from join condition
		containerSourceExpr.setOCQL(String.format("collectionLink('%s')", jsonPath));
		containerSourceExpr.setLinkElement(getLinkElementValue(nested));
		String linkField = mapping.getUniqueFeatureLinkAttribute(source.getDefinition().getType(),
				"nestedFTMapping.getMappingName()");
		containerSourceExpr.setLinkField(linkField);
		containerJoinMapping.setSourceExpression(containerSourceExpr);
		AttributeMappingType nestedJoinMapping = new AttributeMappingType();
		AttributeExpressionMappingType nestedSourceExpr = new AttributeExpressionMappingType();
		// join column extracted from join condition
		nestedSourceExpr.setOCQL("nestedCollectionLink()");
		nestedJoinMapping.setSourceExpression(nestedSourceExpr);
		nestedJoinMapping.setTargetAttribute(linkField);
		nested.getAttributeMappings().getAttributeMapping().add(nestedJoinMapping);

		// add collection id to the container
		AttributeMappingType attributeMapping = mapping.getOrCreateAttributeMapping(targetType,
				mappingNameNested, null);
		attributeMapping.setTargetAttribute(nested.getTargetElement());
		// set id expression
		AttributeExpressionMappingType idExpression = new AttributeExpressionMappingType();
		idExpression.setOCQL("collectionId()");
		attributeMapping.setIdExpression(idExpression);

		// return nested feature type definition
		return nested;
	}

	private String getLinkElementValue(FeatureTypeMapping nestedFeatureTypeMapping) {
		if (nestedFeatureTypeMapping.getMappingName() != null
				&& !nestedFeatureTypeMapping.getMappingName().isEmpty()) {
			// playing safe: always enclose mapping name in single quotes
			return "'" + nestedFeatureTypeMapping.getMappingName() + "'";
		}
		else {
			return nestedFeatureTypeMapping.getTargetElement();
		}
	}
}