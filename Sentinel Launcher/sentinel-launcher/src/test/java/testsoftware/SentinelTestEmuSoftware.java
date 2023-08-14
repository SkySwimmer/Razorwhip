package testsoftware;

import javax.swing.JOptionPane;

import org.asf.razorwhip.sentinel.launcher.api.IEmulationSoftwareProvider;

public class SentinelTestEmuSoftware implements IEmulationSoftwareProvider {

	@Override
	public void init() {
		JOptionPane.showMessageDialog(null, "Test version 1");
	}

}
