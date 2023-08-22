package org.asf.razorwhip.sentinel.launcher.windows;

import java.awt.FlowLayout;
import java.awt.SystemColor;

import javax.swing.JPanel;
import javax.swing.border.BevelBorder;
import javax.swing.event.ListDataListener;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.awt.event.ActionEvent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JCheckBox;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.JList;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;

import org.asf.razorwhip.sentinel.launcher.LauncherUtils;
import org.asf.razorwhip.sentinel.launcher.api.PayloadType;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.swing.event.ListSelectionEvent;

public class PayloadManagerWindow extends JDialog {

	private static final long serialVersionUID = 1l;

	private JList<PayloadEntry> payloadListBox;
	private JButton btnOk;

	private LinkedHashMap<String, PayloadEntry> payloads = new LinkedHashMap<String, PayloadEntry>();

	private boolean wasCancelled = true;
	private JsonObject lastIndex = new JsonObject();

	private class PayloadDependency {
		public String id;
		public String version;
		public String versionString;

		public String url;
	}

	private class PayloadEntry {
		public String file;

		public String id;
		public String name;
		public String version;

		public PayloadType type;

		public PayloadDependency[] dependencies;
		public PayloadDependency[] conflictsWith;

		public boolean enabled;
		public JCheckBox box;
	}

	/**
	 * Create the application.
	 */
	public PayloadManagerWindow() {
		initialize();
	}

	public PayloadManagerWindow(JFrame parent) {
		super(parent);
		initialize();
	}

	public boolean showDialog() throws IOException {
		payloads.clear();

		// Load payloads
		JsonObject index = new JsonObject();
		if (new File("activepayloads.json").exists()) {
			index = JsonParser.parseString(Files.readString(Path.of("activepayloads.json"))).getAsJsonObject();
		}

		// Discover SPF files
		for (File spf : new File("payloads").listFiles(t -> t.isFile() && t.getName().endsWith(".spf"))) {
			boolean enabled = index.has(spf.getName()) && index.get(spf.getName()).getAsBoolean();

			// Load SPF file descriptor
			Map<String, String> descriptor = LauncherUtils.parseProperties(getStringFrom(spf, "payloadinfo"));

			// Create payload object
			PayloadEntry payload = new PayloadEntry();
			payload.id = spf.getName();
			payload.name = spf.getName();
			payload.version = "default";
			payload.file = spf.getName();
			payload.enabled = enabled;

			// Check type
			String type = "Full";
			if (descriptor.containsKey("Type"))
				type = descriptor.get("Type");
			type = type.toLowerCase();

			// Check
			if (!type.equals("resource") && !type.equals("full")) {
				// Incompatible
				index.remove(spf.getName());
				continue;
			}

			// Set type
			payload.type = type.equals("resource") ? PayloadType.RESOURCE : PayloadType.PAYLOAD;

			// Load settings
			if (descriptor.containsKey("Payload-ID")) {
				payload.id = descriptor.get("Payload-ID");
				payload.name = payload.id;
			}
			if (descriptor.containsKey("Payload-Name"))
				payload.name = descriptor.get("Payload-Name");
			if (descriptor.containsKey("Payload-Version"))
				payload.version = descriptor.get("Payload-Version");

			// Check compatibility
			if (descriptor.containsKey("Game-ID")) {
				// Check
				if (!descriptor.get("Game-ID").equalsIgnoreCase(LauncherUtils.getGameID())) {
					// Incompatible
					index.remove(spf.getName());
					continue;
				}
			}
			if (descriptor.containsKey("Software-ID")) {
				// Check
				if (!descriptor.get("Software-ID").equalsIgnoreCase(LauncherUtils.getSoftwareID())) {
					// Incompatible
					index.remove(spf.getName());
					continue;
				}
			}

			// Load dependencies
			payload.dependencies = new PayloadDependency[0];
			payload.conflictsWith = new PayloadDependency[0];
			try {
				JsonObject depsConfig = JsonParser.parseString(getStringFrom(spf, "dependencies.json"))
						.getAsJsonObject();

				// Prepare lists
				ArrayList<PayloadDependency> deps = new ArrayList<PayloadDependency>();
				ArrayList<PayloadDependency> conflicts = new ArrayList<PayloadDependency>();

				// Read from config
				if (depsConfig.has("dependencies")) {
					for (JsonElement ele : depsConfig.get("dependencies").getAsJsonArray()) {
						JsonObject dep = ele.getAsJsonObject();

						// Add
						PayloadDependency dependency = new PayloadDependency();
						dependency.id = dep.get("id").getAsString();
						if (dep.has("version")) {
							dependency.version = dep.get("version").getAsString();
							if (dep.has("versionString"))
								dependency.versionString = dep.get("versionString").getAsString();
							else
								dependency.versionString = dependency.version;
						}
						if (dep.has("url"))
							dependency.url = dep.get("url").getAsString();
						deps.add(dependency);
					}
				}
				if (depsConfig.has("conflicts")) {
					for (JsonElement ele : depsConfig.get("conflicts").getAsJsonArray()) {
						JsonObject dep = ele.getAsJsonObject();

						// Add
						PayloadDependency conflict = new PayloadDependency();
						conflict.id = dep.get("id").getAsString();
						if (dep.has("version"))
							conflict.version = dep.get("version").getAsString();
						conflicts.add(conflict);
					}
				}

				// Apply
				payload.dependencies = deps.toArray(t -> new PayloadDependency[t]);
				payload.conflictsWith = conflicts.toArray(t -> new PayloadDependency[t]);
			} catch (IOException e) {
			}

			// Create box
			payload.box = new JCheckBox(payload.name + " (" + payload.type.toString().toLowerCase() + ")");

			// Add
			if (payloads.containsKey(payload.id)) {
				// Warn
				JOptionPane.showMessageDialog(this, "WARNING! Payload conflict detected!\n\nPayload ID: " + payload.id
						+ "\nTwo or more files provide this payload ID.\n\nFile 1: " + payloads.get(payload.id).file
						+ "\nFile 2: " + payload.file + "\n\nThe first file will be used.", "Warning",
						JOptionPane.WARNING_MESSAGE);
				index.remove(spf.getName());
				continue;
			}
			payloads.put(payload.id, payload);
		}

		// Load payload list
		lastIndex = index;
		refreshPayloadList();

		// Show
		setVisible(true);

		// Return
		return !wasCancelled;
	}

	private String getStringFrom(File file, String entry) throws IOException {
		if (file.isDirectory()) {
			FileInputStream strm = new FileInputStream(new File(file, entry));

			// Read
			String res = new String(strm.readAllBytes(), "UTF-8");
			strm.close();

			// Return
			return res;
		}

		// Get zip
		ZipFile f = new ZipFile(file);
		try {
			ZipEntry ent = f.getEntry(entry);
			if (ent == null) {
				throw new FileNotFoundException("Entry " + entry + " not found in " + file);
			}

			// Get stream
			InputStream strm = f.getInputStream(ent);

			// Read
			String res = new String(strm.readAllBytes(), "UTF-8");
			strm.close();

			// Return
			return res;
		} finally {
			f.close();
		}
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		setTitle("Payload manager");
		setBounds(100, 100, 600, 360);
		setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		setLocationRelativeTo(null);
		setResizable(false);
		setModal(true);
		getContentPane().setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));

		JPanel panel = new JPanel();
		panel.setPreferredSize(new Dimension(590, 315));
		getContentPane().add(panel);
		panel.setLayout(null);

		btnOk = new JButton("Ok");
		btnOk.setBounds(473, 276, 105, 27);
		panel.add(btnOk);

		JButton btnCancel = new JButton("Cancel");
		btnCancel.setBounds(356, 276, 105, 27);
		panel.add(btnCancel);

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane.setBounds(12, 33, 566, 189);
		panel.add(scrollPane);

		payloadListBox = new JList<PayloadEntry>();
		payloadListBox.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_SPACE || e.getKeyCode() == KeyEvent.VK_ENTER) {
					e.consume();
					if (payloadListBox.getSelectedValue() != null) {
						PayloadEntry entry = payloadListBox.getSelectedValue();
						entry.enabled = !entry.enabled;
						entry.box.setEnabled(entry.enabled);
						payloadListBox.repaint();

						// Cascade
						if (entry.enabled) {
							// Enable dependencies
							enableDependencies(entry);
						} else {
							// Disable dependent
							disableDependent(entry);
						}
					}
				}
			}
		});
		payloadListBox.addMouseListener(new MouseAdapter() {
			public synchronized void mouseClicked(MouseEvent evt) {
				if (evt.getClickCount() == 1) {
					int index = payloadListBox.locationToIndex(evt.getPoint());
					if (index >= 0 && index < payloads.size()) {
						PayloadEntry entry = payloads.values().toArray(new PayloadEntry[0])[index];
						entry.enabled = !entry.enabled;
						entry.box.setEnabled(entry.enabled);
						payloadListBox.repaint();

						// Cascade
						if (entry.enabled) {
							// Enable dependencies
							enableDependencies(entry);
						} else {
							// Disable dependent
							disableDependent(entry);
						}
					}
				}
			}
		});
		payloadListBox.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		payloadListBox.setCellRenderer(new ListCellRenderer<PayloadEntry>() {
			@Override
			public Component getListCellRendererComponent(JList<? extends PayloadEntry> list, PayloadEntry entry,
					int index, boolean selected, boolean cellHasFocus) {
				entry.box.setComponentOrientation(list.getComponentOrientation());
				entry.box.setFont(list.getFont());
				entry.box.setBackground(list.getBackground());
				if (cellHasFocus)
					entry.box.setForeground(SystemColor.blue);
				else
					entry.box.setForeground(SystemColor.black);
				entry.box.setSelected(entry.enabled);
				entry.box.setEnabled(true);
				return entry.box;
			}
		});
		scrollPane.setViewportView(payloadListBox);

		JLabel lblNewLabel = new JLabel("Payloads");
		lblNewLabel.setBounds(12, 12, 566, 17);
		panel.add(lblNewLabel);

		JButton btnAdd = new JButton("Add payload...");
		btnAdd.setBounds(422, 234, 156, 27);
		panel.add(btnAdd);

		JButton btnRemove = new JButton("Delete");
		btnRemove.setEnabled(false);
		btnRemove.setBounds(12, 234, 105, 27);
		panel.add(btnRemove);

		// Add events
		btnCancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JsonObject newIndex = new JsonObject();
				for (PayloadEntry payload : payloads.values())
					newIndex.addProperty(payload.file, payload.enabled);
				if (!newIndex.toString().equals(lastIndex.toString())) {
					if (JOptionPane.showConfirmDialog(PayloadManagerWindow.this,
							"Are you sure you want to close the payload manager without saving?", "Warning",
							JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
						return;
					}
				}
				dispose();
			}
		});
		btnOk.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					while (true) {
						// Verify payloads
						boolean allPassed = true;
						for (PayloadEntry payload : payloads.values()) {
							if (!payload.enabled)
								continue;

							// Check dependencies
							for (PayloadDependency dep : payload.dependencies) {
								while (true) {
									if (!payloads.containsKey(dep.id)) {
										// Failed
										allPassed = false;

										// Check URL
										if (dep.url == null) {
											// Error
											JOptionPane.showMessageDialog(PayloadManagerWindow.this,
													"Missing payload dependencies!" //
															+ "\n" //
															+ "\nPayload ID: " + payload.id //
															+ "\nPayload file: " + payload.file //
															+ "\n" //
															+ "\nMissing dependency: " + dep.id //
															+ "\n" //
															+ "\nUnable to save at this time, please resolve these issues.",
													"Missing dependencies", JOptionPane.ERROR_MESSAGE);
											return;
										} else {
											if (JOptionPane.showConfirmDialog(PayloadManagerWindow.this,
													"Missing payload dependencies were detected." //
															+ "\n" //
															+ "\nPayload ID: " + payload.id //
															+ "\nPayload file: " + payload.file //
															+ "\n" //
															+ "\nMissing dependency: " + dep.id //
															+ "\n" //
															+ "\nDo you wish to download this payload file?" //
															+ "\nIf you select no, saving will be cancelled.",
													"Missing dependencies", JOptionPane.YES_NO_OPTION,
													JOptionPane.QUESTION_MESSAGE) != JOptionPane.YES_OPTION) {
												return;
											}

											// Download file
											if (JOptionPane.showConfirmDialog(PayloadManagerWindow.this,
													"Payload will be downloaded, this may take a bit...\n\nPress OK to start downloading.",
													"Payload download", JOptionPane.OK_CANCEL_OPTION,
													JOptionPane.INFORMATION_MESSAGE) == JOptionPane.CANCEL_OPTION) {
												continue;
											}

											// Download payload
											try {
												URL u = new URL(dep.url);
												File tempOut = File.createTempFile("payload-download-temp-", ".spf");
												try {
													InputStream sourceStrm = u.openStream();
													FileOutputStream fO = new FileOutputStream(tempOut);
													sourceStrm.transferTo(fO);
													fO.close();
													sourceStrm.close();

													// Add the payload
													// Should throw exception on any errors
													importPayload(tempOut, true);

													// Success
													// Set enabled
													payloads.get(dep.id).enabled = true;
													payloads.get(dep.id).box.setSelected(true);
												} finally {
													tempOut.delete();
												}

												// Success
												JOptionPane.showMessageDialog(PayloadManagerWindow.this,
														"Download success!", "Download status",
														JOptionPane.INFORMATION_MESSAGE);
											} catch (IOException e2) {
												JOptionPane.showMessageDialog(PayloadManagerWindow.this,
														"Download failed! Unable to save the new payload!",
														"Download status", JOptionPane.ERROR_MESSAGE);
												return;
											}
										}
									} else {
										// Check version if needed
										PayloadEntry dependency = payloads.get(dep.id);
										if (dep.version != null) {
											// Check version
											if (!LauncherUtils.verifyVersionRequirement(dependency.version,
													dep.version)) {
												// Failed
												allPassed = false;

												// Error
												JOptionPane.showMessageDialog(PayloadManagerWindow.this,
														"Payload dependency mismatch!" //
																+ "\n" //
																+ "\nPayload ID: " + payload.id //
																+ "\nPayload file: " + payload.file //
																+ "\n" //
																+ "\nFailed dependency: " + dep.id + "\n" //
																+ "\nExpected version: " + dep.versionString //
																+ "\nCurrent version: " + dependency.version //
																+ "\n" //
																+ "\nUnable to save at this time, please resolve these issues.",
														"Dependency mismatch", JOptionPane.ERROR_MESSAGE);
												return;
											}
										}

										// Success
										break;
									}
								}

							}

							// Check conflicts
							for (PayloadDependency conflict : payload.conflictsWith) {
								if (payloads.containsKey(conflict.id)) {
									// Check
									if (conflict.version == null) {
										// Error
										JOptionPane.showMessageDialog(PayloadManagerWindow.this, "Conflict detected!" //
												+ "\n" //
												+ "\nPayload ID: " + payload.id //
												+ "\nPayload name: " + payload.name //
												+ "\nPayload file: " + payload.file //
												+ "\n" //
												+ "\nConflicting payload: " + conflict.id + "\n" //
												+ "\nThe payload '" + payload.id + "' cannot run with '" + conflict.id
												+ "' installed!" //
												+ "\n" //
												+ "\nUnable to save at this time, please resolve these issues.",
												"Dependency mismatch", JOptionPane.ERROR_MESSAGE);
										return;
									}
								} else {
									// Check version
									if (!LauncherUtils.verifyVersionRequirement(payloads.get(conflict.id).version,
											conflict.version)) {
										// Error
										JOptionPane.showMessageDialog(PayloadManagerWindow.this, "Conflict detected!" //
												+ "\n" //
												+ "\nPayload ID: " + payload.id //
												+ "\nPayload name: " + payload.name //
												+ "\nPayload file: " + payload.file //
												+ "\n" //
												+ "\nConflicting payload: " + conflict.id + "\n" //
												+ "\nAffected versions: " + conflict.versionString + "\n" //
												+ "\n" //
												+ "\nThe payload '" + payload.id
												+ "' cannot run with the current version of '" + conflict.id + "'!" //
												+ "\n" //
												+ "\nUnable to save at this time, please resolve these issues.",
												"Dependency mismatch", JOptionPane.ERROR_MESSAGE);
										return;
									}
								}
							}
						}

						// Break if success
						if (allPassed)
							break;
					}

					// Save
					JsonObject newIndex = new JsonObject();
					for (PayloadEntry payload : payloads.values())
						newIndex.addProperty(payload.file, payload.enabled);
					if (!newIndex.toString().equals(lastIndex.toString()))
						Files.writeString(Path.of("activepayloads.json"), newIndex.toString());

					// Close
					wasCancelled = false;
					dispose();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});
		payloadListBox.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				btnRemove.setEnabled(payloadListBox.getSelectedValue() != null);
			}
		});
		btnAdd.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// Show window
				JFileChooser chooser = new JFileChooser();
				FileFilter filter = new FileFilter() {

					@Override
					public boolean accept(File f) {
						return f.isDirectory() || f.getName().endsWith(".spf");
					}

					@Override
					public String getDescription() {
						return "Sentinel payload files (*.spf)";
					}

				};
				chooser.addChoosableFileFilter(filter);
				chooser.setFileFilter(filter);
				if (chooser.showDialog(PayloadManagerWindow.this, "Select") == JFileChooser.APPROVE_OPTION) {
					// Check file
					File spf = chooser.getSelectedFile();
					if (spf != null) {
						try {
							importPayload(spf, false);
						} catch (IOException e1) {
						}
					}
				}
			}
		});
		btnRemove.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (JOptionPane.showConfirmDialog(PayloadManagerWindow.this,
						"Are you sure you want to remove this payload?\n\nRemoving a payload will delete all the payloads that are dependent on it!",
						"Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
					return;
				}

				// Remove
				PayloadEntry entry = (PayloadEntry) payloadListBox.getSelectedValue();
				removePayload(entry);

				// Refresh
				refreshPayloadList();
			}

			private void removePayload(PayloadEntry payload) {
				// Remove
				payloads.remove(payload.id);

				// Remove dependent payloads
				for (PayloadEntry dep : payloads.values().toArray(t -> new PayloadEntry[t])) {
					// Remove if needed
					if (Stream.of(dep.dependencies).anyMatch(t -> t.id.equals(payload.id)))
						removePayload(dep);
				}
			}
		});
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				JsonObject newIndex = new JsonObject();
				for (PayloadEntry payload : payloads.values())
					newIndex.addProperty(payload.file, payload.enabled);
				if (!newIndex.toString().equals(lastIndex.toString())) {
					if (JOptionPane.showConfirmDialog(PayloadManagerWindow.this,
							"Are you sure you want to close the payload manager without saving?", "Warning",
							JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
						return;
					}
				}
				dispose();
			}
		});
	}

	protected boolean importPayload(File spf, boolean throwError) throws IOException {
		try {
			// Read descriptor
			Map<String, String> descriptor = LauncherUtils.parseProperties(getStringFrom(spf, "payloadinfo"));

			// Verify descriptor validity
			String type = "Full";
			if (descriptor.containsKey("Type"))
				type = descriptor.get("Type");
			type = type.toLowerCase();
			if (!type.equals("resource") && !type.equals("full")) {
				// Incompatible
				JOptionPane.showMessageDialog(PayloadManagerWindow.this,
						"The payload you selected has an invalid type!", "Error", JOptionPane.ERROR_MESSAGE);
				return false;
			}
			if (descriptor.containsKey("Game-ID")) {
				// Check
				if (!descriptor.get("Game-ID").equalsIgnoreCase(LauncherUtils.getGameID())) {
					// Incompatible
					if (throwError)
						throw new IOException("Incompatible payload");
					JOptionPane.showMessageDialog(PayloadManagerWindow.this,
							"The payload you selected is not compatible with the game or emulation software!", "Error",
							JOptionPane.ERROR_MESSAGE);
					return false;
				}
			}
			if (descriptor.containsKey("Software-ID")) {
				// Check
				if (!descriptor.get("Software-ID").equalsIgnoreCase(LauncherUtils.getSoftwareID())) {
					// Incompatible
					if (throwError)
						throw new IOException("Incompatible payload");
					JOptionPane.showMessageDialog(PayloadManagerWindow.this,
							"The payload you selected is not compatible with the game or emulation software!", "Error",
							JOptionPane.ERROR_MESSAGE);
					return false;
				}
			}

			// Load ID
			String id = spf.getName();
			if (descriptor.containsKey("Payload-ID"))
				id = descriptor.get("Payload-ID");

			// Check
			if (payloads.containsKey(id)) {
				// Incompatible
				if (throwError)
					throw new IOException("Payload conflict");
				JOptionPane.showMessageDialog(PayloadManagerWindow.this, "The payload you selected is already present!",
						"Error", JOptionPane.ERROR_MESSAGE);
				return false;
			}

			// Verify signature
			if (LauncherUtils.isPackageSigned(spf) && LauncherUtils
					.verifyPackageSignature(new File("payloadcache/payloadverificationkeys", id + ".pem"), spf)) {
				LauncherUtils.extractPackagePublicKey(new File("payloadcache/payloadverificationkeys", id + ".pem"),
						spf);
			} else {
				if (throwError)
					throw new IOException("Incompatible payload");
				JOptionPane.showMessageDialog(PayloadManagerWindow.this,
						"Payload signature failed to verify! Please verify the authenticity of this payload!", "Error",
						JOptionPane.ERROR_MESSAGE);
				return false;
			}

			// Add
			String name = (id.endsWith(".spf") ? id : id + ".spf");
			int i = 1;
			while (new File("payloads", name).exists()) {
				name = id + "-" + i++ + ".spf";
			}

			// Copy file
			Files.copy(spf.toPath(), new File("payloads", name).toPath());
			spf = new File("payloads", name);

			// Add
			PayloadEntry payload = new PayloadEntry();
			payload.id = name;
			payload.name = name;
			payload.version = "default";
			payload.file = name;
			payload.enabled = false;

			// Set type
			payload.type = type.equals("resource") ? PayloadType.RESOURCE : PayloadType.PAYLOAD;

			// Load settings
			if (descriptor.containsKey("Payload-ID")) {
				payload.id = descriptor.get("Payload-ID");
				payload.name = payload.id;
			}
			if (descriptor.containsKey("Payload-Name"))
				payload.name = descriptor.get("Payload-Name");
			if (descriptor.containsKey("Payload-Version"))
				payload.version = descriptor.get("Payload-Version");

			// Load dependencies
			payload.dependencies = new PayloadDependency[0];
			payload.conflictsWith = new PayloadDependency[0];
			try {
				JsonObject depsConfig = JsonParser.parseString(getStringFrom(spf, "dependencies.json"))
						.getAsJsonObject();

				// Prepare lists
				ArrayList<PayloadDependency> deps = new ArrayList<PayloadDependency>();
				ArrayList<PayloadDependency> conflicts = new ArrayList<PayloadDependency>();

				// Read from config
				if (depsConfig.has("dependencies")) {
					for (JsonElement ele : depsConfig.get("dependencies").getAsJsonArray()) {
						JsonObject dep = ele.getAsJsonObject();

						// Add
						PayloadDependency dependency = new PayloadDependency();
						dependency.id = dep.get("id").getAsString();
						if (dep.has("version")) {
							dependency.version = dep.get("version").getAsString();
							if (dep.has("versionString"))
								dependency.versionString = dep.get("versionString").getAsString();
							else
								dependency.versionString = dependency.version;
						}
						if (dep.has("url"))
							dependency.url = dep.get("url").getAsString();
						deps.add(dependency);
					}
				}
				if (depsConfig.has("conflicts")) {
					for (JsonElement ele : depsConfig.get("conflicts").getAsJsonArray()) {
						JsonObject dep = ele.getAsJsonObject();

						// Add
						PayloadDependency conflict = new PayloadDependency();
						conflict.id = dep.get("id").getAsString();
						if (dep.has("version"))
							conflict.version = dep.get("version").getAsString();
						conflicts.add(conflict);
					}
				}

				// Apply
				payload.dependencies = deps.toArray(t -> new PayloadDependency[t]);
				payload.conflictsWith = conflicts.toArray(t -> new PayloadDependency[t]);
			} catch (IOException e) {
			}

			// Create box
			payload.box = new JCheckBox(payload.name + " (" + payload.type.toString().toLowerCase() + ")");

			// Add
			payloads.put(payload.id, payload);

			// Refresh
			refreshPayloadList();
		} catch (Exception e2) {
			if (throwError)
				throw new IOException("Incompatible payload");
			JOptionPane.showMessageDialog(PayloadManagerWindow.this, "Failed to load payload file!", "Error",
					JOptionPane.ERROR_MESSAGE);
			return false;
		}
		return true;
	}

	private void refreshPayloadList() {
		payloadListBox.setModel(new ListModel<PayloadEntry>() {

			@Override
			public int getSize() {
				return payloads.size();
			}

			@Override
			public PayloadEntry getElementAt(int index) {
				return payloads.values().toArray(t -> new PayloadEntry[t])[index];
			}

			@Override
			public void addListDataListener(ListDataListener l) {
			}

			@Override
			public void removeListDataListener(ListDataListener l) {
			}

		});
	}

	private void disableDependent(PayloadEntry payload) {
		for (PayloadEntry dep : payloads.values().toArray(t -> new PayloadEntry[t])) {
			// Remove if needed
			if (Stream.of(dep.dependencies).anyMatch(t -> t.id.equals(payload.id))) {
				dep.box.setSelected(false);
				dep.enabled = false;
				disableDependent(dep);
			}
		}
	}

	private void enableDependencies(PayloadEntry payload) {
		for (PayloadDependency dep : payload.dependencies) {
			if (payloads.containsKey(dep.id)) {
				PayloadEntry d = payloads.get(dep.id);
				d.box.setSelected(true);
				d.enabled = true;
				enableDependencies(d);
			}
		}
	}
}
