package org.asf.razorwhip.sentinel.launcher.windows;

import java.awt.FlowLayout;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import javax.swing.event.ListDataListener;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.awt.Dimension;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.ComboBoxModel;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.awt.event.ActionEvent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JComboBox;
import javax.swing.JCheckBox;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.JList;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ListSelectionListener;

import javax.swing.event.ListSelectionEvent;

public class VersionManagerWindow extends JDialog {

	private static final long serialVersionUID = 1l;
	private boolean firstTime = false;

	private JComboBox<AssetArchiveEntry> archiveSelector;
	private JCheckBox checkBoxDownload;
	private JList<ClientEntry> clientListBox;
	private JButton btnOk;

	private ArrayList<AssetArchiveEntry> archives = new ArrayList<AssetArchiveEntry>();
	private ArrayList<ClientEntry> clients = new ArrayList<ClientEntry>();
	private AssetArchiveEntry lastArchive;

	private JLabel lblThanks;

	private boolean wasCancelled = true;
	private boolean warnedArchiveChange = false;

	private class AssetArchiveEntry {
		public String id;
		public String name;
		public boolean deprecated;

		public JsonObject entry;

		@Override
		public String toString() {
			String str = "";
			if (deprecated)
				str += "[Deprecated] ";
			str += name;
			return str;
		}
	}

	private class ClientEntry {
		public String version;

		@Override
		public String toString() {
			return "Version " + version;
		}
	}

	/**
	 * Create the application.
	 */
	public VersionManagerWindow() {
		initialize();
	}

	public VersionManagerWindow(JFrame parent, boolean firstTime) {
		super(parent);
		this.firstTime = firstTime;
		initialize();
	}

	public boolean showDialog() throws IOException {
		clients.clear();
		archives.clear();
		lastArchive = null;

		// Load archive list
		JsonObject archiveLst = JsonParser.parseString(Files.readString(Path.of("assets/assetarchives.json")))
				.getAsJsonObject();
		for (String id : archiveLst.keySet()) {
			JsonObject archiveDef = archiveLst.get(id).getAsJsonObject();

			// Verify
			//
			// Deprecated archives may only be used if they can be downloaded, they should
			// not be streamed from after deprecation
			if ((!archiveDef.get("allowFullDownload").getAsBoolean()
					&& !archiveDef.get("allowStreaming").getAsBoolean())
					|| (!archiveDef.get("allowFullDownload").getAsBoolean() && archiveDef.has("deprecated")
							&& archiveDef.has("deprecationNotice") && archiveDef.get("deprecated").getAsBoolean()))
				continue;

			// Add
			AssetArchiveEntry entry = new AssetArchiveEntry();
			entry.id = id;
			entry.entry = archiveDef;
			entry.name = archiveDef.get("archiveName").getAsString();
			entry.deprecated = archiveDef.has("deprecated") && archiveDef.has("deprecationNotice")
					&& archiveDef.get("deprecated").getAsBoolean();
			archives.add(entry);
		}

		// Load last archive ID from disk
		File localArchiveSettings = new File("assets/localdata.json");
		if (localArchiveSettings.exists()) {
			// Load
			JsonObject settings = JsonParser.parseString(Files.readString(localArchiveSettings.toPath()))
					.getAsJsonObject();
			String id = settings.get("id").getAsString();
			boolean streaming = settings.get("stream").getAsBoolean();
			for (AssetArchiveEntry entry : archives) {
				if (entry.id.equals(id)) {
					lastArchive = entry;
					archiveSelector.setSelectedItem(entry);
					checkBoxDownload.setEnabled(!entry.deprecated && entry.entry.get("allowFullDownload").getAsBoolean()
							&& entry.entry.get("allowStreaming").getAsBoolean());
					break;
				}
			}
			checkBoxDownload.setSelected(!streaming);
		} else if (archives.size() > 0) {
			lastArchive = archives.get(0);
			checkBoxDownload
					.setEnabled(!lastArchive.deprecated && lastArchive.entry.get("allowFullDownload").getAsBoolean()
							&& lastArchive.entry.get("allowStreaming").getAsBoolean());
			checkBoxDownload.setSelected(!lastArchive.entry.get("allowStreaming").getAsBoolean());
		}

		// Set model
		archiveSelector.setModel(new ComboBoxModel<AssetArchiveEntry>() {
			private AssetArchiveEntry selected;

			@Override
			public int getSize() {
				return archives.size();
			}

			@Override
			public AssetArchiveEntry getElementAt(int index) {
				return archives.get(index);
			}

			@Override
			public void setSelectedItem(Object anItem) {
				selected = (AssetArchiveEntry) anItem;
			}

			@Override
			public Object getSelectedItem() {
				return selected;
			}

			@Override
			public void addListDataListener(ListDataListener l) {
			}

			@Override
			public void removeListDataListener(ListDataListener l) {
			}

		});
		archiveSelector.setSelectedItem(lastArchive);

		// Load client list
		JsonArray clientsArr = new JsonArray();
		File clientListFile = new File("assets/clients.json");
		if (clientListFile.exists()) {
			clientsArr = JsonParser.parseString(Files.readString(Path.of("assets/clients.json"))).getAsJsonArray();
		}
		for (JsonElement c : clientsArr) {
			ClientEntry entry = new ClientEntry();
			entry.version = c.getAsString();
			clients.add(entry);
		}

		// Check
		if (archives.size() == 1 && lastArchive != null && firstTime) {
			// Check clients
			JsonObject clients = lastArchive.entry.get("clients").getAsJsonObject();
			if (clients.size() == 1) {
				// Set
				String client = clients.keySet().toArray(t -> new String[t])[0];

				// Write
				boolean found = false;
				for (JsonElement c : clientsArr) {
					if (c.getAsString().equals(client)) {
						found = true;
						break;
					}
				}
				if (!found) {
					// Add and save
					clientsArr.add(client);
					Files.writeString(Path.of("assets/clients.json"), clientsArr.toString());
				}

				// Write settings
				JsonObject settings = new JsonObject();
				settings.addProperty("id", lastArchive.id);
				settings.addProperty("stream", !checkBoxDownload.isSelected());
				Files.writeString(localArchiveSettings.toPath(), settings.toString());

				// Return
				return true;
			}
		}

		// Load client list
		refreshClientList();

		// Show
		setVisible(true);

		// Return
		return !wasCancelled;
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		setTitle("Version manager");
		setBounds(100, 100, 600, 697);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setLocationRelativeTo(null);
		setResizable(false);
		setModal(true);
		getContentPane().setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));

		JPanel panel = new JPanel();
		panel.setPreferredSize(new Dimension(590, 655));
		getContentPane().add(panel);
		panel.setLayout(null);

		btnOk = new JButton("Ok");
		btnOk.setBounds(473, 616, 105, 27);
		panel.add(btnOk);

		JButton btnCancel = new JButton("Cancel");
		btnCancel.setBounds(356, 616, 105, 27);
		panel.add(btnCancel);

		JLabel lblAssetArchive = new JLabel("Asset archive");
		lblAssetArchive.setBounds(12, 12, 566, 17);
		panel.add(lblAssetArchive);

		archiveSelector = new JComboBox<AssetArchiveEntry>();
		archiveSelector.setBounds(12, 34, 566, 26);
		panel.add(archiveSelector);

		checkBoxDownload = new JCheckBox("Download all assets to local device for offline play (WILL TAKE TIME)");
		checkBoxDownload.setSelected(true);
		checkBoxDownload.setEnabled(false);
		checkBoxDownload.setBounds(8, 63, 570, 25);
		panel.add(checkBoxDownload);

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane.setBounds(12, 140, 566, 352);
		panel.add(scrollPane);

		clientListBox = new JList<ClientEntry>();
		clientListBox.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		scrollPane.setViewportView(clientListBox);

		JLabel lblNewLabel = new JLabel("Clients");
		lblNewLabel.setBounds(12, 119, 566, 17);
		panel.add(lblNewLabel);

		JButton btnAdd = new JButton("Add client...");
		btnAdd.setBounds(473, 497, 105, 27);
		panel.add(btnAdd);

		JButton btnRemove = new JButton("Delete");
		btnRemove.setEnabled(false);
		btnRemove.setBounds(12, 497, 105, 27);
		panel.add(btnRemove);

		lblThanks = new JLabel("Assets kindly mirrored by ...");
		lblThanks.setVisible(false);
		lblThanks.setBounds(12, 621, 326, 17);
		panel.add(lblThanks);

		// Add events
		archiveSelector.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() != ItemEvent.SELECTED)
					return;
				SwingUtilities.invokeLater(() -> {
					if (archiveSelector.getSelectedItem() == null) {
						checkBoxDownload.setEnabled(false);
					} else {
						AssetArchiveEntry eA = (AssetArchiveEntry) archiveSelector.getSelectedItem();
						if (lastArchive == null || !eA.id.equals(lastArchive.id)) {
							// Warn about change
							if (!warnedArchiveChange && !firstTime) {
								// Warn
								if (JOptionPane.showConfirmDialog(VersionManagerWindow.this,
										"WARNING! Changing the asset archive will very likely trigger a full re-download!\n\nAre you sure you want to continue?",
										"Warning", JOptionPane.YES_NO_OPTION,
										JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
									archiveSelector.setSelectedItem(lastArchive);
									return;
								}
								warnedArchiveChange = true;
							}

							// Check deprecation
							if (eA.deprecated) {
								String message = eA.entry.get("deprecationNotice").getAsString();
								JOptionPane.showMessageDialog(VersionManagerWindow.this,
										"Warning! The archive you selected has been deprecated and may be removed!\n\n"
												+ message,
										"Deprecated archive", JOptionPane.WARNING_MESSAGE);
							}

							// Change archive
							if (eA.deprecated || !eA.entry.get("allowFullDownload").getAsBoolean()
									|| !eA.entry.get("allowStreaming").getAsBoolean()) {
								checkBoxDownload.setEnabled(false);
								checkBoxDownload
										.setSelected(eA.deprecated || !eA.entry.get("allowStreaming").getAsBoolean());
							} else
								checkBoxDownload.setEnabled(true);
							lastArchive = eA;
							refreshClientList();
						}
					}
				});
			}
		});
		btnCancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});
		btnOk.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					// Write clients
					JsonArray clientsArr = new JsonArray();
					for (ClientEntry client : clients)
						clientsArr.add(client.version);
					Files.writeString(Path.of("assets/clients.json"), clientsArr.toString());

					// Write archive
					File localArchiveSettings = new File("assets/localdata.json");
					JsonObject settings = new JsonObject();
					settings.addProperty("id", lastArchive.id);
					settings.addProperty("stream", !checkBoxDownload.isSelected());
					Files.writeString(localArchiveSettings.toPath(), settings.toString());
				} catch (IOException e1) {
					throw new RuntimeException(e1);
				}

				// Close
				wasCancelled = false;
				dispose();
			}
		});
		clientListBox.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				btnRemove.setEnabled(clientListBox.getSelectedValue() != null);
			}
		});
		btnAdd.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// Create selection list
				JsonObject clientLst = lastArchive.entry.get("clients").getAsJsonObject();
				ArrayList<String> versions = new ArrayList<String>();
				for (String version : clientLst.keySet()) {
					if (!clients.stream().anyMatch(t -> t.version.equals(version))) {
						versions.add(version);
					}
				}
				String ver = (String) JOptionPane.showInputDialog(VersionManagerWindow.this,
						"Select a client version to add...", "Add version", JOptionPane.QUESTION_MESSAGE, null,
						versions.toArray(t -> new Object[t]), null);
				if (ver != null) {
					// Add
					ClientEntry ent = new ClientEntry();
					ent.version = ver;
					clients.add(ent);
					refreshClientList();
				}
			}
		});
		btnRemove.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (JOptionPane.showConfirmDialog(VersionManagerWindow.this,
						"WARNING! Deleting a client will delete all files related to it!\n\nAre you sure you want to continue?",
						"Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
					return;
				}

				// Remove
				ClientEntry entry = (ClientEntry) clientListBox.getSelectedValue();
				clients.remove(entry);
				refreshClientList();
			}
		});

		// Set
		btnOk.setEnabled(lastArchive != null && filteredClientEntryData().length != 0);
	}

	private ClientEntry[] filteredClientEntryData() {
		ArrayList<ClientEntry> filteredData = new ArrayList<ClientEntry>();
		if (lastArchive == null)
			return new ClientEntry[0];
		JsonObject clientLst = lastArchive.entry.get("clients").getAsJsonObject();
		for (ClientEntry entry : clients) {
			// Check
			if (lastArchive != null) {
				if (clientLst.has(entry.version))
					filteredData.add(entry);
			}
		}
		return filteredData.toArray(t -> new ClientEntry[t]);
	}

	private void refreshClientList() {
		ClientEntry[] filteredData = filteredClientEntryData();
		clientListBox.setModel(new ListModel<ClientEntry>() {

			@Override
			public int getSize() {
				return filteredData.length;
			}

			@Override
			public ClientEntry getElementAt(int index) {
				return filteredData[index];
			}

			@Override
			public void addListDataListener(ListDataListener l) {
			}

			@Override
			public void removeListDataListener(ListDataListener l) {
			}

		});
		btnOk.setEnabled(lastArchive != null && filteredData.length != 0);
		lblThanks.setVisible(lastArchive != null);
		if (lastArchive != null)
			lblThanks.setText("Assets kindly mirrored by " + lastArchive.entry.get("thanksTo").getAsString());
	}
}
