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

package it.geosolutions.hale.io.appschema.writer;

import static org.junit.Assert.assertEquals;

import javax.xml.namespace.QName;

import org.junit.Test;

import eu.esdihumboldt.hale.common.schema.model.PropertyDefinition;
import eu.esdihumboldt.hale.common.schema.model.impl.DefaultPropertyDefinition;
import eu.esdihumboldt.hale.common.schema.model.impl.DefaultTypeDefinition;
import eu.esdihumboldt.hale.common.schema.model.impl.internal.ReparentProperty;

/**
 * Testing class for {@link AppSchemaMappingGenerator} class.
 * 
 * @author Fernando Mino, Geosolutions.
 */
public class AppSchemaMappingGeneratorTest {

	private static final String TYPE_NAMESPACE = "http://geoserver.org/1.0";

	/**
	 * Check tryInferNamespacePrefix method for infer Prefix on some cases child
	 * QName doesn't have any value.
	 */
	@Test
	public void testTryInferNamespacePrefix() {
		DefaultTypeDefinition parentType = new DefaultTypeDefinition(
				new QName(TYPE_NAMESPACE, "ParentType", "geo"));

		DefaultTypeDefinition definitionGroup = new DefaultTypeDefinition(
				new QName(TYPE_NAMESPACE, "GroupDef", "geo"));

		DefaultTypeDefinition propertyType = new DefaultTypeDefinition(
				new QName(TYPE_NAMESPACE, "PropertyType", ""));
		final DefaultPropertyDefinition propertyDefinition = new DefaultPropertyDefinition(
				new QName(TYPE_NAMESPACE, "level"), definitionGroup, propertyType);
		PropertyDefinition child = new ReparentProperty(propertyDefinition, parentType);
		QName qName = AppSchemaMappingGenerator.tryInferNamespacePrefix(child);
		assertEquals(TYPE_NAMESPACE, qName.getNamespaceURI());
		assertEquals("geo", qName.getPrefix());
	}

}
