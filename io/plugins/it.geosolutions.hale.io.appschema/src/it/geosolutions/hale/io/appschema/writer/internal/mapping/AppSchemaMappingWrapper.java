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

package it.geosolutions.hale.io.appschema.writer.internal.mapping;

import it.geosolutions.hale.io.appschema.impl.internal.generated.app_schema.AppSchemaDataAccessType;

/**
 * App-schema mapping configuration wrapper.
 * 
 * <p>
 * Holds the state associated to the same mapping configuration and provides
 * utility methods to mutate it.
 * </p>
 * 
 * @author Stefano Costa, GeoSolutions
 */
public class AppSchemaMappingWrapper extends AppSchemaMappingWrapperBase {

	/**
	 * Constructor.
	 * 
	 * @param appSchemaMapping the app-schema mapping to wrap
	 */
	public AppSchemaMappingWrapper(AppSchemaDataAccessType appSchemaMapping) {
		super(appSchemaMapping);
	}

}
