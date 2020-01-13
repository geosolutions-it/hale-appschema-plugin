package it.geosolutions.hale.io.appschema.mongodb;

import eu.esdihumboldt.hale.common.align.model.Property;
import it.geosolutions.hale.io.appschema.writer.AppSchemaMappingUtils;
import it.geosolutions.hale.io.appschema.writer.internal.AbstractPropertyTransformationHandler;

/**
 * Rename Handler implementation for MongoDB cases.
 */
public class MongoRenameHandler extends AbstractPropertyTransformationHandler {

	/**
	 * @see it.geosolutions.hale.io.appschema.writer.internal.AbstractPropertyTransformationHandler#getSourceExpressionAsCQL()
	 */
	@Override
	protected String getSourceExpressionAsCQL() {
		Property source = AppSchemaMappingUtils.getSourceProperty(propertyCell);

		String cqlExpression = source.getDefinition().getDefinition().getName().getLocalPart();

		// apply MongoDB JSON selection if needed
		String jsonPath = Utils.getRelativeJsonPath(source);
		if (jsonPath != null) {
			cqlExpression = String.format("jsonSelect('%s')", jsonPath);
		}

		return getConditionalExpression(source.getDefinition(), cqlExpression);
	}

}
