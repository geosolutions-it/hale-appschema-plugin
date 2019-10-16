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

package it.geosolutions.hale.io.appschema;

import java.util.Optional;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import eu.esdihumboldt.hale.common.align.model.Cell;
import eu.esdihumboldt.hale.common.align.model.ChildContext;
import eu.esdihumboldt.hale.common.align.model.impl.PropertyEntityDefinition;
import eu.esdihumboldt.hale.common.schema.model.ChildDefinition;
import eu.esdihumboldt.hale.common.schema.model.GroupPropertyDefinition;
import eu.esdihumboldt.hale.common.schema.model.TypeDefinition;
import eu.esdihumboldt.hale.common.schema.model.constraint.property.Cardinality;

/**
 * Class holding constants and utility methods.
 * 
 * @author Stefano Costa, GeoSolutions
 */
public abstract class AppSchemaIO {

	/**
	 * 
	 */
	private static final String ANONYMOUS_TYPE = "AnonymousType";
	/**
	 * Namespace for app-schema mapping elements.
	 */
	public static final String APP_SCHEMA_NAMESPACE = "http://www.geotools.org/app-schema";
	/**
	 * Default prefix for app-schema namespace.
	 */
	public static final String APP_SCHEMA_PREFIX = "as";
	/**
	 * ID of app-schema mapping file content type.
	 */
	public static final String CONTENT_TYPE_MAPPING = "it.geosolutions.hale.io.appschema.mapping";
	/**
	 * ID of app-schema configuration archive content type.
	 */
	public static final String CONTENT_TYPE_ARCHIVE = "it.geosolutions.hale.io.appschema.archive";
	/**
	 * ID of app-schema configuration REST content type
	 */
	public static final String CONTENT_TYPE_REST = "it.geosolutions.hale.io.appschema.rest";
	/**
	 * Datastore configuration parameter name.
	 */
	public static final String PARAM_DATASTORE = "appschema.source.datastore";
	/**
	 * Feature chaining configuration parameter name.
	 */
	public static final String PARAM_CHAINING = "appschema.feature.chaining";
	/**
	 * Include schema configuration parameter name.
	 */
	public static final String PARAM_INCLUDE_SCHEMA = "appschema.include.schema";
	/**
	 * Workspace configuration parameter name.
	 */
	public static final String PARAM_WORKSPACE = "appschema.workspace.conf";
	/**
	 * REST user configuration parameter name.
	 */
	public static final String PARAM_USER = "appschema.rest.user";
	/**
	 * REST password configuration parameter name.
	 */
	public static final String PARAM_PASSWORD = "appschema.rest.password";

	/**
	 * Location of the default mapping file template.
	 */
	public static final String MAPPING_TEMPLATE = "/it/geosolutions/hale/io/geoserver/template/data/mapping-template.xml";
	/**
	 * Namespace configuration file name.
	 */
	public static final String NAMESPACE_FILE = "namespace.xml";
	/**
	 * Workspace configuration file name.
	 */
	public static final String WORKSPACE_FILE = "workspace.xml";
	/**
	 * Datastore configuration file name.
	 */
	public static final String DATASTORE_FILE = "datastore.xml";
	/**
	 * Feature type configuration file name.
	 */
	public static final String FEATURETYPE_FILE = "featuretype.xml";
	/**
	 * Layer configuration file name.
	 */
	public static final String LAYER_FILE = "layer.xml";
	/**
	 * Included types mapping configuration file name.
	 */
	public static final String INCLUDED_TYPES_MAPPING_FILE = "includedTypes.xml";

	/**
	 * Retrieve the first element descendant of <code>parent</code>, with the
	 * provided tag name.
	 * 
	 * @param parent the parent element
	 * @param tagName the tag name
	 * @return the first matching <code>Element</code> node descendant of
	 *         <code>parent</code>
	 */
	public static Element getFirstElementByTagName(Element parent, String tagName) {
		return getFirstElementByTagName(parent, tagName, null);
	}

	/**
	 * Retrieve the first element descendant of <code>parent</code>, with the
	 * provided tag name and namespace.
	 * 
	 * @param parent the parent element
	 * @param tagName the tag name
	 * @param namespace the namespace
	 * @return the first matching <code>Element</code> node descendant of
	 *         <code>parent</code>
	 */
	public static Element getFirstElementByTagName(Element parent, String tagName,
			String namespace) {
		if (namespace == null)
			namespace = "";

		NodeList elements = (namespace.isEmpty()) ? parent.getElementsByTagName(tagName)
				: parent.getElementsByTagNameNS(namespace, tagName);

		if (elements != null && elements.getLength() > 0) {
			return (Element) elements.item(0);
		}
		else {
			return null;
		}
	}

	/**
	 * Checks if provided TypeDefinition is a GML ReferenceType.
	 * 
	 * @param typeDef type definition to check.
	 * @return true if it is a ReferenceType, else returns false.
	 */
	public static boolean isReferenceType(TypeDefinition typeDef) {
		try {
			return typeDef.getName().getLocalPart().equals("ReferenceType")
					&& typeDef.getName().getNamespaceURI().equals("http://www.opengis.net/gml/3.2");
		} catch (NullPointerException e) {
			return false;
		}
	}

	/**
	 * Checks if provided TypeDefinition is an anonymous unbounded sequence
	 * type.
	 */
	public static boolean isUnboundedSequence(TypeDefinition typeDef) {
		if (typeDef == null || typeDef.getName() == null || typeDef.getChildren() == null
				|| typeDef.getChildren().size() != 1)
			return false;
		final QName qname = typeDef.getName();
		final boolean isAnonymousType = ANONYMOUS_TYPE.equals(qname.getLocalPart());
		final Optional<GroupPropertyDefinition> sequence = typeDef.getChildren().stream()
				.filter(c -> c instanceof GroupPropertyDefinition)
				.map(c -> (GroupPropertyDefinition) c).filter(c -> isCardinality1N(c)).findFirst();
		return isAnonymousType && sequence.isPresent();
	}

	private static boolean isCardinality1N(GroupPropertyDefinition definition) {
		final Cardinality cardinality = definition.getConstraint(Cardinality.class);
		if (cardinality != null) {
			return cardinality.getMaxOccurs() == -1L;
		}
		return false;
	}

	public static boolean isUnboundedElement(final PropertyEntityDefinition selectedProperty) {
		final Optional<Cardinality> cardinality = Optional.of(selectedProperty)
				.map(PropertyEntityDefinition::getLastPathElement).map(ChildContext::getChild)
				.map(cd -> (ChildDefinition<Object>) cd)
				.map(cd -> cd.getConstraint(Cardinality.class)).map(x -> x);
		final Long maxOccurs = cardinality.map(Cardinality::getMaxOccurs).orElse(1L);
		return maxOccurs == -1L;
	}

	/**
	 * Checks if the provided cell is compatible for Href client property
	 * mappings.
	 * 
	 * @param cell the cell object to check.
	 * @return true if it has a compatible transformation identifier.
	 */
	public static boolean isHrefClientPropertyCompatible(Cell cell) {
		if (cell == null)
			return false;
		final String trIdent = cell.getTransformationIdentifier();
		return "eu.esdihumboldt.hale.align.rename".equals(trIdent)
				|| "eu.esdihumboldt.hale.align.formattedstring".equals(trIdent);
	}
}
