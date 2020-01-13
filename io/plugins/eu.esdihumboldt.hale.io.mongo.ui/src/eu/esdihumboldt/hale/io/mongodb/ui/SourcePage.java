package eu.esdihumboldt.hale.io.mongodb.ui;

import java.io.InputStream;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import eu.esdihumboldt.hale.common.core.io.ImportProvider;
import eu.esdihumboldt.hale.common.core.io.supplier.LocatableInputSupplier;
import eu.esdihumboldt.hale.io.mongo.Source;
import eu.esdihumboldt.hale.ui.io.source.AbstractProviderSource;

public final class SourcePage extends AbstractProviderSource<ImportProvider> {

	private Text host;
	private Text port;
	private Text database;

	@Override
	public void createControls(Composite page) {
		// setup the UI page, one column for labels and another for components
		page.setLayout(GridLayoutFactory.swtDefaults().numColumns(2).create());
		GridDataFactory labels = GridDataFactory.swtDefaults().align(SWT.END, SWT.CENTER);
		GridDataFactory components = GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false);
		// instantiate connection info components
		host = createTextComponent(page, labels, components, "Host", false);
		port = createTextComponent(page, labels, components, "Port", false);
		database = createTextComponent(page, labels, components, "Database*", false);
		// preset label
		Label providerLabel = new Label(page, SWT.NONE);
		providerLabel.setText("Import as");
		labels.applyTo(providerLabel);
		// create provider combo
		ComboViewer provider = createProviders(page);
		components.applyTo(provider.getControl());
		// initial state update
		updateState(true);
	}

	private Text createTextComponent(Composite page, GridDataFactory labels, GridDataFactory components,
			String labelText, boolean password) {
		// create the component label
		Label label = new Label(page, SWT.NONE);
		label.setText(labelText);
		labels.applyTo(label);
		// create the component
		Text component = new Text(page, password ? SWT.BORDER | SWT.SINGLE | SWT.PASSWORD : SWT.BORDER | SWT.SINGLE);
		components.applyTo(component);
		component.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(ModifyEvent event) {
				updateState(false);
			}
		});
		return component;
	}

	@Override
	protected LocatableInputSupplier<? extends InputStream> getSource() {
		return (LocatableInputSupplier<InputStream>) new Source(host.getText(), port.getText(), database.getText());
	}

	@Override
	protected boolean isValidSource() {
		return database.getText() != null && !database.getText().isEmpty();
	}
}