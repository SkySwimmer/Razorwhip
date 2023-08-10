package testdescriptor;

import org.asf.razorwhip.sentinel.launcher.api.IGameDescriptor;

public class SentinelTestGameDescriptor implements IGameDescriptor {

	@Override
	public void init() {
		getClass();
	}

}
