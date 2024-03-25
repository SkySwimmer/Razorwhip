package org.asf.razorwhip.sentinel.launcher.windows;

import java.awt.FlowLayout;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import javax.swing.event.ListDataListener;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import java.awt.event.ActionEvent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JComboBox;
import javax.swing.JCheckBox;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.ItemEvent;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.JList;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;

import org.asf.razorwhip.sentinel.launcher.AssetManager;
import org.asf.razorwhip.sentinel.launcher.LauncherUtils;
import org.asf.razorwhip.sentinel.launcher.assets.ArchiveInformation;
import org.asf.razorwhip.sentinel.launcher.assets.ArchiveMode;
import org.asf.razorwhip.sentinel.launcher.assets.AssetInformation;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

public class VersionManagerWindow extends JDialog {

	private static final long serialVersionUID = 1l;
	private boolean firstTime = false;

	private JCheckBox checkBoxDownload;
	private JList<ClientEntry> clientListBox;
	private JComboBox<ArchiveInformation> archiveSelector;
	private JComboBox<QualityLevelEntry> qualityLevelBox;
	private JLabel lblQualityLevels;
	private JButton btnExport;
	private JButton btnImport;
	private JButton btnAdd;
	private JButton btnRemove;
	private JButton btnCancel;
	private JButton btnOk;

	private boolean isFirstSelection;

	private ArrayList<QualityLevelEntry> qualityLevelElements = new ArrayList<QualityLevelEntry>();
	private ArrayList<ClientEntry> clients = new ArrayList<ClientEntry>();
	private ArchiveInformation lastArchive;

	private boolean downloadFinished;

	private JLabel lblThanks;

	private boolean wasCancelled = true;
	private boolean warnedArchiveChange = false;

	private boolean updateDescriptor = true;
	private JButton btnDelete;

	private class QualityLevelEntry {
		public String level;
		public boolean enabled = true;
		public JCheckBox checkBox;

		@Override
		public String toString() {
			return level;
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
		setLocationRelativeTo(parent);
	}

	public boolean showDialog() throws IOException {
		clients.clear();
		lastArchive = null;
		isFirstSelection = false;

		// Load last archive ID from disk
		File localArchiveSettings = new File("assets/localdata.json");
		if (localArchiveSettings.exists()) {
			// Load
			JsonObject settings = JsonParser.parseString(Files.readString(localArchiveSettings.toPath()))
					.getAsJsonObject();
			String id = settings.get("id").getAsString();
			boolean streaming = settings.get("stream").getAsBoolean();
			for (ArchiveInformation entry : AssetManager.getArchives()) {
				if (entry.archiveID.equals(id)) {
					lastArchive = entry;
					archiveSelector.setSelectedItem(entry);
					checkBoxDownload
							.setEnabled(!entry.isDeprecated && entry.supportsDownloads && entry.supportsStreaming);
					lblQualityLevels.setVisible(checkBoxDownload.isSelected());
					qualityLevelBox.setVisible(checkBoxDownload.isSelected());
					btnExport.setEnabled(lastArchive != null && lastArchive.mode == ArchiveMode.REMOTE
							&& lastArchive.supportsDownloads);
					btnDelete.setVisible(lastArchive != null && lastArchive.isUserArchive);
					btnDelete.setEnabled(true);
					break;
				}
			}
			checkBoxDownload.setSelected(!streaming);
		} else if (AssetManager.getArchives().length > 0) {
			lastArchive = AssetManager.getArchives()[0];
			isFirstSelection = true;
			checkBoxDownload.setVisible(lastArchive.mode == ArchiveMode.REMOTE);
			checkBoxDownload.setEnabled(
					!lastArchive.isDeprecated && lastArchive.supportsDownloads && lastArchive.supportsStreaming);
			checkBoxDownload.setSelected(!lastArchive.supportsStreaming);
			lblQualityLevels.setVisible(checkBoxDownload.isSelected());
			qualityLevelBox.setVisible(checkBoxDownload.isSelected());
			btnExport.setEnabled(
					lastArchive != null && lastArchive.mode == ArchiveMode.REMOTE && lastArchive.supportsDownloads);
			btnDelete.setVisible(lastArchive != null && lastArchive.isUserArchive);
			btnDelete.setEnabled(true);
		}

		// Set model
		archiveSelector.setModel(new ComboBoxModel<ArchiveInformation>() {
			private ArchiveInformation selected;

			@Override
			public int getSize() {
				return AssetManager.getArchives().length;
			}

			@Override
			public ArchiveInformation getElementAt(int index) {
				return AssetManager.getArchives()[index];
			}

			@Override
			public void setSelectedItem(Object anItem) {
				selected = (ArchiveInformation) anItem;
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
		if (AssetManager.getArchives().length == 1 && lastArchive != null && firstTime) {
			// Check clients
			JsonObject clients = lastArchive.archiveClientLst;
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
				settings.addProperty("id", lastArchive.archiveID);
				settings.addProperty("stream", !checkBoxDownload.isSelected());
				Files.writeString(localArchiveSettings.toPath(), settings.toString());

				// Return
				return true;
			}
		}

		// Mark as update required
		updateDescriptor = true;

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
		setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		setLocationRelativeTo(null);
		setResizable(false);
		setModal(true);
		getContentPane().setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				if (!btnCancel.isEnabled()) {
					// Warn
					if (JOptionPane.showConfirmDialog(VersionManagerWindow.this,
							"WARNING! The version manager is presently busy!\nClosing the version manager while its busy WILL RESTART THE LAUNCHER!\n\nAre you sure you want to continue?",
							"Warning", JOptionPane.YES_NO_OPTION,
							JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
						return;
					}
					System.exit(237);
					return;
				}
				if (JOptionPane.showConfirmDialog(VersionManagerWindow.this,
						"Are you sure you want to close the version manager without saving?", "Warning",
						JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
					return;
				}
				dispose();
			}
		});

		JPanel panel = new JPanel();
		panel.setPreferredSize(new Dimension(590, 655));
		getContentPane().add(panel);
		panel.setLayout(null);

		btnOk = new JButton("Ok");
		btnOk.setBounds(473, 616, 105, 27);
		panel.add(btnOk);

		btnCancel = new JButton("Cancel");
		btnCancel.setBounds(356, 616, 105, 27);
		panel.add(btnCancel);

		JLabel lblAssetArchive = new JLabel("Asset archive");
		lblAssetArchive.setBounds(12, 12, 566, 17);
		panel.add(lblAssetArchive);

		archiveSelector = new JComboBox<ArchiveInformation>();
		archiveSelector.setBounds(12, 34, 350, 26);
		panel.add(archiveSelector);

		btnImport = new JButton("Add...");
		btnImport.setBounds(473, 34, 105, 26);
		panel.add(btnImport);

		btnExport = new JButton("Export...");
		btnExport.setBounds(366, 34, 105, 26);
		panel.add(btnExport);

		btnDelete = new JButton("Remove");
		btnDelete.setBounds(473, 62, 105, 26);
		panel.add(btnDelete);

		checkBoxDownload = new JCheckBox("Download all assets to local device for offline play (WILL TAKE TIME)");
		checkBoxDownload.setSelected(true);
		checkBoxDownload.setEnabled(false);
		btnExport.setEnabled(
				lastArchive != null && lastArchive.mode == ArchiveMode.REMOTE && lastArchive.supportsDownloads);
		btnDelete.setVisible(lastArchive != null && lastArchive.isUserArchive);
		checkBoxDownload.setBounds(8, 63, 459, 25);
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

		lblQualityLevels = new JLabel("Quality levels to download");
		lblQualityLevels.setBounds(356, 554, 222, 17);
		panel.add(lblQualityLevels);

		qualityLevelBox = new JComboBox<QualityLevelEntry>() {
			private static final long serialVersionUID = 1L;

			@Override
			public void setPopupVisible(boolean visible) {
				if (visible) {
					super.setPopupVisible(visible);
				}
			}
		};
		qualityLevelBox.setBounds(356, 577, 222, 27);
		panel.add(qualityLevelBox);

		lblQualityLevels.setVisible(checkBoxDownload.isSelected());
		qualityLevelBox.setVisible(checkBoxDownload.isSelected());
		checkBoxDownload.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				lblQualityLevels.setVisible(checkBoxDownload.isSelected());
				qualityLevelBox.setVisible(checkBoxDownload.isSelected());
			}
		});

		String[] levels = LauncherUtils.getGameDescriptor().knownAssetQualityLevels();
		loadQualityLevelBox(qualityLevelBox, levels);

		btnAdd = new JButton("Add client...");
		btnAdd.setBounds(473, 497, 105, 27);
		panel.add(btnAdd);

		btnRemove = new JButton("Delete");
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
						lblQualityLevels.setVisible(false);
						qualityLevelBox.setVisible(false);
						btnExport.setEnabled(false);
						btnDelete.setVisible(false);
					} else {
						ArchiveInformation eA = (ArchiveInformation) archiveSelector.getSelectedItem();
						if (lastArchive == null || !eA.archiveID.equals(lastArchive.archiveID)) {
							// Warn about change
							if (lastArchive != null && !warnedArchiveChange && !firstTime) {
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

							// Update descriptor
							updateDescriptor = true;

							// Check deprecation
							if (eA.isDeprecated) {
								String message = eA.deprecationNotice;
								JOptionPane.showMessageDialog(VersionManagerWindow.this,
										"Warning! The archive you selected has been deprecated and may be removed!\n\n"
												+ message,
										"Deprecated archive", JOptionPane.WARNING_MESSAGE);
							}

							// Change archive
							if (eA.isDeprecated || !eA.supportsDownloads || !eA.supportsStreaming) {
								checkBoxDownload.setEnabled(false);
								checkBoxDownload.setSelected(eA.isDeprecated || !eA.supportsStreaming);
								lblQualityLevels.setVisible(checkBoxDownload.isSelected());
								qualityLevelBox.setVisible(checkBoxDownload.isSelected());
							} else {
								checkBoxDownload.setEnabled(true);
								lblQualityLevels.setVisible(checkBoxDownload.isSelected());
								qualityLevelBox.setVisible(checkBoxDownload.isSelected());
							}
							isFirstSelection = false;
							lastArchive = eA;
							checkBoxDownload.setVisible(lastArchive.mode == ArchiveMode.REMOTE);
							btnExport
									.setText(lastArchive == null || lastArchive.mode == ArchiveMode.REMOTE ? "Export..."
											: "Delete");
							btnExport.setEnabled(lastArchive != null && lastArchive.mode == ArchiveMode.REMOTE
									&& lastArchive.supportsDownloads);
							btnDelete.setVisible(lastArchive != null && lastArchive.isUserArchive);
							btnDelete.setEnabled(true);
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
				// Check deprecation
				if (isFirstSelection && lastArchive.isDeprecated) {
					String message = lastArchive.deprecationNotice;
					if (JOptionPane.showConfirmDialog(VersionManagerWindow.this,
							"Warning! The archive you selected has been deprecated and may be removed!\n\n" + message
									+ "\n\nDo you want to continue?",
							"Deprecated archive", JOptionPane.YES_NO_OPTION,
							JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
						// Cancel
						return;
					}
				}

				// Check
				if (checkBoxDownload.isSelected()) {
					// Disable
					boolean wasEnabledRemove = btnRemove.isEnabled();
					btnOk.setEnabled(false);
					qualityLevelBox.setEnabled(false);
					btnCancel.setEnabled(false);
					archiveSelector.setEnabled(false);
					btnAdd.setEnabled(false);
					boolean chBoxWasEnabled = checkBoxDownload.isEnabled();
					checkBoxDownload.setEnabled(false);
					clientListBox.setEnabled(false);
					btnExport.setEnabled(false);
					btnDelete.setEnabled(false);
					btnImport.setEnabled(false);
					btnRemove.setEnabled(false);

					// Set text
					btnOk.setText("Busy...");

					// Collect assets
					Thread th = new Thread(() -> {
						try {
							// Update descriptor
							if (!updateDescriptorIfNeeded(chBoxWasEnabled, wasEnabledRemove))
								return;

							// Load descriptor
							JsonObject archiveDescriptor = JsonParser
									.parseString(Files.readString(Path.of("assets/descriptor/descriptor.json")))
									.getAsJsonObject();

							// Collect changed assets and find size
							SwingUtilities.invokeAndWait(() -> {
								lblThanks.setText("Indexing assets...");
								lblThanks.setVisible(true);
							});
							LauncherUtils.log("Indexing assets... Please wait...", true);
							HashMap<String, String> assetHashes = new HashMap<String, String>();
							HashMap<String, AssetInformation> assetsList = new LinkedHashMap<String, AssetInformation>();
							indexAssetHashes(assetHashes, new File("assets/descriptor/hashes.shl"));
							for (String path : assetHashes.keySet()) {
								AssetInformation asset = new AssetInformation();
								asset.assetPath = path;
								asset.assetHash = assetHashes.get(path);
								asset.localAssetFile = new File("assets/assetarchive/assets", asset.assetHash + ".sa");
								assetsList.put(asset.assetPath.toLowerCase(), asset);
							}

							// Collect versions
							LauncherUtils.log("Collecting game clients...");
							ArrayList<String> versions = new ArrayList<String>();
							for (ClientEntry client : clients) {
								if (lastArchive.archiveClientLst.has(client.version))
									versions.add(client.version);
							}

							// Collect assets
							SwingUtilities.invokeAndWait(() -> {
								lblThanks.setText("Collecting assets...");
								lblThanks.setVisible(true);
							});
							LauncherUtils.log("Collecting assets...", true);
							Map<String, AssetInformation> assets = new LinkedHashMap<String, AssetInformation>();
							for (String clientVersion : versions) {
								SwingUtilities.invokeAndWait(() -> {
									lblThanks.setText("Collecting assets of " + clientVersion + "...");
									lblThanks.setVisible(true);
								});
								ArrayList<String> lvls = new ArrayList<String>();
								for (QualityLevelEntry lv : qualityLevelElements) {
									if (lv.enabled)
										lvls.add(lv.level);
								}
								LauncherUtils.log("Collecting assets of " + clientVersion + "...");
								AssetInformation[] collectedAssets = LauncherUtils.getGameDescriptor()
										.collectVersionAssets(assetsList.values().toArray(t -> new AssetInformation[t]),
												lvls.toArray(t -> new String[t]), clientVersion, lastArchive,
												lastArchive.archiveDef, archiveDescriptor, assetHashes);
								for (AssetInformation asset : collectedAssets) {
									// Check if present
									if (!assets.containsKey(asset.assetHash.toLowerCase())) {
										AssetInformation as = new AssetInformation();
										as.assetHash = asset.assetHash;
										as.assetPath = asset.assetPath;
										as.localAssetFile = asset.localAssetFile;
										assets.put(asset.assetHash.toLowerCase(), as);
									}
								}
							}

							// Verify
							SwingUtilities.invokeAndWait(() -> {
								lblThanks.setText("Collecting updated assets...");
								lblThanks.setVisible(true);
							});
							LauncherUtils.log("Collecting updated assets...", true);
							Map<String, AssetInformation> assetsNeedingDownloads = new LinkedHashMap<String, AssetInformation>();
							for (AssetInformation asset : assets.values()) {
								if (!asset.isUpToDate()) {
									// Add asset
									if (!assetsNeedingDownloads.containsKey(asset.assetHash))
										assetsNeedingDownloads.put(asset.assetHash, asset);
								}
							}
							LauncherUtils.log("Collected " + assetsNeedingDownloads.size() + " updated asset(s)", true);
							if (assetsNeedingDownloads.size() != 0) {
								// Index
								SwingUtilities.invokeAndWait(() -> {
									lblThanks.setText("Collecting asset size...");
									lblThanks.setVisible(true);
								});
								HashMap<String, Long> assetSizes = new HashMap<String, Long>();
								indexAssetSizes(assetSizes, new File("assets/descriptor/index.sfl"));

								// Find size
								long size = 0;
								for (AssetInformation asset : assetsNeedingDownloads.values()) {
									size += assetSizes.getOrDefault(asset.assetPath, 0l);
								}

								// Pretty-print
								String sizeStr = size + "b";
								if (size >= 1024) {
									size = size / 1024;
									sizeStr = size + "kb";
								}
								if (size >= 1024) {
									size = size / 1024;
									sizeStr = size + "mb";
								}
								if (size >= 1024) {
									size = size / 1024;
									sizeStr = size + "gb";
								}

								// Update
								String sizeStrF = sizeStr;
								SwingUtilities.invokeAndWait(() -> {
									if (lastArchive.mode == ArchiveMode.REMOTE)
										lblThanks.setText("Need to download " + sizeStrF);
									else
										lblThanks.setText("Need to extract " + sizeStrF);
									lblThanks.setVisible(true);
								});

								// Notify
								if (JOptionPane.showConfirmDialog(VersionManagerWindow.this,
										(lastArchive.mode == ArchiveMode.REMOTE
												? ("WARNING! This operation will download " + sizeStr
														+ " of asset data!\n\nAre you sure you want to continue?")
												: ("WARNING! This operation will extract " + sizeStr
														+ " of asset data!\n\nAre you sure you want to continue?")),
										"Warning", JOptionPane.YES_NO_OPTION,
										JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {

									// Re-enable
									SwingUtilities.invokeLater(() -> {
										btnOk.setEnabled(true);
										btnCancel.setEnabled(true);
										archiveSelector.setEnabled(true);
										btnAdd.setEnabled(true);
										checkBoxDownload.setVisible(lastArchive.mode == ArchiveMode.REMOTE);
										checkBoxDownload.setEnabled(chBoxWasEnabled);
										btnExport.setEnabled(
												lastArchive != null && lastArchive.mode == ArchiveMode.REMOTE
														&& lastArchive.supportsDownloads);
										btnDelete.setVisible(lastArchive != null && lastArchive.isUserArchive);
										btnDelete.setEnabled(true);
										btnImport.setEnabled(true);
										qualityLevelBox.setEnabled(true);
										clientListBox.setEnabled(true);
										btnRemove.setEnabled(wasEnabledRemove);
										btnOk.setText("Ok");
										lblThanks.setVisible(lastArchive != null);
										lblThanks.setText("");
										if (lastArchive != null && lastArchive.archiveDef.has("thanksTo"))
											lblThanks.setText("Assets kindly mirrored by "
													+ lastArchive.archiveDef.get("thanksTo").getAsString());
									});

									return;
								}
							}

							// Save
							SwingUtilities.invokeLater(() -> {
								// Re-enable
								btnOk.setEnabled(true);
								btnCancel.setEnabled(true);
								archiveSelector.setEnabled(true);
								btnAdd.setEnabled(true);
								checkBoxDownload.setVisible(lastArchive.mode == ArchiveMode.REMOTE);
								checkBoxDownload.setEnabled(chBoxWasEnabled);
								btnExport.setEnabled(lastArchive != null && lastArchive.mode == ArchiveMode.REMOTE
										&& lastArchive.supportsDownloads);
								btnDelete.setVisible(lastArchive != null && lastArchive.isUserArchive);
								btnDelete.setEnabled(true);
								btnImport.setEnabled(true);
								qualityLevelBox.setEnabled(true);
								clientListBox.setEnabled(true);
								btnRemove.setEnabled(wasEnabledRemove);
								btnOk.setText("Ok");
								lblThanks.setVisible(lastArchive != null);
								lblThanks.setText("");
								if (lastArchive != null && lastArchive.archiveDef.has("thanksTo"))
									lblThanks.setText("Assets kindly mirrored by "
											+ lastArchive.archiveDef.get("thanksTo").getAsString());
								doSave();
							});
						} catch (Exception e2) {
							// Error
							String stackTrace = "";
							Throwable t = e2;
							while (t != null) {
								for (StackTraceElement ele : t.getStackTrace())
									stackTrace += "\n     At: " + ele;
								t = t.getCause();
								if (t != null)
									stackTrace += "\nCaused by: " + t;
							}
							JOptionPane.showMessageDialog(VersionManagerWindow.this,
									"An error occurred!\n\nError details: " + e2 + stackTrace, "Launcher error",
									JOptionPane.ERROR_MESSAGE);
							downloadFinished = true;

							// Re-enable
							SwingUtilities.invokeLater(() -> {
								btnOk.setEnabled(true);
								btnCancel.setEnabled(true);
								archiveSelector.setEnabled(true);
								btnAdd.setEnabled(true);
								checkBoxDownload.setVisible(lastArchive.mode == ArchiveMode.REMOTE);
								checkBoxDownload.setEnabled(chBoxWasEnabled);
								btnExport.setEnabled(lastArchive != null && lastArchive.mode == ArchiveMode.REMOTE
										&& lastArchive.supportsDownloads);
								btnDelete.setVisible(lastArchive != null && lastArchive.isUserArchive);
								btnDelete.setEnabled(true);
								btnImport.setEnabled(true);
								qualityLevelBox.setEnabled(true);
								clientListBox.setEnabled(true);
								btnRemove.setEnabled(wasEnabledRemove);
								btnOk.setText("Ok");
								lblThanks.setVisible(lastArchive != null);
								lblThanks.setText("");
								if (lastArchive != null && lastArchive.archiveDef.has("thanksTo"))
									lblThanks.setText("Assets kindly mirrored by "
											+ lastArchive.archiveDef.get("thanksTo").getAsString());
							});
						}
					});
					th.setDaemon(true);
					th.start();
					return;
				}

				// Make sure descriptor gets re-downloaded
				if (new File("assets/descriptor.hash").exists())
					new File("assets/descriptor.hash").delete();
				if (new File("assets/descriptor-local.version").exists())
					new File("assets/descriptor-local.version").delete();

				// Close
				doSave();
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
				JsonObject clientLst = lastArchive.archiveClientLst;
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
		btnExport.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (lastArchive == null)
					return;
				if (lastArchive.mode == ArchiveMode.REMOTE) {
					// Export

					// Show selection
					int selected = JOptionPane.showOptionDialog(VersionManagerWindow.this,
							"Welcome to the archive export tool!\nHere you can create a archive SGA file or server folder based on your current archive settings!\n\nWARNING! Please note that exporting an archive WILL SAVE THE CURRENT ARCHIVE SETTINGS,\nIT WILL ALSO DOWNLOAD ALL ASSETS WITH ALL QUALITY LEVELS REGARDLESS OF QUALITY SETTINGS.\n\nPlease select a operation...\n ",
							"Archive Exporter", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null,
							new Object[] { "Export as SGA file", "Create archive server folder", "Cancel" }, "Cancel");

					// Check cancel
					if (selected == 2 || selected == -1)
						return;

					// Disable
					boolean wasEnabledRemove = btnRemove.isEnabled();
					btnOk.setEnabled(false);
					qualityLevelBox.setEnabled(false);
					btnCancel.setEnabled(false);
					archiveSelector.setEnabled(false);
					btnAdd.setEnabled(false);
					boolean chBoxWasEnabled = checkBoxDownload.isEnabled();
					checkBoxDownload.setEnabled(false);
					clientListBox.setEnabled(false);
					btnExport.setEnabled(false);
					btnImport.setEnabled(false);
					btnRemove.setEnabled(false);
					btnDelete.setEnabled(false);

					// Set text
					btnOk.setText("Busy...");

					// Collect assets
					Thread th = new Thread(() -> {
						try {
							// Save
							SwingUtilities.invokeAndWait(() -> {
								lblThanks.setText("Saving settings...");
								lblThanks.setVisible(true);
							});
							LauncherUtils.log("Saving settings...", true);
							saveSettings();

							// Reload
							SwingUtilities.invokeAndWait(() -> {
								lblThanks.setText("Reloading asset manager...");
								lblThanks.setVisible(true);
							});
							LauncherUtils.log("Reloading asset manager...", true);
							AssetManager.reloadSavedSettings();

							// Update descriptor
							if (!updateDescriptorIfNeeded(chBoxWasEnabled, wasEnabledRemove))
								return;

							// Load descriptor
							JsonObject archiveDescriptor = JsonParser
									.parseString(Files.readString(Path.of("assets/descriptor/descriptor.json")))
									.getAsJsonObject();

							// Collect changed assets and find size
							SwingUtilities.invokeAndWait(() -> {
								lblThanks.setText("Indexing assets...");
								lblThanks.setVisible(true);
							});
							LauncherUtils.log("Indexing assets... Please wait...", true);
							HashMap<String, String> assetHashes = new HashMap<String, String>();
							HashMap<String, AssetInformation> assetsList = new LinkedHashMap<String, AssetInformation>();
							indexAssetHashes(assetHashes, new File("assets/descriptor/hashes.shl"));
							for (String path : assetHashes.keySet()) {
								AssetInformation asset = new AssetInformation();
								asset.assetPath = path;
								asset.assetHash = assetHashes.get(path);
								asset.localAssetFile = new File("assets/assetarchive/assets", asset.assetHash + ".sa");
								assetsList.put(asset.assetPath.toLowerCase(), asset);
							}

							// Collect versions
							LauncherUtils.log("Collecting game clients...");
							ArrayList<String> versions = new ArrayList<String>();
							for (ClientEntry client : clients) {
								if (lastArchive.archiveClientLst.has(client.version))
									versions.add(client.version);
							}

							// Collect assets
							SwingUtilities.invokeAndWait(() -> {
								lblThanks.setText("Collecting assets...");
								lblThanks.setVisible(true);
							});
							LauncherUtils.log("Collecting assets...", true);
							Map<String, AssetInformation> assets = new LinkedHashMap<String, AssetInformation>();
							for (String clientVersion : versions) {
								SwingUtilities.invokeAndWait(() -> {
									lblThanks.setText("Collecting assets of " + clientVersion + "...");
									lblThanks.setVisible(true);
								});
								LauncherUtils.log("Collecting assets of " + clientVersion + "...");
								AssetInformation[] collectedAssets = LauncherUtils.getGameDescriptor()
										.collectVersionAssets(assetsList.values().toArray(t -> new AssetInformation[t]),
												LauncherUtils.getGameDescriptor().knownAssetQualityLevels(),
												clientVersion, lastArchive, lastArchive.archiveDef, archiveDescriptor,
												assetHashes);
								for (AssetInformation asset : collectedAssets) {
									// Check if present
									if (!assets.containsKey(asset.assetHash.toLowerCase())) {
										AssetInformation as = new AssetInformation();
										as.assetHash = asset.assetHash;
										as.assetPath = asset.assetPath;
										as.localAssetFile = asset.localAssetFile;
										as.clientVersions = new String[] { clientVersion };
										assets.put(asset.assetPath.toLowerCase(), as);
									} else {
										AssetInformation as = assets.get(asset.assetPath.toLowerCase());
										if (!Stream.of(as.clientVersions)
												.anyMatch(t -> t.equalsIgnoreCase(clientVersion)))
											as.clientVersions = appendToStringArray(as.clientVersions, clientVersion);
									}
								}
							}

							// Verify
							SwingUtilities.invokeAndWait(() -> {
								lblThanks.setText("Collecting updated assets...");
								lblThanks.setVisible(true);
							});
							LauncherUtils.log("Collecting updated assets...", true);
							ArrayList<String> clientsNeedingUpdates = new ArrayList<String>();
							Map<String, AssetInformation> assetsNeedingDownloads = new LinkedHashMap<String, AssetInformation>();
							for (AssetInformation asset : assets.values()) {
								if (!asset.isUpToDate()) {
									// Add clients
									for (String version : asset.clientVersions) {
										if (!clientsNeedingUpdates.contains(version))
											clientsNeedingUpdates.add(version);
									}

									// Add asset
									if (!assetsNeedingDownloads.containsKey(asset.assetHash))
										assetsNeedingDownloads.put(asset.assetHash, asset);
								}
							}
							LauncherUtils.log("Collected " + assetsNeedingDownloads.size() + " updated asset(s)", true);
							if (assetsNeedingDownloads.size() != 0) {
								// Index
								SwingUtilities.invokeAndWait(() -> {
									lblThanks.setText("Collecting asset size...");
									lblThanks.setVisible(true);
								});
								HashMap<String, Long> assetSizes = new HashMap<String, Long>();
								indexAssetSizes(assetSizes, new File("assets/descriptor/index.sfl"));

								// Find size
								long size = 0;
								for (AssetInformation asset : assetsNeedingDownloads.values()) {
									size += assetSizes.getOrDefault(asset.assetPath, 0l);
								}

								// Pretty-print
								String sizeStr = size + "b";
								if (size >= 1024) {
									size = size / 1024;
									sizeStr = size + "kb";
								}
								if (size >= 1024) {
									size = size / 1024;
									sizeStr = size + "mb";
								}
								if (size >= 1024) {
									size = size / 1024;
									sizeStr = size + "gb";
								}

								// Update
								String sizeStrF = sizeStr;
								SwingUtilities.invokeAndWait(() -> {
									if (lastArchive.mode == ArchiveMode.REMOTE)
										lblThanks.setText("Need to download " + sizeStrF);
									else
										lblThanks.setText("Need to extract " + sizeStrF);
									lblThanks.setVisible(true);
								});

								// Notify
								if (JOptionPane.showConfirmDialog(VersionManagerWindow.this,
										(lastArchive.mode == ArchiveMode.REMOTE
												? ("WARNING! This operation will download " + sizeStr
														+ " of asset data!\n\nAre you sure you want to continue?")
												: ("WARNING! This operation will extract " + sizeStr
														+ " of asset data!\n\nAre you sure you want to continue?")),
										"Warning", JOptionPane.YES_NO_OPTION,
										JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {

									// Re-enable
									SwingUtilities.invokeLater(() -> {
										btnOk.setEnabled(true);
										btnCancel.setEnabled(true);
										archiveSelector.setEnabled(true);
										btnAdd.setEnabled(true);
										checkBoxDownload.setVisible(lastArchive.mode == ArchiveMode.REMOTE);
										checkBoxDownload.setEnabled(chBoxWasEnabled);
										btnExport.setEnabled(
												lastArchive != null && lastArchive.mode == ArchiveMode.REMOTE
														&& lastArchive.supportsDownloads);
										btnDelete.setVisible(lastArchive != null && lastArchive.isUserArchive);
										btnDelete.setEnabled(true);
										btnImport.setEnabled(true);
										qualityLevelBox.setEnabled(true);
										clientListBox.setEnabled(true);
										btnRemove.setEnabled(wasEnabledRemove);
										btnOk.setText("Ok");
										lblThanks.setVisible(lastArchive != null);
										lblThanks.setText("");
										if (lastArchive != null && lastArchive.archiveDef.has("thanksTo"))
											lblThanks.setText("Assets kindly mirrored by "
													+ lastArchive.archiveDef.get("thanksTo").getAsString());
									});

									return;
								}
							}

							// Check mode
							SwingUtilities.invokeAndWait(() -> {
								lblThanks.setText("Preparing to export...");
								lblThanks.setVisible(true);
							});
							LauncherUtils.setStatus("Preparing to export...");
							LauncherUtils.log("Determining mode...");
							File outputFile = null;
							if (selected == 0) {
								// SGA
								LauncherUtils.log("SGA mode, requesting user save file selection...");
								SwingUtilities.invokeAndWait(() -> {
									lblThanks.setText("Awaiting user selection...");
									lblThanks.setVisible(true);
								});
								LauncherUtils.log("Awaiting user selection...");
								JFileChooser f = new JFileChooser();
								f.setDialogTitle("Choose where to save...");
								FileFilter filter = new FileFilter() {

									@Override
									public boolean accept(File f) {
										return f.isDirectory() || f.getName().endsWith(".sga");
									}

									@Override
									public String getDescription() {
										return "Sentinel game asset archive files (*.sga)";
									}

								};
								f.addChoosableFileFilter(filter);
								f.setFileFilter(filter);
								f.showSaveDialog(VersionManagerWindow.this);
								if (f.getSelectedFile() != null) {
									outputFile = f.getSelectedFile();
								}
							} else if (selected == 1) {
								// Server file
								LauncherUtils.log("Server folder mode, requesting user save file selection...");
								SwingUtilities.invokeAndWait(() -> {
									lblThanks.setText("Awaiting user selection...");
									lblThanks.setVisible(true);
								});
								LauncherUtils.log("Awaiting user selection...");
								JFileChooser f = new JFileChooser();
								f.setDialogTitle("Choose where to save...");
								f.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
								f.showSaveDialog(VersionManagerWindow.this);
								if (f.getSelectedFile() != null) {
									outputFile = f.getSelectedFile();
								}
							}

							// Cancel
							if (outputFile == null) {
								SwingUtilities.invokeLater(() -> {
									// Re-enable
									downloadFinished = true;
									btnOk.setEnabled(true);
									btnCancel.setEnabled(true);
									archiveSelector.setEnabled(true);
									btnAdd.setEnabled(true);
									checkBoxDownload.setVisible(lastArchive.mode == ArchiveMode.REMOTE);
									checkBoxDownload.setEnabled(chBoxWasEnabled);
									btnExport.setEnabled(lastArchive != null && lastArchive.mode == ArchiveMode.REMOTE
											&& lastArchive.supportsDownloads);
									btnDelete.setVisible(lastArchive != null && lastArchive.isUserArchive);
									btnDelete.setEnabled(true);
									btnImport.setEnabled(true);
									qualityLevelBox.setEnabled(true);
									clientListBox.setEnabled(true);
									btnRemove.setEnabled(wasEnabledRemove);
									btnOk.setText("Ok");
									lblThanks.setVisible(lastArchive != null);
									lblThanks.setText("");
									if (lastArchive != null && lastArchive.archiveDef.has("thanksTo"))
										lblThanks.setText("Assets kindly mirrored by "
												+ lastArchive.archiveDef.get("thanksTo").getAsString());
								});
								return;
							}

							// Make sure the label updates
							downloadFinished = false;
							Thread th2 = new Thread(() -> {
								String lastMsg = "";
								while (!downloadFinished) {
									try {
										String pref = LauncherUtils.getStatusMessage();
										int percentage = (int) ((100f / (float) (LauncherUtils.getProgressMax()))
												* (float) LauncherUtils.getProgress());
										String suff = " [" + percentage + "%]";
										if (!LauncherUtils.isProgressPanelVisible())
											suff = "";
										String sF = suff;
										if (!lastMsg.equals(pref + suff)) {
											lastMsg = pref + suff;
											SwingUtilities.invokeAndWait(() -> {
												if (!downloadFinished) {
													lblThanks.setText(pref + sF);
												}
											});
										}
										Thread.sleep(100);
									} catch (InvocationTargetException | InterruptedException e1) {
										break;
									}
								}
							});
							th2.setDaemon(true);
							th2.start();

							// Prepare assets
							LauncherUtils.log("Preparing assets...", true);
							AssetManager.migrateAssetsIfNeeded();
							LauncherUtils.hideProgressPanel();
							LauncherUtils.resetProgressBar();

							// Download clients
							LauncherUtils.setStatus("Checking for updates...");
							try {
								AssetManager.verifyClients(true);
								LauncherUtils.hideProgressPanel();
								LauncherUtils.resetProgressBar();
							} catch (IOException e2) {
								LauncherUtils.hideProgressPanel();
								LauncherUtils.resetProgressBar();

								// Show error
								JOptionPane.showMessageDialog(VersionManagerWindow.this,
										"Server connection could not be established with the archive server!\n\nCannot export as there are clients that are not up to date!",
										"No server connection", JOptionPane.ERROR_MESSAGE);

								// Reset
								SwingUtilities.invokeLater(() -> {
									// Re-enable
									downloadFinished = true;
									btnOk.setEnabled(true);
									btnCancel.setEnabled(true);
									archiveSelector.setEnabled(true);
									btnAdd.setEnabled(true);
									checkBoxDownload.setVisible(lastArchive.mode == ArchiveMode.REMOTE);
									checkBoxDownload.setEnabled(chBoxWasEnabled);
									btnExport.setEnabled(lastArchive != null && lastArchive.mode == ArchiveMode.REMOTE
											&& lastArchive.supportsDownloads);
									btnDelete.setVisible(lastArchive != null && lastArchive.isUserArchive);
									btnDelete.setEnabled(true);
									btnImport.setEnabled(true);
									qualityLevelBox.setEnabled(true);
									clientListBox.setEnabled(true);
									btnRemove.setEnabled(wasEnabledRemove);
									btnOk.setText("Ok");
									lblThanks.setVisible(lastArchive != null);
									lblThanks.setText("");
									if (lastArchive != null && lastArchive.archiveDef.has("thanksTo"))
										lblThanks.setText("Assets kindly mirrored by "
												+ lastArchive.archiveDef.get("thanksTo").getAsString());
								});
								return;
							}

							// Verify assets
							LauncherUtils.log("Verifying assets...", true);
							if (assetsNeedingDownloads.size() != 0) {
								// Test connection
								if (!AssetManager.testArchiveConnection(lastArchive)) {
									// Error
									JOptionPane.showMessageDialog(VersionManagerWindow.this,
											"Server connection could not be established with the archive server!\n\nCannot export as there are assets that need to be updated!",
											"No server connection", JOptionPane.ERROR_MESSAGE);

									// Reset
									SwingUtilities.invokeLater(() -> {
										// Re-enable
										downloadFinished = true;
										btnOk.setEnabled(true);
										btnCancel.setEnabled(true);
										archiveSelector.setEnabled(true);
										btnAdd.setEnabled(true);
										checkBoxDownload.setVisible(lastArchive.mode == ArchiveMode.REMOTE);
										checkBoxDownload.setEnabled(chBoxWasEnabled);
										btnExport.setEnabled(
												lastArchive != null && lastArchive.mode == ArchiveMode.REMOTE
														&& lastArchive.supportsDownloads);
										btnDelete.setVisible(lastArchive != null && lastArchive.isUserArchive);
										btnDelete.setEnabled(true);
										btnImport.setEnabled(true);
										qualityLevelBox.setEnabled(true);
										clientListBox.setEnabled(true);
										btnRemove.setEnabled(wasEnabledRemove);
										btnOk.setText("Ok");
										lblThanks.setVisible(lastArchive != null);
										lblThanks.setText("");
										if (lastArchive != null && lastArchive.archiveDef.has("thanksTo"))
											lblThanks.setText("Assets kindly mirrored by "
													+ lastArchive.archiveDef.get("thanksTo").getAsString());
									});
									return;
								}

								// Download missing assets
								LauncherUtils.log("Downloading client assets...", true);
								LauncherUtils.getGameDescriptor().downloadAssets(lastArchive.source,
										clientsNeedingUpdates.toArray(t -> new String[t]),
										assetsNeedingDownloads.values().toArray(t -> new AssetInformation[t]),
										assets.values().toArray(t -> new AssetInformation[t]),
										assetsList.values().toArray(t -> new AssetInformation[t]),
										AssetManager.getActiveArchive(), lastArchive.archiveDef, archiveDescriptor,
										assetHashes);
							}

							// Download completed
							LauncherUtils.log("Preparing to export archive...", true);
							if (selected == 0) {
								// SGA file

								// Create output
								LauncherUtils.log("Preparing output file...");
								FileOutputStream fO = new FileOutputStream(outputFile);
								ZipOutputStream zO = new ZipOutputStream(fO);
								try {
									// Calculate progress max
									// Things to do:
									// - Archive information file
									// - Add clients
									// - Add assets
									// - Add archive descriptor
									int max = 1;
									Map<String, AssetInformation> deduped = new LinkedHashMap<String, AssetInformation>();
									for (AssetInformation asset : assets.values()) {
										if (!deduped.containsKey(asset.assetHash))
											deduped.put(asset.assetHash, asset);
									}
									int descriptorIndexSize = indexDir(new File("assets/descriptor"));
									max += versions.size() * 100;
									max += deduped.size();
									max += descriptorIndexSize;
									LauncherUtils.resetProgressBar();
									LauncherUtils.setProgress(0, max);
									LauncherUtils.showProgressPanel();

									// Determine platform
									String plat = null;
									if (System.getProperty("os.name").toLowerCase().contains("win")
											&& !System.getProperty("os.name").toLowerCase().contains("darwin")) { // Windows
										plat = "windows";
									} else if (System.getProperty("os.name").toLowerCase().contains("darwin")
											|| System.getProperty("os.name").toLowerCase().contains("mac")) { // MacOS
										plat = "macos";
									} else if (System.getProperty("os.name").toLowerCase().contains("linux")) {// Linux
										plat = "linux";
									}

									// Copy clients
									int i = 0;
									int p = 0;
									LauncherUtils.log("Adding clients...");
									LauncherUtils.setStatus(
											"Archiving... Adding " + i + "/" + versions.size() + " clients...");

									// Add client folder
									ZipEntry ent = new ZipEntry("clients/");
									zO.putNextEntry(ent);
									zO.closeEntry();

									// Add clients
									for (String version : versions) {
										LauncherUtils.log("Adding " + version + "...");
										LauncherUtils.setStatus("Archiving... Adding " + (i + 1) + "/" + versions.size()
												+ " clients... [0%]");

										// Add client folder
										ent = new ZipEntry("clients/client-" + version + "/");
										zO.putNextEntry(ent);
										zO.closeEntry();

										// Add clients
										int maxM = max;
										int in = i;
										int progStart = LauncherUtils.getProgress();
										String cHash = archiveDescriptor.get("versionHashes").getAsJsonObject()
												.get(plat).getAsJsonObject().get(version).getAsString();
										LauncherUtils.getGameDescriptor().addCleanClientFilesToArchiveFile(zO, version,
												"clients/client-" + version + "/", outputFile, lastArchive,
												lastArchive.archiveDef, archiveDescriptor, cHash, (val, mx) -> {
													// Calculate
													float step = (100f / (float) mx);
													LauncherUtils.setProgress(progStart + (int) (step * val), maxM);
													LauncherUtils.setStatus(
															"Archiving... Adding " + (in + 1) + "/" + versions.size()
																	+ " clients... [" + (int) (step * val) + "%]");
												});

										// Increase
										i++;
										p = progStart + 100;
										LauncherUtils.setProgress(p, max);
									}

									// Add asset folder
									ent = new ZipEntry("assets/");
									zO.putNextEntry(ent);
									zO.closeEntry();

									// Copy assets
									i = 0;
									LauncherUtils.log("Adding asset files...");
									LauncherUtils.setStatus(
											"Archiving... Copying " + i + "/" + deduped.size() + " assets... [0%]");
									for (AssetInformation asset : deduped.values()) {
										float step = (100f / (float) deduped.size());
										LauncherUtils.log("Adding " + asset.assetPath + "...");
										LauncherUtils.setStatus("Archiving... Added " + (i + 1) + "/" + deduped.size()
												+ " assets... [" + (int) (step * i) + "%]");

										// Copy
										ent = new ZipEntry("assets/" + asset.assetHash + ".sa");
										zO.putNextEntry(ent);
										FileInputStream fIn = new FileInputStream(asset.localAssetFile);
										fIn.transferTo(zO);
										fIn.close();
										zO.closeEntry();

										// Increase
										i++;
										p++;
										LauncherUtils.setProgress(p, max);
									}

									// Add descriptor folder
									ent = new ZipEntry("descriptor/");
									zO.putNextEntry(ent);
									zO.closeEntry();

									// Copy descriptor
									i = 0;
									LauncherUtils.log("Adding descriptor files...");
									LauncherUtils.setStatus("Archiving... Copying " + i + "/" + descriptorIndexSize
											+ " descriptor files...");
									transferDescriptor(new File("assets/descriptor"), zO, "", i, descriptorIndexSize);

									// Create archive information file
									LauncherUtils.log("Creating archive information file...", true);
									JsonObject archiveInfoJson = new JsonObject();
									archiveInfoJson.addProperty("type", lastArchive.descriptorType);

									// Create client information
									JsonObject clientLst = new JsonObject();
									for (String version : versions) {
										clientLst.addProperty(version, "clients/client-" + version);
									}
									archiveInfoJson.add("clients", clientLst);

									// Write
									ent = new ZipEntry("archiveinfo.json");
									zO.putNextEntry(ent);
									zO.write(new GsonBuilder().setPrettyPrinting().create().toJson(archiveInfoJson)
											.getBytes("UTF-8"));
									zO.closeEntry();

									// Hide bars
									LauncherUtils.hideProgressPanel();
									LauncherUtils.resetProgressBar();

									// Done
									LauncherUtils.log("Finished! Archive saved to " + outputFile.getPath() + "!", true);
									JOptionPane.showMessageDialog(VersionManagerWindow.this,
											"Archive created successfully!\n\nArchive has been saved to '"
													+ outputFile.getPath() + "'",
											"Archive created successfully", JOptionPane.INFORMATION_MESSAGE);
								} finally {
									zO.close();
									fO.close();
								}
							} else if (selected == 1) {
								// Server folder

								// Create output
								LauncherUtils.log("Creating output folders...");
								File outputAssets = new File(outputFile, "assets");
								File outputClients = new File(outputFile, "clients");
								File archiveInfo = new File(outputFile, "sentinelarchiveinfo.json");
								outputAssets.mkdirs();
								outputClients.mkdirs();

								// Calculate progress max
								int max = 1 + versions.size() + assets.size();
								LauncherUtils.resetProgressBar();
								LauncherUtils.setProgress(0, max);
								LauncherUtils.showProgressPanel();

								// Determine platform
								String plat = null;
								if (System.getProperty("os.name").toLowerCase().contains("win")
										&& !System.getProperty("os.name").toLowerCase().contains("darwin")) { // Windows
									plat = "windows";
								} else if (System.getProperty("os.name").toLowerCase().contains("darwin")
										|| System.getProperty("os.name").toLowerCase().contains("mac")) { // MacOS
									plat = "macos";
								} else if (System.getProperty("os.name").toLowerCase().contains("linux")) {// Linux
									plat = "linux";
								}

								// Copy clients
								int i = 0;
								int p = 0;
								LauncherUtils.log("Copying clients...");
								LauncherUtils.setStatus(
										"Creating archive... Copying " + i + "/" + versions.size() + " clients...");
								Map<String, String> clientPaths = new LinkedHashMap<String, String>();
								for (String version : versions) {
									LauncherUtils.log("Copying " + version + "...");
									LauncherUtils.setStatus("Creating archive... Copying " + (i + 1) + "/"
											+ versions.size() + " clients...");

									// Add client
									String cHash = archiveDescriptor.get("versionHashes").getAsJsonObject().get(plat)
											.getAsJsonObject().get(version).getAsString();
									File clientOutputFile = LauncherUtils.getGameDescriptor().addClientToArchiveFolder(
											version, outputClients, outputFile, lastArchive, lastArchive.archiveDef,
											archiveDescriptor, cHash);

									// Check if the client file is relative to the archive
									File f = clientOutputFile;
									while (f != null
											&& !f.getAbsolutePath().equalsIgnoreCase(outputFile.getAbsolutePath())) {
										f = f.getParentFile();
									}
									if (f == null)
										throw new IOException(
												"Game descriptor returned a client path that is not relative to the archive: "
														+ clientOutputFile);

									// Get relative path
									String pth = outputFile.toPath().relativize(clientOutputFile.toPath()).toString();
									clientPaths.put(version, pth);

									// Increase
									i++;
									p++;
									LauncherUtils.setProgress(p, max);
								}

								// Copy assets
								i = 0;
								LauncherUtils.log("Copying asset files...");
								LauncherUtils.setStatus(
										"Creating archive... Copying " + i + "/" + assets.size() + " assets...");
								for (AssetInformation asset : assets.values()) {
									LauncherUtils.log("Copying " + asset.assetPath + "...");
									LauncherUtils.setStatus("Creating archive... Copying " + (i + 1) + "/"
											+ assets.size() + " assets...");

									// Copy
									File output = new File(outputAssets, asset.assetPath);
									output.getParentFile().mkdirs();
									Files.copy(asset.localAssetFile.toPath(), output.toPath(),
											StandardCopyOption.REPLACE_EXISTING);

									// Increase
									i++;
									p++;
									LauncherUtils.setProgress(p, max);
								}

								// Show window
								LauncherUtils.log("Waiting for user input...", true);
								ArchiveCreationWindow window = new ArchiveCreationWindow();
								ArchiveCreationWindow.CreationResult res = window.show(VersionManagerWindow.this);
								if (res != null) {
									// Log
									LauncherUtils.log("Creating archive information file...", true);

									// Create url
									String urlBase = res.archiveURL;
									if (!urlBase.endsWith("/"))
										urlBase += "/";

									// Create archive information file
									JsonObject archiveInfoJson = new JsonObject();
									archiveInfoJson.addProperty("archiveName", res.archiveName);
									if (res.creditsField != null)
										archiveInfoJson.addProperty("thanksTo", res.creditsField);
									archiveInfoJson.addProperty("type", lastArchive.descriptorType);
									archiveInfoJson.addProperty("allowFullDownload", res.allowDownload);
									archiveInfoJson.addProperty("allowStreaming", res.allowStreaming);
									archiveInfoJson.addProperty("deprecated", false);
									archiveInfoJson.addProperty("deprecationNotice", "");

									// Create client information
									JsonObject clientLst = new JsonObject();
									for (String version : versions) {
										clientLst.addProperty(version, clientPaths.get(version));
									}
									archiveInfoJson.add("clients", clientLst);

									// Save
									Files.writeString(archiveInfo.toPath(),
											new GsonBuilder().setPrettyPrinting().create().toJson(archiveInfoJson));

									// Hide bars
									LauncherUtils.hideProgressPanel();
									LauncherUtils.resetProgressBar();

									// Done
									LauncherUtils.log("Finished! Archive saved to " + outputFile.getPath() + "!", true);
									JOptionPane.showMessageDialog(VersionManagerWindow.this,
											"Archive created successfully!\n\nArchive has been saved to '"
													+ outputFile.getPath() + "'",
											"Archive created successfully", JOptionPane.INFORMATION_MESSAGE);

									// Show archive snippet
									String id = UUID.randomUUID().toString();
									while (AssetManager.getArchive(id) != null)
										id = UUID.randomUUID().toString();
									JsonObject entry = new JsonObject();
									entry.add(id, archiveInfoJson);
									ArchiveDefSnippetViewer viewer = new ArchiveDefSnippetViewer();
									viewer.show(VersionManagerWindow.this,
											new GsonBuilder().setPrettyPrinting().create().toJson(entry));
								} else {
									// Hide bars
									LauncherUtils.hideProgressPanel();
									LauncherUtils.resetProgressBar();
								}
							}

							// Done
							downloadFinished = true;
							SwingUtilities.invokeLater(() -> {
								// Re-enable
								downloadFinished = true;
								btnOk.setEnabled(true);
								btnCancel.setEnabled(true);
								archiveSelector.setEnabled(true);
								btnAdd.setEnabled(true);
								checkBoxDownload.setVisible(lastArchive.mode == ArchiveMode.REMOTE);
								checkBoxDownload.setEnabled(chBoxWasEnabled);
								btnExport.setEnabled(lastArchive != null && lastArchive.mode == ArchiveMode.REMOTE
										&& lastArchive.supportsDownloads);
								btnDelete.setVisible(lastArchive != null && lastArchive.isUserArchive);
								btnDelete.setEnabled(true);
								btnImport.setEnabled(true);
								qualityLevelBox.setEnabled(true);
								clientListBox.setEnabled(true);
								btnRemove.setEnabled(wasEnabledRemove);
								btnOk.setText("Ok");
								lblThanks.setVisible(lastArchive != null);
								lblThanks.setText("");
								if (lastArchive != null && lastArchive.archiveDef.has("thanksTo"))
									lblThanks.setText("Assets kindly mirrored by "
											+ lastArchive.archiveDef.get("thanksTo").getAsString());
							});
						} catch (Exception e2) {
							// Error
							String stackTrace = "";
							Throwable t = e2;
							while (t != null) {
								for (StackTraceElement ele : t.getStackTrace())
									stackTrace += "\n     At: " + ele;
								t = t.getCause();
								if (t != null)
									stackTrace += "\nCaused by: " + t;
							}
							JOptionPane.showMessageDialog(VersionManagerWindow.this,
									"An error occurred!\n\nError details: " + e2 + stackTrace, "Export error",
									JOptionPane.ERROR_MESSAGE);
							downloadFinished = true;

							// Re-enable
							SwingUtilities.invokeLater(() -> {
								btnOk.setEnabled(true);
								btnCancel.setEnabled(true);
								archiveSelector.setEnabled(true);
								btnAdd.setEnabled(true);
								checkBoxDownload.setVisible(lastArchive.mode == ArchiveMode.REMOTE);
								checkBoxDownload.setEnabled(chBoxWasEnabled);
								btnDelete.setVisible(lastArchive != null && lastArchive.isUserArchive);
								btnExport.setEnabled(lastArchive != null && lastArchive.mode == ArchiveMode.REMOTE
										&& lastArchive.supportsDownloads);
								btnDelete.setVisible(lastArchive != null && lastArchive.isUserArchive);
								btnDelete.setEnabled(true);
								btnImport.setEnabled(true);
								qualityLevelBox.setEnabled(true);
								clientListBox.setEnabled(true);
								btnRemove.setEnabled(wasEnabledRemove);
								btnOk.setText("Ok");
								lblThanks.setVisible(lastArchive != null);
								lblThanks.setText("");
								if (lastArchive != null && lastArchive.archiveDef.has("thanksTo"))
									lblThanks.setText("Assets kindly mirrored by "
											+ lastArchive.archiveDef.get("thanksTo").getAsString());
							});
						}
					});
					th.setDaemon(true);
					th.start();
				}
			}
		});
		btnImport.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// Import

				// Show selection
				int selected = JOptionPane.showOptionDialog(VersionManagerWindow.this,
						"Welcome to the archive import tool!\nHere you can add local and unofficial archives to Sentinel.\n\nPlease select a operation...\n ",
						"Archive Exporter", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null,
						new Object[] { "Import SGA file", "Add fan-run remote server", "Cancel" }, "Cancel");

				// Check cancel
				if (selected == 2 || selected == -1)
					return;

				// Disable
				boolean wasEnabledRemove = btnRemove.isEnabled();
				btnOk.setEnabled(false);
				qualityLevelBox.setEnabled(false);
				btnCancel.setEnabled(false);
				archiveSelector.setEnabled(false);
				btnAdd.setEnabled(false);
				boolean chBoxWasEnabled = checkBoxDownload.isEnabled();
				checkBoxDownload.setEnabled(false);
				clientListBox.setEnabled(false);
				btnExport.setEnabled(false);
				btnImport.setEnabled(false);
				btnRemove.setEnabled(false);
				btnDelete.setEnabled(false);

				Thread th = new Thread(() -> {
					try {
						// Set text
						btnOk.setText("Busy...");

						// Handle option
						SwingUtilities.invokeAndWait(() -> {
							lblThanks.setText("Preparing to import...");
							lblThanks.setVisible(true);
						});
						LauncherUtils.setStatus("Preparing to import...");
						LauncherUtils.log("Determining mode...");
						if (selected == 0) {
							// SGA file
							LauncherUtils.log("SGA mode, requesting user file selection...");
							SwingUtilities.invokeAndWait(() -> {
								lblThanks.setText("Awaiting user selection...");
								lblThanks.setVisible(true);
							});
							LauncherUtils.log("Awaiting user selection...");
							JFileChooser f = new JFileChooser();
							f.setDialogTitle("Choose SGA file to load...");
							FileFilter filter = new FileFilter() {

								@Override
								public boolean accept(File f) {
									return f.isDirectory() || f.getName().endsWith(".sga");
								}

								@Override
								public String getDescription() {
									return "Sentinel game asset archive files (*.sga)";
								}

							};
							f.addChoosableFileFilter(filter);
							f.setFileFilter(filter);
							f.showOpenDialog(VersionManagerWindow.this);
							if (f.getSelectedFile() == null) {
								// Re-enable
								SwingUtilities.invokeLater(() -> {
									btnOk.setEnabled(true);
									btnCancel.setEnabled(true);
									archiveSelector.setEnabled(true);
									btnAdd.setEnabled(true);
									checkBoxDownload.setEnabled(chBoxWasEnabled);
									btnDelete.setVisible(lastArchive != null && lastArchive.isUserArchive);
									btnExport.setEnabled(lastArchive != null && lastArchive.mode == ArchiveMode.REMOTE
											&& lastArchive.supportsDownloads);
									btnDelete.setVisible(lastArchive != null && lastArchive.isUserArchive);
									btnDelete.setEnabled(true);
									btnImport.setEnabled(true);
									qualityLevelBox.setEnabled(true);
									clientListBox.setEnabled(true);
									btnRemove.setEnabled(wasEnabledRemove);
									btnOk.setText("Ok");
									lblThanks.setVisible(lastArchive != null);
									lblThanks.setText("");
									if (lastArchive != null && lastArchive.archiveDef.has("thanksTo"))
										lblThanks.setText("Assets kindly mirrored by "
												+ lastArchive.archiveDef.get("thanksTo").getAsString());
								});
							}

							// Read file
							LauncherUtils.log("Reading SGA file...");
							SwingUtilities.invokeAndWait(() -> {
								lblThanks.setText("Reading SGA file...");
								lblThanks.setVisible(true);
							});
							ZipFile zip = new ZipFile(f.getSelectedFile());
							ZipEntry entry = zip.getEntry("archiveinfo.json");
							if (entry == null) {
								// Invalid
								zip.close();

								// Show error
								JOptionPane.showMessageDialog(VersionManagerWindow.this,
										"Invalid SGA file was selected.\n\nPlease select a valid Sentinel Archive File.",
										"Import error", JOptionPane.ERROR_MESSAGE);

								// Re-enable
								SwingUtilities.invokeLater(() -> {
									btnOk.setEnabled(true);
									btnCancel.setEnabled(true);
									archiveSelector.setEnabled(true);
									btnAdd.setEnabled(true);
									checkBoxDownload.setEnabled(chBoxWasEnabled);
									btnDelete.setVisible(lastArchive != null && lastArchive.isUserArchive);
									btnExport.setEnabled(lastArchive != null && lastArchive.mode == ArchiveMode.REMOTE
											&& lastArchive.supportsDownloads);
									btnDelete.setVisible(lastArchive != null && lastArchive.isUserArchive);
									btnDelete.setEnabled(true);
									btnImport.setEnabled(true);
									qualityLevelBox.setEnabled(true);
									clientListBox.setEnabled(true);
									btnRemove.setEnabled(wasEnabledRemove);
									btnOk.setText("Ok");
									lblThanks.setVisible(lastArchive != null);
									lblThanks.setText("");
									if (lastArchive != null && lastArchive.archiveDef.has("thanksTo"))
										lblThanks.setText("Assets kindly mirrored by "
												+ lastArchive.archiveDef.get("thanksTo").getAsString());
								});
							}

							// Read data
							InputStream strm = zip.getInputStream(entry);
							JsonObject archiveDetails = JsonParser.parseString(new String(strm.readAllBytes(), "UTF-8"))
									.getAsJsonObject();
							strm.close();

							// add details
							LauncherUtils.log("Adding archive...");
							SwingUtilities.invokeAndWait(() -> {
								lblThanks.setText("Adding archive...");
								lblThanks.setVisible(true);
							});
							archiveDetails.addProperty("isSgaFile", true);
							archiveDetails.addProperty("allowFullDownload", true);
							archiveDetails.addProperty("allowStreaming", false);
							archiveDetails.addProperty("archiveName", f.getSelectedFile().getName());
							archiveDetails.addProperty("filePath", f.getSelectedFile().getCanonicalPath());

							// Add archive
							ArchiveInformation archive = AssetManager.addUserArchive(archiveDetails);

							// Select
							lastArchive = archive;
							saveSettings();
							if (new File("assets/descriptor.hash").exists())
								new File("assets/descriptor.hash").delete();
							if (new File("assets/descriptor-local.version").exists())
								new File("assets/descriptor-local.version").delete();
							SwingUtilities.invokeAndWait(() -> {
								// Re-enable
								btnOk.setEnabled(true);
								btnCancel.setEnabled(true);
								archiveSelector.setEnabled(true);
								btnAdd.setEnabled(true);
								checkBoxDownload.setEnabled(chBoxWasEnabled);
								btnDelete.setVisible(lastArchive != null && lastArchive.isUserArchive);
								btnExport.setEnabled(lastArchive != null && lastArchive.mode == ArchiveMode.REMOTE
										&& lastArchive.supportsDownloads);
								btnDelete.setVisible(lastArchive != null && lastArchive.isUserArchive);
								btnDelete.setEnabled(true);
								btnImport.setEnabled(true);
								qualityLevelBox.setEnabled(true);
								clientListBox.setEnabled(true);
								btnRemove.setEnabled(wasEnabledRemove);
								btnOk.setText("Ok");
								lblThanks.setVisible(lastArchive != null);
								lblThanks.setText("");
								if (lastArchive != null && lastArchive.archiveDef.has("thanksTo"))
									lblThanks.setText("Assets kindly mirrored by "
											+ lastArchive.archiveDef.get("thanksTo").getAsString());

								// Set buttons and boxes
								checkBoxDownload.setEnabled(!lastArchive.isDeprecated && lastArchive.supportsDownloads
										&& lastArchive.supportsStreaming);
								checkBoxDownload.setSelected(!lastArchive.supportsStreaming);
								lblQualityLevels.setVisible(checkBoxDownload.isSelected());
								qualityLevelBox.setVisible(checkBoxDownload.isSelected());
								btnExport.setEnabled(lastArchive != null && lastArchive.mode == ArchiveMode.REMOTE
										&& lastArchive.supportsDownloads);
								btnDelete.setVisible(lastArchive != null && lastArchive.isUserArchive);
								btnDelete.setEnabled(true);
								archiveSelector.setSelectedItem(lastArchive);

								// Load client list
								refreshClientList();
							});
						} else if (selected == 1) {
							// Remote server

							// Show input
							LauncherUtils.log("Remote server mode, requesting URL...");
							SwingUtilities.invokeAndWait(() -> {
								lblThanks.setText("Awaiting URL input...");
								lblThanks.setVisible(true);
							});
							LauncherUtils.log("Awaiting URL input...");
							String url = JOptionPane.showInputDialog(VersionManagerWindow.this, "Server URL",
									"Add server", JOptionPane.QUESTION_MESSAGE);
							if (url == null) {
								// Re-enable
								SwingUtilities.invokeLater(() -> {
									btnOk.setEnabled(true);
									btnCancel.setEnabled(true);
									archiveSelector.setEnabled(true);
									btnAdd.setEnabled(true);
									checkBoxDownload.setEnabled(chBoxWasEnabled);
									btnDelete.setVisible(lastArchive != null && lastArchive.isUserArchive);
									btnExport.setEnabled(lastArchive != null && lastArchive.mode == ArchiveMode.REMOTE
											&& lastArchive.supportsDownloads);
									btnDelete.setVisible(lastArchive != null && lastArchive.isUserArchive);
									btnDelete.setEnabled(true);
									btnImport.setEnabled(true);
									qualityLevelBox.setEnabled(true);
									clientListBox.setEnabled(true);
									btnRemove.setEnabled(wasEnabledRemove);
									btnOk.setText("Ok");
									lblThanks.setVisible(lastArchive != null);
									lblThanks.setText("");
									if (lastArchive != null && lastArchive.archiveDef.has("thanksTo"))
										lblThanks.setText("Assets kindly mirrored by "
												+ lastArchive.archiveDef.get("thanksTo").getAsString());
								});
								return;
							}

							// Contact server
							LauncherUtils.log("Contacting server...");
							SwingUtilities.invokeAndWait(() -> {
								lblThanks.setText("Contacting server...");
								lblThanks.setVisible(true);
							});
							try {
								// Download information
								String archiveURL = url;
								if (!archiveURL.endsWith("/"))
									archiveURL += "/";
								JsonObject archiveDetails = JsonParser
										.parseString(
												LauncherUtils.downloadString(archiveURL + "sentinelarchiveinfo.json"))
										.getAsJsonObject();
								if (!archiveDetails.has("archiveName") || !archiveDetails.has("type")
										|| !archiveDetails.has("allowFullDownload")
										|| !archiveDetails.has("allowStreaming") || !archiveDetails.has("deprecated")
										|| !archiveDetails.has("clients")
										|| !archiveDetails.get("clients").isJsonObject())
									throw new IOException();

								// Update URLs
								LauncherUtils.log("Adding archive...");
								SwingUtilities.invokeAndWait(() -> {
									lblThanks.setText("Adding archive...");
									lblThanks.setVisible(true);
								});
								archiveDetails.addProperty("url", archiveURL + "assets");
								JsonObject clients = archiveDetails.get("clients").getAsJsonObject();
								for (String version : clients.keySet()) {
									clients.addProperty(version, archiveURL + clients.get(version).getAsString());
								}

								// Add archive
								ArchiveInformation archive = AssetManager.addUserArchive(archiveDetails);

								// Select
								lastArchive = archive;
								saveSettings();
								if (new File("assets/descriptor.hash").exists())
									new File("assets/descriptor.hash").delete();
								if (new File("assets/descriptor-local.version").exists())
									new File("assets/descriptor-local.version").delete();
								SwingUtilities.invokeAndWait(() -> {
									// Re-enable
									btnOk.setEnabled(true);
									btnCancel.setEnabled(true);
									archiveSelector.setEnabled(true);
									btnAdd.setEnabled(true);
									checkBoxDownload.setEnabled(chBoxWasEnabled);
									btnDelete.setVisible(lastArchive != null && lastArchive.isUserArchive);
									btnExport.setEnabled(lastArchive != null && lastArchive.mode == ArchiveMode.REMOTE
											&& lastArchive.supportsDownloads);
									btnDelete.setVisible(lastArchive != null && lastArchive.isUserArchive);
									btnDelete.setEnabled(true);
									btnImport.setEnabled(true);
									qualityLevelBox.setEnabled(true);
									clientListBox.setEnabled(true);
									btnRemove.setEnabled(wasEnabledRemove);
									btnOk.setText("Ok");
									lblThanks.setVisible(lastArchive != null);
									lblThanks.setText("");
									if (lastArchive != null && lastArchive.archiveDef.has("thanksTo"))
										lblThanks.setText("Assets kindly mirrored by "
												+ lastArchive.archiveDef.get("thanksTo").getAsString());

									// Set buttons and boxes
									checkBoxDownload.setEnabled(!lastArchive.isDeprecated
											&& lastArchive.supportsDownloads && lastArchive.supportsStreaming);
									checkBoxDownload.setSelected(!lastArchive.supportsStreaming);
									lblQualityLevels.setVisible(checkBoxDownload.isSelected());
									qualityLevelBox.setVisible(checkBoxDownload.isSelected());
									btnExport.setEnabled(lastArchive != null && lastArchive.mode == ArchiveMode.REMOTE
											&& lastArchive.supportsDownloads);
									btnDelete.setVisible(lastArchive != null && lastArchive.isUserArchive);
									btnDelete.setEnabled(true);
									archiveSelector.setSelectedItem(lastArchive);

									// Load client list
									refreshClientList();
								});
							} catch (Exception e2) {
								// Fail

								// Show error
								JOptionPane.showMessageDialog(VersionManagerWindow.this, "Could not contact " + url
										+ "\n\nPlease make sure this is a sentinel-exported archive source and that the host is online.",
										"Import error", JOptionPane.ERROR_MESSAGE);

								// Re-enable
								SwingUtilities.invokeLater(() -> {
									btnOk.setEnabled(true);
									btnCancel.setEnabled(true);
									archiveSelector.setEnabled(true);
									btnAdd.setEnabled(true);
									checkBoxDownload.setVisible(lastArchive.mode == ArchiveMode.REMOTE);
									checkBoxDownload.setEnabled(chBoxWasEnabled);
									btnDelete.setVisible(lastArchive != null && lastArchive.isUserArchive);
									btnExport.setEnabled(lastArchive != null && lastArchive.mode == ArchiveMode.REMOTE
											&& lastArchive.supportsDownloads);
									btnDelete.setVisible(lastArchive != null && lastArchive.isUserArchive);
									btnDelete.setEnabled(true);
									btnImport.setEnabled(true);
									qualityLevelBox.setEnabled(true);
									clientListBox.setEnabled(true);
									btnRemove.setEnabled(wasEnabledRemove);
									btnOk.setText("Ok");
									lblThanks.setVisible(lastArchive != null);
									lblThanks.setText("");
									if (lastArchive != null && lastArchive.archiveDef.has("thanksTo"))
										lblThanks.setText("Assets kindly mirrored by "
												+ lastArchive.archiveDef.get("thanksTo").getAsString());
								});
								return;
							}
						}
					} catch (Exception e2) {
						// Error
						String stackTrace = "";
						Throwable t = e2;
						while (t != null) {
							for (StackTraceElement ele : t.getStackTrace())
								stackTrace += "\n     At: " + ele;
							t = t.getCause();
							if (t != null)
								stackTrace += "\nCaused by: " + t;
						}
						JOptionPane.showMessageDialog(VersionManagerWindow.this,
								"An error occurred!\n\nError details: " + e2 + stackTrace, "Import error",
								JOptionPane.ERROR_MESSAGE);
						downloadFinished = true;

						// Re-enable
						SwingUtilities.invokeLater(() -> {
							btnOk.setEnabled(true);
							btnCancel.setEnabled(true);
							archiveSelector.setEnabled(true);
							btnAdd.setEnabled(true);
							checkBoxDownload.setVisible(lastArchive.mode == ArchiveMode.REMOTE);
							checkBoxDownload.setEnabled(chBoxWasEnabled);
							btnDelete.setVisible(lastArchive != null && lastArchive.isUserArchive);
							btnExport.setEnabled(lastArchive != null && lastArchive.mode == ArchiveMode.REMOTE
									&& lastArchive.supportsDownloads);
							btnDelete.setVisible(lastArchive != null && lastArchive.isUserArchive);
							btnDelete.setEnabled(true);
							btnImport.setEnabled(true);
							qualityLevelBox.setEnabled(true);
							clientListBox.setEnabled(true);
							btnRemove.setEnabled(wasEnabledRemove);
							btnOk.setText("Ok");
							lblThanks.setVisible(lastArchive != null);
							lblThanks.setText("");
							if (lastArchive != null && lastArchive.archiveDef.has("thanksTo"))
								lblThanks.setText("Assets kindly mirrored by "
										+ lastArchive.archiveDef.get("thanksTo").getAsString());
						});
					}
				}, "Importer thread");
				th.setDaemon(true);
				th.start();
			}
		});
		btnDelete.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// Check
				if (lastArchive != null && lastArchive.isUserArchive) {
					// Confirm
					if (JOptionPane.showConfirmDialog(VersionManagerWindow.this,
							"Are you sure you wish to remove this archive?", "Archive removal",
							JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
						return;
					}

					// Remove from asset manager
					try {
						AssetManager.removeUserArchive(lastArchive.archiveID);
					} catch (IOException e1) {
						throw new RuntimeException(e1);
					}

					// Unassign
					lastArchive = null;
					warnedArchiveChange = true;
					archiveSelector.setSelectedItem(null);
					checkBoxDownload.setEnabled(false);
					lblQualityLevels.setVisible(false);
					qualityLevelBox.setVisible(false);
					btnExport.setEnabled(false);
					btnDelete.setVisible(false);
					archiveSelector.updateUI();

					if (AssetManager.getArchives().length > 0) {
						// Select first
						lastArchive = AssetManager.getArchives()[0];
						checkBoxDownload.setVisible(lastArchive.mode == ArchiveMode.REMOTE);
						checkBoxDownload.setEnabled(!lastArchive.isDeprecated && lastArchive.supportsDownloads
								&& lastArchive.supportsStreaming);
						checkBoxDownload.setSelected(!lastArchive.supportsStreaming);
						lblQualityLevels.setVisible(checkBoxDownload.isSelected());
						qualityLevelBox.setVisible(checkBoxDownload.isSelected());
						btnExport.setEnabled(lastArchive != null && lastArchive.mode == ArchiveMode.REMOTE
								&& lastArchive.supportsDownloads);
						btnDelete.setVisible(lastArchive != null && lastArchive.isUserArchive);
						btnDelete.setEnabled(true);
						archiveSelector.setSelectedItem(lastArchive);

						// Save
						try {
							saveSettings();
						} catch (IOException e1) {
							throw new RuntimeException(e1);
						}
					}
				}
			}
		});

		// Set
		btnOk.setEnabled(lastArchive != null && filteredClientEntryData().length != 0);
	}

	private void loadQualityLevelBox(JComboBox<QualityLevelEntry> qualityLevelBox, String[] levels) {
		// Create quality level list
		ArrayList<QualityLevelEntry> entries = new ArrayList<QualityLevelEntry>();

		// Load quality levels
		ArrayList<String> qualityLevels = new ArrayList<String>();
		File enabledQualityLevelListFile = new File("assets/qualitylevels.json");
		if (enabledQualityLevelListFile.exists()) {
			// Read quality levels
			try {
				JsonArray qualityLevelsArr = JsonParser
						.parseString(Files.readString(enabledQualityLevelListFile.toPath())).getAsJsonArray();
				for (JsonElement ele : qualityLevelsArr) {
					// Check validity
					String lvl = ele.getAsString();
					if (!qualityLevels.contains(lvl) && Stream.of(levels).anyMatch(t -> t.equalsIgnoreCase(lvl))) {
						// Add
						qualityLevels.add(lvl);
					}
				}
			} catch (IOException e) {
			}
		}

		// Verify
		if (qualityLevels.size() == 0) {
			// Add all
			for (String lvl : levels)
				qualityLevels.add(lvl);
		}

		// Populate list
		QualityLevelEntry mask = new QualityLevelEntry();
		for (String level : levels) {
			QualityLevelEntry lv = new QualityLevelEntry();
			lv.level = level;
			lv.enabled = qualityLevels.contains(level);
			lv.checkBox = new JCheckBox();
			lv.checkBox.setSelected(lv.enabled);
			lv.checkBox.setText(lv.toString());
			entries.add(lv);
		}
		qualityLevelElements = entries;

		// Add renderer
		qualityLevelBox.setRenderer(new DefaultListCellRenderer() {

			private static final long serialVersionUID = 1L;

			@Override
			public Component getListCellRendererComponent(JList<? extends Object> list, Object lvE, int index,
					boolean isSelected, boolean cellHasFocus) {
				JLabel comp = (JLabel) super.getListCellRendererComponent(list, lvE, index, isSelected, cellHasFocus);

				QualityLevelEntry lv = (QualityLevelEntry) lvE;
				if (lv == null || lv.level == null) {
					String str = "";
					for (QualityLevelEntry lvl : qualityLevelElements) {
						if (lvl.enabled) {
							if (!str.isEmpty())
								str += ", ";
							str += lvl.level;
						}
					}
					comp.setText("Quality levels: " + str);
					return comp;
				}

				// Set checkbox
				lv.checkBox.setComponentOrientation(list.getComponentOrientation());
				lv.checkBox.setSelected(lv.enabled);
				lv.checkBox.setText(lv.toString());
				lv.checkBox.setForeground(comp.getForeground());
				lv.checkBox.setBackground(comp.getBackground());
				lv.checkBox.setEnabled(list.isEnabled());
				lv.checkBox.setFont(list.getFont());
				lv.checkBox.setBorder(comp.getBorder());
				return lv.checkBox;
			}

		});
		qualityLevelBox.addActionListener(e -> {
			QualityLevelEntry ent = (QualityLevelEntry) qualityLevelBox.getSelectedItem();
			if (ent != null) {
				ent.enabled = !ent.enabled;
				qualityLevelBox.setPopupVisible(true);
				qualityLevelBox.setSelectedItem(null);
				qualityLevelBox.repaint();
			}
		});

		// Set model
		qualityLevelBox.setModel(new ComboBoxModel<QualityLevelEntry>() {
			private QualityLevelEntry selected;

			@Override
			public int getSize() {
				return entries.size() + 1;
			}

			@Override
			public QualityLevelEntry getElementAt(int index) {
				if (index == 0)
					return mask;
				return entries.get(index - 1);
			}

			@Override
			public void setSelectedItem(Object anItem) {
				selected = (QualityLevelEntry) anItem;
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
	}

	private ClientEntry[] filteredClientEntryData() {
		ArrayList<ClientEntry> filteredData = new ArrayList<ClientEntry>();
		if (lastArchive == null)
			return new ClientEntry[0];
		JsonObject clientLst = lastArchive.archiveClientLst;
		for (ClientEntry entry : clients) {
			// Check
			if (lastArchive != null) {
				if (clientLst.has(entry.version))
					filteredData.add(entry);
			}
		}
		return filteredData.toArray(t -> new ClientEntry[t]);
	}

	private static String[] appendToStringArray(String[] source, String ele) {
		String[] res = new String[source.length + 1];
		for (int i = 0; i < source.length; i++)
			res[i] = source[i];
		res[res.length - 1] = ele;
		return res;
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
		lblThanks.setVisible(lastArchive != null && lastArchive.archiveDef.has("thanksTo"));
		if (lastArchive != null && lastArchive.archiveDef.has("thanksTo"))
			lblThanks.setText("Assets kindly mirrored by " + lastArchive.archiveDef.get("thanksTo").getAsString());
	}

	private String parseURL(String url, String urlBaseDescriptorFileF, String urlBaseSoftwareFileF,
			String sentinelAssetRoot) {
		if (url.startsWith("sgd:")) {
			String source = url.substring(4);
			while (source.startsWith("/"))
				source = source.substring(1);
			url = urlBaseDescriptorFileF + source;
		} else if (url.startsWith("svp:")) {
			String source = url.substring(4);
			while (source.startsWith("/"))
				source = source.substring(1);
			url = urlBaseSoftwareFileF + source;
		} else if (sentinelAssetRoot != null && url.startsWith("sac:")) {
			String source = url.substring(4);
			while (source.startsWith("/"))
				source = source.substring(1);
			url = sentinelAssetRoot + source;
		}
		return url;
	}

	private void indexAssetHashes(HashMap<String, String> assetHashes, File hashFile)
			throws JsonSyntaxException, IOException {
		// Load hashes
		String[] lines = Files.readString(hashFile.toPath()).split("\n");
		for (String line : lines) {
			if (line.isEmpty())
				continue;
			// Parse
			String name = line.substring(0, line.indexOf(": ")).replace(";sp;", " ").replace(";cl;", ":")
					.replace(";sl;", ";");
			String hash = line.substring(line.indexOf(": ") + 2);
			assetHashes.put(name, hash);
		}
	}

	private void indexAssetSizes(HashMap<String, Long> assetSizes, File sizeFile)
			throws JsonSyntaxException, IOException {
		// Load hashes
		String[] lines = Files.readString(sizeFile.toPath()).split("\n");
		for (String line : lines) {
			if (line.isEmpty())
				continue;
			// Parse
			String name = line.substring(0, line.indexOf(": ")).replace(";sp;", " ").replace(";cl;", ":")
					.replace(";sl;", ";");
			String len = line.substring(line.indexOf(": ") + 2);
			assetSizes.put(name, Long.parseLong(len));
		}
	}

	private int transferDescriptor(File dir, ZipOutputStream zO, String prefix, int index, int max) throws IOException {
		for (File subDir : dir.listFiles(t -> t.isDirectory())) {
			ZipEntry ent = new ZipEntry("descriptor/" + prefix + subDir.getName() + "/");
			zO.putNextEntry(ent);
			zO.closeEntry();
			index += transferDescriptor(subDir, zO, prefix + subDir.getName() + "/", index, max);
			index++;
			LauncherUtils.setStatus("Archiving... Copying " + index + "/" + max + " descriptor files...");
			LauncherUtils.increaseProgress();
		}
		for (File f : dir.listFiles(t -> t.isFile())) {
			ZipEntry ent = new ZipEntry("descriptor/" + prefix + f.getName());
			zO.putNextEntry(ent);
			FileInputStream fIn = new FileInputStream(f);
			fIn.transferTo(zO);
			fIn.close();
			zO.closeEntry();
			index++;
			LauncherUtils.setStatus("Archiving... Copying " + index + "/" + max + " descriptor files...");
			LauncherUtils.increaseProgress();
		}
		return index;
	}

	private static int indexDir(File dir) {
		int i = 0;
		for (File subDir : dir.listFiles(t -> t.isDirectory())) {
			i += indexDir(subDir) + 1;
		}
		File[] listFiles = dir.listFiles(t -> !t.isDirectory());
		for (int j = 0; j < listFiles.length; j++) {
			i++;
		}
		return i;
	}

	private boolean updateDescriptorIfNeeded(boolean chBoxWasEnabled, boolean wasEnabledRemove) throws Exception {
		// Update descriptor
		if (updateDescriptor) {
			if (lastArchive.mode == ArchiveMode.REMOTE) {
				// Test connection
				if (!AssetManager.testArchiveConnection(lastArchive)) {
					// Error
					LauncherUtils.log("Archive connection could not be made.");
					JOptionPane.showMessageDialog(VersionManagerWindow.this,
							"Server connection could not be established with the archive server!\n\nCannot continue as the archive descriptor needs to be updated!",
							"No server connection", JOptionPane.ERROR_MESSAGE);

					// Re-enable
					SwingUtilities.invokeLater(() -> {
						btnOk.setEnabled(true);
						btnCancel.setEnabled(true);
						archiveSelector.setEnabled(true);
						btnAdd.setEnabled(true);
						checkBoxDownload.setVisible(lastArchive.mode == ArchiveMode.REMOTE);
						checkBoxDownload.setEnabled(chBoxWasEnabled);
						qualityLevelBox.setEnabled(true);
						clientListBox.setEnabled(true);
						btnExport.setEnabled(lastArchive != null && lastArchive.mode == ArchiveMode.REMOTE
								&& lastArchive.supportsDownloads);
						btnDelete.setVisible(lastArchive != null && lastArchive.isUserArchive);
						btnDelete.setEnabled(true);
						btnImport.setEnabled(true);
						btnRemove.setEnabled(wasEnabledRemove);
						btnOk.setText("Ok");
						lblThanks.setVisible(lastArchive != null && lastArchive.archiveDef.has("thanksTo"));
						if (lastArchive != null && lastArchive.archiveDef.has("thanksTo"))
							lblThanks.setText("Assets kindly mirrored by "
									+ lastArchive.archiveDef.get("thanksTo").getAsString());
					});
					return false;
				}

				// Download descriptor
				LauncherUtils.log("Downloading archive descriptor...", true);
				SwingUtilities.invokeAndWait(() -> {
					if (!downloadFinished) {
						lblThanks.setText("Downloading archive descriptor... Please wait... [0%]");
						lblThanks.setVisible(true);
					}
				});
				downloadFinished = false;
				Thread th2 = new Thread(() -> {
					String lastMsg = "";
					while (!downloadFinished) {
						try {
							int percentage = (int) ((100f / (float) (LauncherUtils.getProgressMax()))
									* (float) LauncherUtils.getProgress());
							if (!lastMsg
									.equals("Downloading archive descriptor... Please wait... [" + percentage + "%]")) {
								lastMsg = "Downloading archive descriptor... Please wait... [" + percentage + "%]";
								SwingUtilities.invokeAndWait(() -> {
									if (!downloadFinished) {
										lblThanks.setText("Downloading archive descriptor... Please wait... ["
												+ percentage + "%]");
									}
								});
							}
							Thread.sleep(100);
						} catch (InvocationTargetException | InterruptedException e1) {
							break;
						}
					}
				});
				th2.setDaemon(true);
				th2.start();
				LauncherUtils.resetProgressBar();
				LauncherUtils.showProgressPanel();
				String dir = parseURL(
						AssetManager.getSentinelAssetControllerConfig().get("descriptorRoot").getAsString(),
						LauncherUtils.urlBaseDescriptorFile, LauncherUtils.urlBaseSoftwareFile,
						AssetManager.getAssetInformationRootURL());
				String rHashDescriptor = LauncherUtils.downloadString(dir + lastArchive.descriptorType + ".hash")
						.replace("\r", "").replace("\n", "");
				LauncherUtils.downloadFile(dir + lastArchive.descriptorType + ".zip",
						new File("assets/descriptor.zip"));
				downloadFinished = true;

				// Hide bars
				LauncherUtils.hideProgressPanel();
				LauncherUtils.resetProgressBar();

				// Verify signature
				LauncherUtils.log("Verifying signature... Please wait...", true);
				SwingUtilities.invokeAndWait(() -> {
					lblThanks.setText("Verifying signature... Please wait...");
					lblThanks.setVisible(true);
				});
				if (!LauncherUtils.verifyPackageSignature(new File("assets/descriptor.zip"),
						new File("assets/sac-publickey.pem"))) {
					// Check if signed
					if (!LauncherUtils.isPackageSigned(new File("assets/descriptor.zip"))) {
						// Hide bars
						LauncherUtils.hideProgressPanel();
						LauncherUtils.resetProgressBar();

						// Unsigned
						// Check support
						LauncherUtils.log("Package is unsigned.");
						if (!AssetManager.getSentinelAssetControllerConfig().get("allowUnsignedArchiveDescriptors")
								.getAsBoolean()) {
							LauncherUtils.log("Package is unsigned.");
							JOptionPane.showMessageDialog(VersionManagerWindow.this,
									"The archive descriptor is unsigned and this game descriptor does not support unsigned archive descriptors.\n\nPlease report this error to the project's archival team.",
									"Update error", JOptionPane.ERROR_MESSAGE);

							// Re-enable
							SwingUtilities.invokeLater(() -> {
								btnOk.setEnabled(true);
								btnCancel.setEnabled(true);
								archiveSelector.setEnabled(true);
								btnAdd.setEnabled(true);
								checkBoxDownload.setVisible(lastArchive.mode == ArchiveMode.REMOTE);
								checkBoxDownload.setEnabled(chBoxWasEnabled);
								qualityLevelBox.setEnabled(true);
								clientListBox.setEnabled(true);
								btnExport.setEnabled(lastArchive != null && lastArchive.mode == ArchiveMode.REMOTE
										&& lastArchive.supportsDownloads);
								btnDelete.setVisible(lastArchive != null && lastArchive.isUserArchive);
								btnDelete.setEnabled(true);
								btnImport.setEnabled(true);
								btnRemove.setEnabled(wasEnabledRemove);
								btnOk.setText("Ok");
								lblThanks.setVisible(lastArchive != null && lastArchive.archiveDef.has("thanksTo"));
								if (lastArchive != null && lastArchive.archiveDef.has("thanksTo"))
									lblThanks.setText("Assets kindly mirrored by "
											+ lastArchive.archiveDef.get("thanksTo").getAsString());
							});
							return false;
						}
					} else {
						// Hide bars
						LauncherUtils.hideProgressPanel();
						LauncherUtils.resetProgressBar();

						LauncherUtils.log("Signature verification failure.");
						JOptionPane.showMessageDialog(VersionManagerWindow.this,
								"Failed to verify integrity of archive descriptor file.\n\nPlease report this error to the project's archival team.",
								"Update error", JOptionPane.ERROR_MESSAGE);

						// Re-enable
						SwingUtilities.invokeLater(() -> {
							btnOk.setEnabled(true);
							btnCancel.setEnabled(true);
							archiveSelector.setEnabled(true);
							btnAdd.setEnabled(true);
							checkBoxDownload.setVisible(lastArchive.mode == ArchiveMode.REMOTE);
							checkBoxDownload.setEnabled(chBoxWasEnabled);
							qualityLevelBox.setEnabled(true);
							btnExport.setEnabled(lastArchive != null && lastArchive.mode == ArchiveMode.REMOTE
									&& lastArchive.supportsDownloads);
							btnDelete.setVisible(lastArchive != null && lastArchive.isUserArchive);
							btnDelete.setEnabled(true);
							btnImport.setEnabled(true);
							clientListBox.setEnabled(true);
							btnRemove.setEnabled(wasEnabledRemove);
							btnOk.setText("Ok");
							lblThanks.setVisible(lastArchive != null && lastArchive.archiveDef.has("thanksTo"));
							if (lastArchive != null && lastArchive.archiveDef.has("thanksTo"))
								lblThanks.setText("Assets kindly mirrored by "
										+ lastArchive.archiveDef.get("thanksTo").getAsString());
						});
						return false;
					}
				}

				// Extract
				SwingUtilities.invokeAndWait(() -> {
					if (!downloadFinished) {
						lblThanks.setText("Extracting archive information... Please wait... [0%]");
						lblThanks.setVisible(true);
					}
				});
				downloadFinished = false;
				th2 = new Thread(() -> {
					String lastMsg = "";
					while (!downloadFinished) {
						try {
							int percentage = (int) ((100f / (float) (LauncherUtils.getProgressMax()))
									* (float) LauncherUtils.getProgress());
							if (!lastMsg
									.equals("Extracting archive information... Please wait... [" + percentage + "%]")) {
								lastMsg = "Extracting archive information... Please wait... [" + percentage + "%]";
								SwingUtilities.invokeAndWait(() -> {
									if (!downloadFinished) {
										lblThanks.setText("Extracting archive information... Please wait... ["
												+ percentage + "%]");
									}
								});
							}
							Thread.sleep(100);
						} catch (InvocationTargetException | InterruptedException e1) {
							break;
						}
					}
				});
				th2.setDaemon(true);
				th2.start();
				LauncherUtils.log("Extracting archive information...", true);
				if (new File("assets/descriptor").exists())
					LauncherUtils.deleteDir(new File("assets/descriptor"));
				LauncherUtils.unZip(new File("assets/descriptor.zip"), new File("assets/descriptor"));
				downloadFinished = true;

				// Write hash
				Files.writeString(Path.of("assets/descriptor.hash"), rHashDescriptor);

				// Hide bars
				LauncherUtils.hideProgressPanel();
				LauncherUtils.resetProgressBar();
				updateDescriptor = false;
			} else {
				// Check source
				if (!new File(lastArchive.source).exists()) {
					// Hide bars
					LauncherUtils.hideProgressPanel();
					LauncherUtils.resetProgressBar();
					updateDescriptor = false;
					return true;
				}

				// Re-extract descriptor
				LauncherUtils.log("Re-extracting archive descriptor...", true);
				SwingUtilities.invokeAndWait(() -> {
					if (!downloadFinished) {
						lblThanks.setText("Re-extracting archive descriptor... Please wait... [0%]");
						lblThanks.setVisible(true);
					}
				});

				// Count entries
				ZipFile archive = new ZipFile(lastArchive.source);
				int count = 0;
				Enumeration<? extends ZipEntry> en = archive.entries();
				while (en.hasMoreElements()) {
					ZipEntry ent = en.nextElement();
					if (ent == null)
						break;
					if (!ent.getName().equals("descriptor/") && ent.getName().startsWith("descriptor/"))
						count++;
				}
				archive.close();
				downloadFinished = false;
				Thread th2 = new Thread(() -> {
					String lastMsg = "";
					while (!downloadFinished) {
						try {
							int percentage = (int) ((100f / (float) (LauncherUtils.getProgressMax()))
									* (float) LauncherUtils.getProgress());
							if (!lastMsg.equals(
									"Re-extracting archive descriptor... Please wait... [" + percentage + "%]")) {
								lastMsg = "Re-extracting archive descriptor... Please wait... [" + percentage + "%]";
								SwingUtilities.invokeAndWait(() -> {
									if (!downloadFinished) {
										lblThanks.setText("Re-extracting archive descriptor... Please wait... ["
												+ percentage + "%]");
									}
								});
							}
							Thread.sleep(100);
						} catch (InvocationTargetException | InterruptedException e1) {
							break;
						}
					}
				});
				th2.setDaemon(true);
				th2.start();
				LauncherUtils.resetProgressBar();
				LauncherUtils.showProgressPanel();
				LauncherUtils.setProgressMax(count);

				// Extract
				if (new File("assets/descriptor").exists())
					LauncherUtils.deleteDir(new File("assets/descriptor"));
				new File("assets/descriptor").mkdirs();

				// Prepare and set max
				archive = new ZipFile(lastArchive.source);
				en = archive.entries();
				int max = count;

				// Extract zip
				int i = 0;
				while (en.hasMoreElements()) {
					ZipEntry ent = en.nextElement();
					if (ent == null)
						break;
					if (ent.getName().equals("descriptor/") || !ent.getName().startsWith("descriptor/"))
						continue;

					if (ent.isDirectory()) {
						new File("assets/descriptor", ent.getName().substring("descriptor/".length())).mkdirs();
					} else {
						FileOutputStream output = new FileOutputStream(
								new File("assets/descriptor", ent.getName().substring("descriptor/".length())));
						InputStream is = archive.getInputStream(ent);
						is.transferTo(output);
						is.close();
						output.close();
					}

					LauncherUtils.setProgress(i++, max);
				}
				LauncherUtils.setProgress(max, max);
				downloadFinished = true;
				archive.close();

				// Write fake hash
				Files.writeString(Path.of("assets/descriptor.hash"),
						"local-" + LauncherUtils.sha512Hash((new File(lastArchive.source).lastModified() + "-"
								+ lastArchive.source + "-" + new File(lastArchive.source).length()).getBytes("UTF-8")));
				Files.writeString(Path.of("assets/descriptor-local.version"), "latest");

				// Hide bars
				LauncherUtils.hideProgressPanel();
				LauncherUtils.resetProgressBar();
				updateDescriptor = false;
			}
		}
		return true;
	}

	private void saveSettings() throws IOException {
		// Write clients
		JsonArray clientsArr = new JsonArray();
		for (ClientEntry client : clients)
			clientsArr.add(client.version);
		Files.writeString(Path.of("assets/clients.json"), clientsArr.toString());

		// Write archive
		File localArchiveSettings = new File("assets/localdata.json");
		JsonObject settings = new JsonObject();
		settings.addProperty("id", lastArchive.archiveID);
		settings.addProperty("stream", !checkBoxDownload.isSelected());
		Files.writeString(localArchiveSettings.toPath(), settings.toString());

		// Save quality levels
		File enabledQualityLevelListFile = new File("assets/qualitylevels.json");
		if (checkBoxDownload.isSelected()) {
			// Save
			JsonArray arr = new JsonArray();
			for (QualityLevelEntry lv : qualityLevelElements) {
				if (lv.enabled)
					arr.add(lv.level);
			}
			Files.writeString(enabledQualityLevelListFile.toPath(), arr.toString());
		}
	}

	private void doSave() {
		try {
			saveSettings();
		} catch (IOException e1) {
			throw new RuntimeException(e1);
		}

		// Close
		wasCancelled = false;
		dispose();
	}
}
