package org.asf.razorwhip.sentinel.launcher.windows;

import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.SystemColor;

import javax.swing.JPanel;
import javax.swing.border.BevelBorder;
import javax.swing.event.ListDataListener;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.awt.event.ActionEvent;
import javax.swing.JLabel;
import javax.swing.JCheckBox;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.ScrollPaneConstants;

import org.asf.razorwhip.sentinel.launcher.experiments.ExperimentManager;
import javax.swing.SwingConstants;

public class ExperimentManagerWindow extends JDialog {

	private static final long serialVersionUID = 1l;

	private JList<ExperimentEntry> experimentListBox;
	private JButton btnOk;

	private LinkedHashMap<String, ExperimentEntry> experiments = new LinkedHashMap<String, ExperimentEntry>();
	private boolean wasCancelled = true;

	private class ExperimentEntry {
		public String key;
		public String name;

		public long timeSelect;
		public boolean enabled;
		public JCheckBox box;
	}

	/**
	 * Create the application.
	 */
	public ExperimentManagerWindow() {
		initialize();
	}

	public ExperimentManagerWindow(JFrame parent) {
		super(parent);
		initialize();
		setLocationRelativeTo(parent);
	}

	public boolean showDialog() {
		// Load experiment list
		refreshExperimentList();

		// Show
		setVisible(true);

		// Return
		return !wasCancelled;
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		setTitle("Experiment manager");
		setBounds(100, 100, 600, 659);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setLocationRelativeTo(null);
		setResizable(false);
		setModal(true);
		getContentPane().setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));

		JPanel panelMain = new JPanel();
		panelMain.setPreferredSize(new Dimension(590, 615));
		getContentPane().add(panelMain);
		panelMain.setLayout(null);

		JPanel panelExperiments = new JPanel();
		panelMain.add(panelExperiments);
		panelExperiments.setLayout(null);
		panelExperiments.setVisible(ExperimentManager.getInstance().isEnabled());
		panelExperiments.setBounds(0, 0, 590, 615);

		JPanel panelExperimentsDisabled = new JPanel();
		panelMain.add(panelExperimentsDisabled);
		panelExperimentsDisabled.setLayout(null);
		panelExperimentsDisabled.setVisible(!ExperimentManager.getInstance().isEnabled());
		panelExperimentsDisabled.setBounds(0, 0, 590, 615);

		JLabel lblNewLabel_1 = new JLabel("Experimental features are not enabled");
		lblNewLabel_1.setHorizontalAlignment(SwingConstants.CENTER);
		lblNewLabel_1.setBounds(12, 169, 566, 54);
		panelExperimentsDisabled.add(lblNewLabel_1);

		JButton btnCancel_1 = new JButton("Cancel");
		btnCancel_1.setBounds(12, 576, 105, 27);
		panelExperimentsDisabled.add(btnCancel_1);
		btnCancel_1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});

		JButton btnNewButton = new JButton("Enable experimental features");
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// Warn
				if (JOptionPane.showConfirmDialog(ExperimentManagerWindow.this, "WARNING!\n" + "\n" //
						+ "Experimenal features, while allowing you access to work-in-progress features and mechanics,\n" //
						+ "are by nature unstable! Enabling them may lead to unwanted issues with your game!\n" //
						+ "\n" //
						+ "Please note that we do not actively support all experimental features, if you encounter issues,\n"
						+ "please disable all experimental features and trying again before creating bug reports.",
						"Enabling experimental features", JOptionPane.OK_CANCEL_OPTION,
						JOptionPane.WARNING_MESSAGE) != JOptionPane.OK_OPTION)
					return;

				// Confirm
				if (JOptionPane.showConfirmDialog(ExperimentManagerWindow.this, "Enable experimental features?",
						"Enabling experimental features", JOptionPane.YES_NO_OPTION,
						JOptionPane.QUESTION_MESSAGE) != JOptionPane.YES_OPTION)
					return;

				// Notify
				if (JOptionPane.showConfirmDialog(ExperimentManagerWindow.this,
						"Experimental features will be enabled!\n\nPress OK to proceed.",
						"Enabling experimental features", JOptionPane.OK_CANCEL_OPTION,
						JOptionPane.WARNING_MESSAGE) != JOptionPane.OK_OPTION)
					return;

				// Enable
				try {
					ExperimentManager.getInstance().enable();
				} catch (IOException e1) {
					throw new RuntimeException(e1);
				}

				// Switch
				panelExperimentsDisabled.setVisible(false);
				panelExperiments.setVisible(true);
			}
		});
		btnNewButton.setBounds(32, 235, 517, 45);
		panelExperimentsDisabled.add(btnNewButton);

		JLabel lblNewLabel = new JLabel("Experiments");
		lblNewLabel.setBounds(12, 12, 566, 17);
		panelExperiments.add(lblNewLabel);

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane.setBounds(12, 33, 566, 531);
		panelExperiments.add(scrollPane);

		btnOk = new JButton("Ok");
		btnOk.setBounds(473, 576, 105, 27);
		panelExperiments.add(btnOk);

		JButton btnCancel = new JButton("Cancel");
		btnCancel.setBounds(12, 576, 105, 27);
		panelExperiments.add(btnCancel);

		experimentListBox = new JList<ExperimentEntry>();
		experimentListBox.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_SPACE || e.getKeyCode() == KeyEvent.VK_ENTER) {
					e.consume();
					if (experimentListBox.getSelectedValue() != null) {
						ExperimentEntry entry = experimentListBox.getSelectedValue();
						entry.enabled = !entry.enabled;
						entry.box.setEnabled(entry.enabled);
						experimentListBox.repaint();
					}
				}
			}
		});
		experimentListBox.addMouseListener(new MouseAdapter() {
			public synchronized void mouseClicked(MouseEvent evt) {
				if (evt.getClickCount() >= 1) {
					Point mouse = evt.getPoint();
					int index = experimentListBox.locationToIndex(mouse);
					Point l = experimentListBox.indexToLocation(index);
					if (index >= 0 && index < experiments.size()) {
						Rectangle bounds = experimentListBox.getCellBounds(index, index);
						ExperimentEntry entry = experiments.values().toArray(new ExperimentEntry[0])[index];
						if (experimentListBox.getSelectedValue() == entry && entry.timeSelect != -1
								&& (((System.currentTimeMillis() - entry.timeSelect) >= 250)
										|| evt.getClickCount() >= 2)
								&& (l.y + bounds.height) >= mouse.y) {
							// Change state
							entry.enabled = !entry.enabled;
							entry.box.setEnabled(entry.enabled);
							experimentListBox.repaint();
						}
					}
				}
			}
		});
		experimentListBox.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		experimentListBox.setCellRenderer(new ListCellRenderer<ExperimentEntry>() {
			@Override
			public Component getListCellRendererComponent(JList<? extends ExperimentEntry> list, ExperimentEntry entry,
					int index, boolean selected, boolean cellHasFocus) {
				entry.box.setComponentOrientation(list.getComponentOrientation());
				entry.box.setFont(list.getFont());
				entry.box.setBackground(list.getBackground());
				if (cellHasFocus) {
					entry.box.setForeground(SystemColor.blue);
					entry.timeSelect = System.currentTimeMillis();
				} else {
					entry.box.setForeground(SystemColor.black);
					entry.timeSelect = -1;
				}
				entry.box.setSelected(entry.enabled);
				entry.box.setEnabled(true);
				return entry.box;
			}
		});
		scrollPane.setViewportView(experimentListBox);

		// Add events
		btnCancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});
		btnOk.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					// Apply
					String[] experimentLst = ExperimentManager.getInstance().getExperiments();
					for (String experiment : experimentLst) {
						// Switch states
						if (experiments.containsKey(experiment))
							ExperimentManager.getInstance().setExperimentEnabled(experiment,
									experiments.get(experiment).enabled);
					}

					// Save
					ExperimentManager.getInstance().saveConfig();

					// Close
					wasCancelled = false;
					dispose();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});
	}

	private void refreshExperimentList() {
		// Clear and populate
		experiments.clear();
		String[] experimentLst = ExperimentManager.getInstance().getExperiments();
		for (String experiment : experimentLst) {
			// Create payload object
			ExperimentEntry payload = new ExperimentEntry();
			payload.key = experiment;
			payload.name = ExperimentManager.getInstance().getExperimentName(experiment);
			payload.enabled = ExperimentManager.getInstance().isExperimentEnabled(experiment);

			// Create box
			payload.box = new JCheckBox(payload.name);
			experiments.put(payload.key, payload);
		}

		// Update
		experimentListBox.setModel(new ListModel<ExperimentEntry>() {

			@Override
			public int getSize() {
				return experiments.size();
			}

			@Override
			public ExperimentEntry getElementAt(int index) {
				return experiments.values().toArray(t -> new ExperimentEntry[t])[index];
			}

			@Override
			public void addListDataListener(ListDataListener l) {
			}

			@Override
			public void removeListDataListener(ListDataListener l) {
			}

		});
	}
}
