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
import com.google.gson.JsonSyntaxException;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;
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
	private JButton btnOk;

	private ArrayList<QualityLevelEntry> qualityLevelElements = new ArrayList<QualityLevelEntry>();
	private ArrayList<ClientEntry> clients = new ArrayList<ClientEntry>();
	private ArchiveInformation lastArchive;

	private boolean downloadFinished;

	private JLabel lblThanks;

	private boolean wasCancelled = true;
	private boolean warnedArchiveChange = false;

	private boolean updateDescriptor = true;

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
	}

	public boolean showDialog() throws IOException {
		clients.clear();
		lastArchive = null;

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
					break;
				}
			}
			checkBoxDownload.setSelected(!streaming);
		} else if (AssetManager.getArchives().length > 0) {
			lastArchive = AssetManager.getArchives()[0];
			checkBoxDownload.setEnabled(
					!lastArchive.isDeprecated && lastArchive.supportsDownloads && lastArchive.supportsStreaming);
			checkBoxDownload.setSelected(!lastArchive.supportsStreaming);
			lblQualityLevels.setVisible(checkBoxDownload.isSelected());
			qualityLevelBox.setVisible(checkBoxDownload.isSelected());
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

		archiveSelector = new JComboBox<ArchiveInformation>();
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
						lblQualityLevels.setVisible(false);
						qualityLevelBox.setVisible(false);
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
				// Check
				if (checkBoxDownload.isSelected()) {
					// Disable
					boolean wasEnabledRemove = btnRemove.isEnabled();
					btnOk.setEnabled(false);
					qualityLevelBox.setEnabled(false);
					btnCancel.setEnabled(false);
					btnAdd.setEnabled(false);
					boolean chBoxWasEnabled = checkBoxDownload.isEnabled();
					checkBoxDownload.setEnabled(false);
					clientListBox.setEnabled(false);
					btnRemove.setEnabled(false);

					// Set text
					btnOk.setText("Busy...");

					// Collect assets
					Thread th = new Thread(() -> {
						try {
							// Update descriptor
							if (updateDescriptor) {
								if (lastArchive.mode == ArchiveMode.REMOTE) {
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
												int percentage = (int) ((100f
														/ (float) (LauncherUtils.getProgressMax()))
														* (float) LauncherUtils.getProgress());
												if (!lastMsg.equals("Downloading archive descriptor... Please wait... ["
														+ percentage + "%]")) {
													lastMsg = "Downloading archive descriptor... Please wait... ["
															+ percentage + "%]";
													SwingUtilities.invokeAndWait(() -> {
														if (!downloadFinished) {
															lblThanks.setText(
																	"Downloading archive descriptor... Please wait... ["
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
											AssetManager.getSentinelAssetControllerConfig().get("descriptorRoot")
													.getAsString(),
											LauncherUtils.urlBaseDescriptorFile, LauncherUtils.urlBaseSoftwareFile,
											AssetManager.getAssetInformationRootURL());
									String rHashDescriptor = LauncherUtils
											.downloadString(dir + lastArchive.descriptorType + ".hash")
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
											if (!AssetManager.getSentinelAssetControllerConfig()
													.get("allowUnsignedArchiveDescriptors").getAsBoolean()) {
												LauncherUtils.log("Package is unsigned.");
												JOptionPane.showMessageDialog(VersionManagerWindow.this,
														"The archive descriptor is unsigned and this game descriptor does not support unsigned archive descriptors.\n\nPlease report this error to the project's archival team.",
														"Update error", JOptionPane.ERROR_MESSAGE);

												// Re-enable
												SwingUtilities.invokeLater(() -> {
													btnOk.setEnabled(true);
													btnCancel.setEnabled(true);
													btnAdd.setEnabled(true);
													checkBoxDownload.setEnabled(chBoxWasEnabled);
													qualityLevelBox.setEnabled(true);
													clientListBox.setEnabled(true);
													btnRemove.setEnabled(wasEnabledRemove);
													btnOk.setText("Ok");
													lblThanks.setVisible(lastArchive != null
															&& lastArchive.archiveDef.has("thanksTo"));
													if (lastArchive != null && lastArchive.archiveDef.has("thanksTo"))
														lblThanks.setText("Assets kindly mirrored by "
																+ lastArchive.archiveDef.get("thanksTo").getAsString());
												});
												return;
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
												btnAdd.setEnabled(true);
												checkBoxDownload.setEnabled(chBoxWasEnabled);
												qualityLevelBox.setEnabled(true);
												clientListBox.setEnabled(true);
												btnRemove.setEnabled(wasEnabledRemove);
												btnOk.setText("Ok");
												lblThanks.setVisible(
														lastArchive != null && lastArchive.archiveDef.has("thanksTo"));
												if (lastArchive != null && lastArchive.archiveDef.has("thanksTo"))
													lblThanks.setText("Assets kindly mirrored by "
															+ lastArchive.archiveDef.get("thanksTo").getAsString());
											});
											return;
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
												int percentage = (int) ((100f
														/ (float) (LauncherUtils.getProgressMax()))
														* (float) LauncherUtils.getProgress());
												if (!lastMsg.equals("Extracting archive information... Please wait... ["
														+ percentage + "%]")) {
													lastMsg = "Extracting archive information... Please wait... ["
															+ percentage + "%]";
													SwingUtilities.invokeAndWait(() -> {
														if (!downloadFinished) {
															lblThanks.setText(
																	"Extracting archive information... Please wait... ["
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
									LauncherUtils.unZip(new File("assets/descriptor.zip"),
											new File("assets/descriptor"));
									downloadFinished = true;

									// Write hash
									Files.writeString(Path.of("assets/descriptor.hash"), rHashDescriptor);

									// Hide bars
									LauncherUtils.hideProgressPanel();
									LauncherUtils.resetProgressBar();
									updateDescriptor = false;
								} else {
									// Re-extract descriptor
									LauncherUtils.log("Re-extracting archive descriptor...", true);
									SwingUtilities.invokeAndWait(() -> {
										if (!downloadFinished) {
											lblThanks
													.setText("Re-extracting archive descriptor... Please wait... [0%]");
											lblThanks.setVisible(true);
										}
									});

									// TODO: support

									// Hide bars
									LauncherUtils.hideProgressPanel();
									LauncherUtils.resetProgressBar();

									// Show error
									LauncherUtils.log("Presently unsupported!");
									JOptionPane.showMessageDialog(VersionManagerWindow.this,
											"Failed to extract asset archive descriptor data as the system has not been implemented yet",
											"Load error", JOptionPane.ERROR_MESSAGE);

									// Re-enable
									SwingUtilities.invokeLater(() -> {
										btnOk.setEnabled(true);
										btnCancel.setEnabled(true);
										btnAdd.setEnabled(true);
										checkBoxDownload.setEnabled(chBoxWasEnabled);
										qualityLevelBox.setEnabled(true);
										clientListBox.setEnabled(true);
										btnRemove.setEnabled(wasEnabledRemove);
										btnOk.setText("Ok");
										lblThanks.setVisible(
												lastArchive != null && lastArchive.archiveDef.has("thanksTo"));
										if (lastArchive != null && lastArchive.archiveDef.has("thanksTo"))
											lblThanks.setText("Assets kindly mirrored by "
													+ lastArchive.archiveDef.get("thanksTo").getAsString());
									});
									return;

//									// Write hash
//									Files.writeString(Path.of("assets/descriptor.hash"), rHashDescriptor);

//									// Hide bars
//									LauncherUtils.hideProgressPanel();
//									LauncherUtils.resetProgressBar();
//									updateDescriptor = false;
								}
							}

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
										btnAdd.setEnabled(true);
										checkBoxDownload.setEnabled(chBoxWasEnabled);
										qualityLevelBox.setEnabled(true);
										clientListBox.setEnabled(true);
										btnRemove.setEnabled(wasEnabledRemove);
										btnOk.setText("Ok");
										lblThanks.setVisible(lastArchive != null);
										if (lastArchive != null)
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
								btnAdd.setEnabled(true);
								checkBoxDownload.setEnabled(chBoxWasEnabled);
								qualityLevelBox.setEnabled(true);
								clientListBox.setEnabled(true);
								btnRemove.setEnabled(wasEnabledRemove);
								btnOk.setText("Ok");
								lblThanks.setVisible(lastArchive != null);
								if (lastArchive != null)
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
								btnAdd.setEnabled(true);
								checkBoxDownload.setEnabled(chBoxWasEnabled);
								qualityLevelBox.setEnabled(true);
								clientListBox.setEnabled(true);
								btnRemove.setEnabled(wasEnabledRemove);
								btnOk.setText("Ok");
								lblThanks.setVisible(lastArchive != null);
								if (lastArchive != null)
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

				// Close
				doSave();
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

			private void doSave() {
				try {
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
}
