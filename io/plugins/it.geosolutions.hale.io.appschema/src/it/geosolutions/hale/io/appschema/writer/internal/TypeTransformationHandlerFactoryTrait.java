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

package it.geosolutions.hale.io.appschema.writer.internal;

/**
 * TODO Type description
 * @author fernando
 */
public interface TypeTransformationHandlerFactoryTrait {

	/**
	 * Creates a new type transformation handler instance to handle the
	 * transformation function specified by the provided identifier.
	 * 
	 * @param typeTransformationIdentifier the type transformation function
	 *            identifier
	 * @return the type transformation handler instance
	 * @throws UnsupportedTransformationException if the specified
	 *             transformation is not supported
	 */
	TypeTransformationHandler createTypeTransformationHandler(String typeTransformationIdentifier)
			throws UnsupportedTransformationException;

}