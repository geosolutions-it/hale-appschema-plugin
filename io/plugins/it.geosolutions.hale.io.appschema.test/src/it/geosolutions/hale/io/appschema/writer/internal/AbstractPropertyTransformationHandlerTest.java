/*
 * Copyright (c) 2020 wetransform GmbH
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

import static org.junit.Assert.assertFalse;

import javax.xml.namespace.QName;

import org.junit.Test;

import eu.esdihumboldt.hale.common.schema.model.TypeDefinition;
import eu.esdihumboldt.hale.io.xsd.reader.internal.AnonymousXmlType;

/**
 * Unit tests for {@link AbstractPropertyTransformationHandler} class.
 */
public class AbstractPropertyTransformationHandlerTest {

	@Test
	public void testIsValidFeatureType() {
		TypeDefinition type = new AnonymousXmlType(new QName("test", "test"));
		assertFalse(AbstractPropertyTransformationHandler.isValidFeatureType(type));
	}

}
