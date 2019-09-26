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

import it.geosolutions.hale.io.appschema.writer.internal.SingleSourceToTargetHandler;
import it.geosolutions.hale.io.appschema.writer.internal.TypeTransformationHandler;
import it.geosolutions.hale.io.appschema.writer.internal.TypeTransformationHandlerFactory;
import it.geosolutions.hale.io.appschema.writer.internal.UnsupportedTransformationException;

public class MongoTypeTransformationHandlerFactory extends TypeTransformationHandlerFactory {

	public MongoTypeTransformationHandlerFactory() {
	}

	/**
	 * @see it.geosolutions.hale.io.appschema.writer.internal.TypeTransformationHandlerFactoryTrait#createTypeTransformationHandler(java.lang.String)
	 */
	@Override
	public TypeTransformationHandler createTypeTransformationHandler(
			String typeTransformationIdentifier) throws UnsupportedTransformationException {
		TypeTransformationHandler handler = super.createTypeTransformationHandler(
				typeTransformationIdentifier);
		if (handler instanceof SingleSourceToTargetHandler) {
			((SingleSourceToTargetHandler) handler)
					.setHandlerDelegate(new MongoSingleSourceToTargetHandler());
		}
		return handler;
	}

}
