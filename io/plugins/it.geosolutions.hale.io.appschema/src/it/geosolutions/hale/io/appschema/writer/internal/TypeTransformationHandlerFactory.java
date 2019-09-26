package it.geosolutions.hale.io.appschema.writer.internal;

import eu.esdihumboldt.hale.common.align.model.functions.JoinFunction;
import eu.esdihumboldt.hale.common.align.model.functions.MergeFunction;
import eu.esdihumboldt.hale.common.align.model.functions.RetypeFunction;

/**
 * Instantiates the type transformation handler capable of handling the
 * specified transformation function.
 * 
 * @author Stefano Costa, GeoSolutions
 */
public class TypeTransformationHandlerFactory implements TypeTransformationHandlerFactoryTrait {

	private static TypeTransformationHandlerFactoryTrait instance;

	public TypeTransformationHandlerFactory() {

	}

	/**
	 * Return the singleton factory instance.
	 * 
	 * @return the factory instance
	 */
	public static TypeTransformationHandlerFactoryTrait getInstance() {
		if (instance == null) {
			instance = new TypeTransformationHandlerFactory();
		}

		return instance;
	}

	/**
	 * @see it.geosolutions.hale.io.appschema.writer.internal.TypeTransformationHandlerFactoryTrait#createTypeTransformationHandler(java.lang.String)
	 */
	@Override
	public TypeTransformationHandler createTypeTransformationHandler(
			String typeTransformationIdentifier) throws UnsupportedTransformationException {
		if (typeTransformationIdentifier == null || typeTransformationIdentifier.trim().isEmpty()) {
			throw new IllegalArgumentException("typeTransformationIdentifier must be set");
		}

		if (typeTransformationIdentifier.equals(RetypeFunction.ID)) {
			return new RetypeHandler();
		}
		else if (typeTransformationIdentifier.equals(MergeFunction.ID)) {
			return new MergeHandler();
		}
		else if (typeTransformationIdentifier.equals(JoinFunction.ID)) {
			return new JoinHandler();
		}
		else {
			String errMsg = String.format("Unsupported type transformation %s",
					typeTransformationIdentifier);
			throw new UnsupportedTransformationException(errMsg, typeTransformationIdentifier);
		}
	}

}
