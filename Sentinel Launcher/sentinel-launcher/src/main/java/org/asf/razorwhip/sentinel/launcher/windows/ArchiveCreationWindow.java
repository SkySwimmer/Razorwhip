package org.asf.razorwhip.sentinel.launcher.windows;

import java.awt.FlowLayout;
import javax.swing.JPanel;
import java.awt.Dimension;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.SwingConstants;
import java.awt.Font;
import javax.swing.JCheckBox;

public class ArchiveCreationWindow {

	public JDialog frmCreateArchive;
	private JTextField archiveNameField;
	private JTextField creditsField;

	private CreationResult result;
	private JTextField archiveUrlField;

	public static class CreationResult {
		public String archiveURL;
		public String archiveName;
		public String creditsField;
		public boolean allowStreaming;
		public boolean allowDownload;
	}

	/**
	 * Create the application.
	 */
	public ArchiveCreationWindow() {
		initialize();
	}

	public CreationResult show(JDialog parent) {
		frmCreateArchive.dispose();
		if (parent == null)
			frmCreateArchive = new JDialog();
		else
			frmCreateArchive = new JDialog(parent);
		initialize();
		frmCreateArchive.setModal(true);
		frmCreateArchive.setLocationRelativeTo(null);
		frmCreateArchive.setLocationRelativeTo(parent);
		frmCreateArchive.setVisible(true);
		return result;
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frmCreateArchive = new JDialog();
		frmCreateArchive.setTitle("Archive Creation");
		frmCreateArchive.setBounds(100, 100, 528, 355);
		frmCreateArchive.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		frmCreateArchive.getContentPane().setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		frmCreateArchive.setLocationRelativeTo(null);
		frmCreateArchive.setResizable(false);

		JPanel panel = new JPanel();
		panel.setPreferredSize(new Dimension(500, 315));
		frmCreateArchive.getContentPane().add(panel);
		panel.setLayout(null);

		JLabel lbl = new JLabel("Create archive details");
		lbl.setFont(new Font("Dialog", Font.BOLD, 14));
		lbl.setHorizontalAlignment(SwingConstants.CENTER);
		lbl.setBounds(12, 12, 476, 35);
		panel.add(lbl);

		JLabel lblArchiveUrl = new JLabel("Archive URL");
		lblArchiveUrl.setBounds(12, 56, 476, 17);
		panel.add(lblArchiveUrl);

		archiveUrlField = new JTextField();
		archiveUrlField.setBounds(12, 74, 476, 21);
		panel.add(archiveUrlField);

		JLabel lblName = new JLabel("Archive name");
		lblName.setBounds(12, 103, 476, 17);
		panel.add(lblName);

		archiveNameField = new JTextField();
		archiveNameField.setBounds(12, 121, 476, 21);
		panel.add(archiveNameField);

		JLabel lblCredits = new JLabel("Hosting credits (optional, place your name or organization here)");
		lblCredits.setBounds(12, 150, 476, 17);
		panel.add(lblCredits);

		creditsField = new JTextField();
		creditsField.setBounds(12, 168, 476, 21);
		panel.add(creditsField);

		JButton btnNewButton = new JButton("Create");
		archiveUrlField.addActionListener(new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				btnNewButton.doClick();
			}
		});
		archiveNameField.addActionListener(new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				btnNewButton.doClick();
			}
		});
		creditsField.addActionListener(new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				btnNewButton.doClick();
			}
		});
		btnNewButton.setBounds(346, 276, 142, 27);
		panel.add(btnNewButton);

		JButton btnNewButton_1 = new JButton("Cancel");
		btnNewButton_1.setBounds(229, 276, 105, 27);
		panel.add(btnNewButton_1);

		JCheckBox chckbxAllowArchiveDownloads = new JCheckBox("Allow archive downloads");
		chckbxAllowArchiveDownloads.setSelected(true);
		chckbxAllowArchiveDownloads.setBounds(8, 199, 480, 25);
		panel.add(chckbxAllowArchiveDownloads);

		JCheckBox chckbxAllowStreaming = new JCheckBox("Allow streaming of assets");
		chckbxAllowStreaming.setSelected(true);
		chckbxAllowStreaming.setBounds(8, 228, 480, 25);
		panel.add(chckbxAllowStreaming);

		btnNewButton_1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				frmCreateArchive.dispose();
			}
		});
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (archiveNameField.getText().isEmpty()) {
					// Error
					JOptionPane.showMessageDialog(frmCreateArchive, "Missing archive name", "Missing details",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				if (archiveUrlField.getText().isEmpty()) {
					// Error
					JOptionPane.showMessageDialog(frmCreateArchive, "Missing archive URL", "Missing details",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				if (!chckbxAllowStreaming.isSelected() && !chckbxAllowArchiveDownloads.isSelected()) {
					// Error
					JOptionPane.showMessageDialog(frmCreateArchive,
							"Please allow either streaming or download, you cannot have both disabled.",
							"Missing details", JOptionPane.ERROR_MESSAGE);
					return;
				}
				result = new CreationResult();
				result.archiveURL = archiveUrlField.getText();
				result.archiveName = archiveNameField.getText();
				result.creditsField = creditsField.getText();
				if (result.creditsField.isEmpty())
					result.creditsField = null;
				result.allowDownload = chckbxAllowArchiveDownloads.isSelected();
				result.allowStreaming = chckbxAllowStreaming.isSelected();
				frmCreateArchive.dispose();
			}
		});
	}
}
