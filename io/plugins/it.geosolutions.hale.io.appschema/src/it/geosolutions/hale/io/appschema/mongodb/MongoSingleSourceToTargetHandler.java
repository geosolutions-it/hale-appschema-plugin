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

package it.geosolutions.hale.io.appschema.mongodb;

import com.google.common.collect.ListMultimap;

import eu.esdihumboldt.hale.common.align.model.Cell;
import eu.esdihumboldt.hale.common.align.model.Entity;
import eu.esdihumboldt.hale.common.align.model.impl.DefaultType;
import eu.esdihumboldt.hale.common.align.model.impl.TypeEntityDefinition;
import eu.esdihumboldt.hale.common.schema.model.TypeDefinition;
import eu.esdihumboldt.hale.io.mongo.JsonPathConstraint;
import it.geosolutions.hale.io.appschema.impl.internal.generated.app_schema.TypeMappingsPropertyType.FeatureTypeMapping;
import it.geosolutions.hale.io.appschema.writer.internal.TypeTransformationHandler;
import it.geosolutions.hale.io.appschema.writer.internal.mapping.AppSchemaMappingContext;

/**
 * MongoDB implementation for SingleSourceToTargetHandler.
 */
public class MongoSingleSourceToTargetHandler implements TypeTransformationHandler {

	/**
	 * @see it.geosolutions.hale.io.appschema.writer.internal.TypeTransformationHandler#handleTypeTransformation(eu.esdihumboldt.hale.common.align.model.Cell,
	 *      it.geosolutions.hale.io.appschema.writer.internal.mapping.AppSchemaMappingContext)
	 */
	@Override
	public FeatureTypeMapping handleTypeTransformation(Cell typeCell,
			AppSchemaMappingContext context) {
		ListMultimap<String, ? extends Entity> sourceEntities = typeCell.getSource();
		if (sourceEntities == null || sourceEntities.size() == 0) {
			throw new IllegalStateException("No source type has been specified.");
		}
		ListMultimap<String, ? extends Entity> targetEntities = typeCell.getTarget();
		if (targetEntities == null || targetEntities.size() == 0) {
			throw new IllegalStateException("No target type has been specified.");
		}

		// Maps 1 source to 1 target, so it is safe to pick the first entity
		// in
		// the list
		Entity sourceType = sourceEntities.values().iterator().next();
		Entity targetType = targetEntities.values().iterator().next();
		TypeDefinition targetTypeDef = targetType.getDefinition().getType();

		// check if its secondary
		// boolean secondary = !Utils.getRootType(sourceType)
		// .equals(source.getDefinition().getType().getDisplayName());

		String mappingName = null;
		boolean secondary = false;
		if (sourceType instanceof DefaultType) {
			TypeEntityDefinition t = ((DefaultType) sourceType).getDefinition();
			JsonPathConstraint c = t.getType().getConstraint(JsonPathConstraint.class);
			if (c != null) {
				secondary = c.getRootKey() != null
						&& !c.getRootKey().equals(t.getType().getDisplayName());
				mappingName = c.getRootKey() + "-"
						+ targetType.getDefinition().getType().getDisplayName();
			}
		}

		FeatureTypeMapping ftMapping = context.getMappingWrapper()
				.getOrCreateFeatureTypeMapping(targetTypeDef, mappingName, secondary);

		// handle MongoDB specific case
		JsonPathConstraint jsonPath = sourceType.getDefinition().getType()
				.getConstraint(JsonPathConstraint.class);
		if (jsonPath.isValid()) {
			ftMapping.setSourceType(jsonPath.getRootKey());
		}
		else {
			ftMapping.setSourceType(sourceType.getDefinition().getType().getName().getLocalPart());
		}

		return ftMapping;
	}
}
