package it.geosolutions.hale.io.appschema.mongodb;

import java.util.List;

import eu.esdihumboldt.hale.common.align.model.ChildContext;
import eu.esdihumboldt.hale.common.schema.model.PropertyDefinition;
import it.geosolutions.hale.io.appschema.impl.internal.generated.app_schema.NamespacesPropertyType.Namespace;
import it.geosolutions.hale.io.appschema.writer.internal.AbstractPropertyTransformationHandler.HandlerCommons;
import it.geosolutions.hale.io.appschema.writer.internal.mapping.AppSchemaMappingContext;
import it.geosolutions.hale.io.appschema.writer.internal.mapping.MappingWrapper;

/**
 * MongoDb implementation of internal commons for PropertyTransformationHandler.
 */
public class MongoDBPropertyTransformationHandlerCommons implements HandlerCommons {

	/**
	 * @see it.geosolutions.hale.io.appschema.writer.internal.AbstractPropertyTransformationHandler.HandlerCommons#getParentPropertyDef(java.util.List)
	 */
	@Override
	public PropertyDefinition getParentPropertyDef(List<ChildContext> parentPropertyPath) {
		return parentPropertyPath.isEmpty() ? null
				: parentPropertyPath.get(parentPropertyPath.size() - 1).getChild().asProperty();
	}

	@Override
	public Namespace getTargetPropertyNamespace(PropertyDefinition targetPropertyDef,
			AppSchemaMappingContext context, MappingWrapper mappingWrapper) {
		return mappingWrapper.getOrCreateNamespace(targetPropertyDef.getName().getNamespaceURI(),
				targetPropertyDef.getName().getPrefix());
	}

}
