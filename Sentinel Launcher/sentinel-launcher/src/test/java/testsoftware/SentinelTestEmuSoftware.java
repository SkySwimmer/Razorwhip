package testsoftware;

import javax.swing.JOptionPane;

import org.asf.razorwhip.sentinel.launcher.api.IEmulationSoftwareProvider;

public class SentinelTestEmuSoftware implements IEmulationSoftwareProvider {

	@Override
	public void init() {
		getClass();
	}

	@Override
	public void showOptionWindow() {
		JOptionPane.showMessageDialog(null, "Test");
	}

}
