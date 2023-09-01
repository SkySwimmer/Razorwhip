package org.asf.razorwhip.sentinel.launcher.windows;

import java.awt.BorderLayout;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import javax.swing.text.DefaultCaret;
import javax.swing.JTextArea;
import java.awt.FlowLayout;
import javax.swing.border.BevelBorder;

public class ArchiveDefSnippetViewer {
	public JDialog frm;

	private JTextArea textArea = new JTextArea();
	private JLabel lastMessage = new JLabel(
			"Here is your archive definition snippet for adding your archive to the public archive list");

	private JPanel contentPane;

	public void show(JDialog parent, String data) {
		initialize(parent);
		textArea.setText(data);
		frm.setVisible(true);
		frm.setModal(true);
	}

	/**
	 * Create the frame.
	 */
	public ArchiveDefSnippetViewer() {
		initialize(null);
	}

	public void initialize(JDialog parent) {
		if (parent != null)
			frm = new JDialog(parent);
		else
			frm = new JDialog();
		frm.setTitle("Archive configuration");
		frm.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		frm.setBounds(100, 100, 743, 433);
		frm.setResizable(false);
		frm.setLocationRelativeTo(parent);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		frm.setContentPane(contentPane);

		textArea.setEditable(false);
		JScrollPane pane = new JScrollPane(textArea);
		pane.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		pane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

		DefaultCaret caret = (DefaultCaret) textArea.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

		contentPane.add(pane, BorderLayout.CENTER);

		JPanel panel = new JPanel();
		FlowLayout flowLayout = (FlowLayout) panel.getLayout();
		flowLayout.setAlignment(FlowLayout.LEFT);
		contentPane.add(panel, BorderLayout.NORTH);

		panel.add(lastMessage);
	}
}
