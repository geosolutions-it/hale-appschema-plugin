package it.geosolutions.hale.io.appschema.writer.internal;

import it.geosolutions.hale.io.appschema.writer.internal.mapping.AppSchemaMappingContext;
import eu.esdihumboldt.hale.common.align.model.Cell;
import it.geosolutions.hale.io.appschema.impl.internal.generated.app_schema.TypeMappingsPropertyType.FeatureTypeMapping;

/**
 * Interface defining the API for type transformation handlers.
 * 
 * @author Stefano Costa, GeoSolutions
 */
public interface TypeTransformationHandler {

	/**
	 * Translates a type cell to an app-schema feature type mapping.
	 * 
	 * @param typeCell the type cell
	 * @param context the mapping context
	 * @return the feature type mapping
	 */
	public FeatureTypeMapping handleTypeTransformation(Cell typeCell,
			AppSchemaMappingContext context);

}
