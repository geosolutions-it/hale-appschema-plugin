
package eu.esdihumboldt.hale.io.mongodb.ui;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import de.fhg.igd.slf4jplus.ALogger;
import de.fhg.igd.slf4jplus.ALoggerFactory;
import eu.esdihumboldt.hale.common.core.io.IOProvider;
import eu.esdihumboldt.hale.common.core.io.ImportProvider;
import eu.esdihumboldt.hale.common.core.io.Value;
import eu.esdihumboldt.hale.common.core.io.extension.IOProviderDescriptor;
import eu.esdihumboldt.hale.common.schema.io.SchemaReader;
import eu.esdihumboldt.hale.io.mongo.Client;
import eu.esdihumboldt.hale.io.mongo.ClientBuilder;
import eu.esdihumboldt.hale.io.mongo.Constants;
import eu.esdihumboldt.hale.ui.io.IOWizard;
import eu.esdihumboldt.hale.ui.io.config.AbstractConfigurationPage;

public class CollectionsPage extends AbstractConfigurationPage<ImportProvider, IOWizard<ImportProvider>> {

	private static final ALogger LOGGER = ALoggerFactory.getLogger(CollectionsPage.class);

	private CheckboxTableViewer schemaTable;
	private final List<String> schemas = new ArrayList<String>();

	// private Composite innerPage;
	private Composite page;

	private boolean isEnable = false;
	private boolean multipleSelection = true;
	// private SchemaSelector customSelector = null;
	// private DriverConfiguration config = null;
	private Button selectAll = null;

	private ComboViewer collectionsCombo;
	private ComboViewer maxCombo;
	private Text specificElements;

	public CollectionsPage() {
		super("schemaRetrieval", "Schemas Retrieval", null);
		setDescription("Please select the collection from where to import the schema.");
	}

	@Override
	public void enable() {
		// Do nothing

	}

	@Override
	public void disable() {
		// Do nothing

	}

	@Override
	public boolean updateConfiguration(ImportProvider provider) {
		provider.setParameter(Constants.SPECIFIC_ELEMENTS, Value.of(specificElements.getText()));
		return true;
	}

	@Override
	protected void createContent(Composite page) {
		// setup the UI page, one column for labels and another for
		page.setLayout(GridLayoutFactory.swtDefaults().numColumns(2).create());
		GridDataFactory labels = GridDataFactory.swtDefaults().align(SWT.END, SWT.CENTER);
		GridDataFactory components = GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false);
		// set the label
		Label collectionsLabel = new Label(page, SWT.NONE);
		collectionsLabel.setText("Select collection: ");
		labels.applyTo(collectionsLabel);
		// create collection selector combo
		collectionsCombo = new ComboViewer(page, SWT.DROP_DOWN | SWT.READ_ONLY);
		collectionsCombo.setContentProvider(ArrayContentProvider.getInstance());
		collectionsCombo.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection selection = (IStructuredSelection)event.getSelection();
				if (selection.isEmpty()) {
					return;
				}
				Object collectionName = selection.getFirstElement();
				if (collectionName == null) {
					return;
				}
				ImportProvider provider = getWizard().getProvider();
				provider.setParameter(Constants.COLLECTION_NAME, Value.of(collectionName.toString()));
				setPageComplete(true);
			}
		});
		components.applyTo(collectionsCombo.getControl());
		// add a combo to select the max number of elements to consider
		Label maxLabel = new Label(page, SWT.NONE);
		maxLabel.setText("Elements to use: ");
		labels.applyTo(maxLabel);
		maxCombo = new ComboViewer(page, SWT.DROP_DOWN | SWT.READ_ONLY);
		maxCombo.setContentProvider(ArrayContentProvider.getInstance());
		maxCombo.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection selection = (IStructuredSelection)event.getSelection();
				if (selection.isEmpty()) {
					return;
				}
				Object maxElements = selection.getFirstElement();
				if (maxElements == null) {
					return;
				}
				ImportProvider provider = getWizard().getProvider();
				provider.setParameter(Constants.MAX_ELEMENTS, Value.of(maxElements.toString()));
				setPageComplete(true);
			}
		});
		components.applyTo(maxCombo.getControl());
		// specific elements to use
		specificElements = createTextComponent(page, labels, components, "Specific elements (id1;id2;...)", false);
		// update page status
		page.layout(true, true);
		setPageComplete(false);
	}

	@Override
	protected void onShowPage(boolean firstShow) {
		ImportProvider provider = getWizard().getProvider();
		try (Client client = new ClientBuilder().withProvider(provider).build()) {
			// get the available connections
			List<String> collectionsNames = client.getCollectionNames();
			collectionsCombo.getCombo().removeAll();
			collectionsCombo.add(collectionsNames.toArray());
		} catch (Exception exception) {
			LOGGER.error("Error getting collections names from MongoDB.", exception);
			Label label = new Label(page, SWT.WRAP);
			label.setText("Error getting collections names from MongoDB: " + exception.getLocalizedMessage());
			label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
			page.layout(true, true);
			setErrorMessage("Connection error !");
			setPageComplete(true);
		}
		// set max elements
		maxCombo.add(new String[] { "1", "10", "50", "100", "500", "1000", "5000", "10000"});
		maxCombo.setSelection(new StructuredSelection(Collections.singletonList("10")));
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
}
