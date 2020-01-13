package eu.esdihumboldt.hale.io.mongodb.ui;

import java.util.Map;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import eu.esdihumboldt.hale.common.core.io.IOProvider;
import eu.esdihumboldt.hale.common.core.io.Value;
import eu.esdihumboldt.hale.common.core.io.project.model.IOConfiguration;
import eu.esdihumboldt.hale.io.mongo.Constants;
import eu.esdihumboldt.hale.ui.io.IOWizard;
import eu.esdihumboldt.hale.ui.io.IOWizardPage;
import eu.esdihumboldt.hale.ui.io.config.AbstractConfigurationPage;

public class AuthenticationPage extends AbstractConfigurationPage<IOProvider, IOWizard<IOProvider>> {

	private Text user;
	private Text password;
	private Text database;

	public AuthenticationPage() {
		super("userPassword", "Authentication", null);
		setDescription("Please enter user name, password and authentication database to use.");
	}

	@Override
	public void enable() {
		// do nothing
	}

	@Override
	public void disable() {
		// do nothing
	}

	@Override
	public boolean updateConfiguration(IOProvider provider) {
		provider.setParameter(Constants.USER, Value.of(user.getText()));
		provider.setParameter(Constants.PASSWORD, Value.of(password.getText()));
		provider.setParameter(Constants.AUTHENTICATION_DATABASE, Value.of(database.getText()));
		return true;
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
				// updateState(false);
			}
		});
		return component;
	}

	@Override
	protected void createContent(Composite page) {
		// setup the UI page, one column for labels and another for components
		page.setLayout(GridLayoutFactory.swtDefaults().numColumns(2).create());
		GridDataFactory labels = GridDataFactory.swtDefaults().align(SWT.END, SWT.CENTER);
		GridDataFactory components = GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false);
		// add components
		user = createTextComponent(page, labels, components, "User", false);
		password = createTextComponent(page, labels, components, "Password", true);
		database = createTextComponent(page, labels, components, "Database", false);
		// filler
		new Label(page, SWT.NONE);
		// label with warning message
		Composite warnComp = new Composite(page, SWT.NONE);
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(warnComp);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(warnComp);

		Label warnImage = new Label(warnComp, SWT.NONE);
		warnImage.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_WARN_TSK));
		GridDataFactory.swtDefaults().align(SWT.CENTER, SWT.BEGINNING).applyTo(warnImage);

		Label warn = new Label(warnComp, SWT.WRAP);
		warn.setText(
				"User and password may be saved in the project configuration as plain text. Be aware of this when distributing the project.");
		GridDataFactory.swtDefaults().align(SWT.FILL, SWT.BEGINNING).grab(true, false).hint(300, SWT.DEFAULT)
				.applyTo(warn);

		setPageComplete(false);
	}

	@Override
	protected void onShowPage(boolean firstShow) {
		if (firstShow) {
			// setPageComplete(true);
			setPageComplete(true);
		}
		user.setFocus();
	}
}
