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
