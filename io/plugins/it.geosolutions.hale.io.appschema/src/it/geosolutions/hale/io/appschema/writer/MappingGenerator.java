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

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import eu.esdihumboldt.hale.common.core.io.report.IOReporter;
import it.geosolutions.hale.io.appschema.writer.internal.mapping.MappingWrapper;
import it.geosolutions.hale.io.geoserver.FeatureType;
import it.geosolutions.hale.io.geoserver.Layer;
import it.geosolutions.hale.io.geoserver.Workspace;

/**
 * Translates a HALE alignment to an app-schema mapping configuration.
 */
public interface MappingGenerator {

	/**
	 * Generates the app-schema mapping configuration.
	 * 
	 * @param reporter status reporter
	 * @return the generated app-schema mapping configuration
	 * @throws IOException if an error occurs loading the mapping template file
	 */
	MappingWrapper generateMapping(IOReporter reporter) throws IOException;

	/**
	 * @return the generated mapping configuration
	 */
	MappingWrapper getGeneratedMapping();

	/**
	 * Generates the app-schema mapping configuration and writes it to the
	 * provided output stream.
	 * 
	 * <p>
	 * If the mapping configuration requires multiple files, only the main
	 * configuration file will be written.
	 * </p>
	 * 
	 * @param output the output stream to write to
	 * @param reporter the status reporter
	 * @throws IOException if an I/O error occurs
	 */
	void generateMapping(OutputStream output, IOReporter reporter) throws IOException;

	/**
	 * Generates the app-schema mapping configuration for the included types
	 * (non-feature types or non-top level feature types) and writes it to the
	 * provided output stream.
	 * 
	 * <p>
	 * If the mapping configuration does not require multiple files, an
	 * {@link IllegalStateException} is thrown.
	 * </p>
	 * 
	 * @param output the output stream to write to
	 * @param reporter the status reporter
	 * @throws IOException if an I/O error occurs
	 * @throws IllegalStateException if the mapping configuration does not
	 *             require multiple files
	 */
	void generateIncludedTypesMapping(OutputStream output, IOReporter reporter) throws IOException;

	/**
	 * Updates a schema URI in the generated mapping configuration.
	 * 
	 * <p>
	 * It is used mainly by exporters that need to change the target schema
	 * location.
	 * </p>
	 * 
	 * @param oldSchemaURI the current schema URI
	 * @param newSchemaURI the updated schema URI
	 */
	void updateSchemaURI(String oldSchemaURI, String newSchemaURI);

	/**
	 * Returns the generated app-schema datastore configuration.
	 * 
	 * @return the generated datastore configuration
	 * @throws IllegalStateException if no app-schema mapping configuration has
	 *             been generated yet or if no target schema is available
	 */
	it.geosolutions.hale.io.geoserver.DataStore getAppSchemaDataStore();

	/**
	 * Returns the generated workspace configuration for the main workspace.
	 * 
	 * @return the main workspace configuration
	 * @throws IllegalStateException if the no app-schema mapping configuration
	 *             has been generated yet or if no target schema is available
	 */
	Workspace getMainWorkspace();

	/**
	 * Returns the generated namespace configuration for the main namespace.
	 * 
	 * @return the main namespace configuration
	 * @throws IllegalStateException if no app-schema mapping configuration has
	 *             been generated yet or if no target schema is available
	 */
	it.geosolutions.hale.io.geoserver.Namespace getMainNamespace();

	/**
	 * Returns the generated namespace configuration for secondary namespaces.
	 * 
	 * @return the secondary namespaces configuration
	 * @throws IllegalStateException if no app-schema mapping configuration has
	 *             been generated yet or if no target schema is available
	 */
	List<it.geosolutions.hale.io.geoserver.Namespace> getSecondaryNamespaces();

	/**
	 * Returns the configuration of the workspace associated to the provided
	 * namespace.
	 * 
	 * @param ns the namespace
	 * @return the configuration of the workspace associated to <code>ns</code>
	 */
	Workspace getWorkspace(it.geosolutions.hale.io.geoserver.Namespace ns);

	/**
	 * Returns the generated feature type configuration for all mapped feature
	 * types.
	 * 
	 * @return the generated feature type configuration
	 */
	List<FeatureType> getFeatureTypes();

	/**
	 * Returns the layer configuration for the provided feature type.
	 * 
	 * @param featureType the feature type
	 * @return the layer configuration
	 */
	Layer getLayer(FeatureType featureType);

	/**
	 * Writes the generated app-schema mapping to the provided output stream.
	 * 
	 * <p>
	 * If the mapping configuration requires multiple files, only the main
	 * configuration file will be written.
	 * </p>
	 * 
	 * @param out the output stream to write to
	 * @throws IOException if an I/O error occurs
	 */
	void writeMappingConf(OutputStream out) throws IOException;

	/**
	 * Writes the generated app-schema mapping configuration for the included
	 * types (non-feature types or non-top level feature types) to the provided
	 * output stream.
	 * 
	 * <p>
	 * If the mapping configuration does not require multiple files, an
	 * {@link IllegalStateException} is thrown.
	 * </p>
	 * 
	 * @param out the output stream to write to
	 * @throws IOException if an I/O error occurs
	 * @throws IllegalStateException if the mapping configuration does not
	 *             require multiple files
	 */
	void writeIncludedTypesMappingConf(OutputStream out) throws IOException;

}