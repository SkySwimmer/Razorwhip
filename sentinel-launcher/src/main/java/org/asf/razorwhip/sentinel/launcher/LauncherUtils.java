package org.asf.razorwhip.sentinel.launcher;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import org.asf.razorwhip.sentinel.launcher.api.IEmulationSoftwareProvider;
import org.asf.razorwhip.sentinel.launcher.api.IGameDescriptor;

public class LauncherUtils {

	static BackgroundPanel panel;
	static JPanel progressPanel;
	static JProgressBar progressBar;
	static JLabel statusLabel;

	static DynamicClassLoader loader = new DynamicClassLoader();

	static String gameID;
	static String softwareID;
	static String softwareVersion;
	static String softwareName;
	static IGameDescriptor gameDescriptor;
	static IEmulationSoftwareProvider emulationSoftware;

	static void addUrlToComponentClassLoader(URL url) {
		loader.addUrl(url);
	}

	/**
	 * Retrieves the active game descriptor
	 * 
	 * @return IGameDescriptor instance
	 */
	public static IGameDescriptor getGameDescriptor() {
		return gameDescriptor;
	}

	/**
	 * Retrieves the game ID
	 * 
	 * @return Game ID string
	 */
	public static String getGameID() {
		return gameID;
	}

	/**
	 * Retrieves the emulation software ID
	 * 
	 * @return Emulation software ID string
	 */
	public static String getSoftwareID() {
		return softwareID;
	}

	/**
	 * Retrieves the emulation software version
	 * 
	 * @return Emulation software version string
	 */
	public static String getSoftwareVersion() {
		return softwareVersion;
	}

	/**
	 * Retrieves the emulation software name
	 * 
	 * @return Emulation software name string
	 */
	public static String getSoftwareName() {
		return softwareName;
	}

	/**
	 * Retrieves the emulation software provider
	 * 
	 * @return IEmulationSoftwareProvider instance
	 */
	public static IEmulationSoftwareProvider getEmulationSoftware() {
		return emulationSoftware;
	}

	/**
	 * Shows the progress panel
	 */
	public static void showProgressPanel() {
		try {
			SwingUtilities.invokeAndWait(() -> {
				progressPanel.setVisible(true);
				panel.repaint();
			});
		} catch (InvocationTargetException | InterruptedException e) {
		}
	}

	/**
	 * Hides the progress panel
	 */
	public static void hideProgressPanel() {
		try {
			SwingUtilities.invokeAndWait(() -> {
				progressPanel.setVisible(false);
				panel.repaint();
			});
		} catch (InvocationTargetException | InterruptedException e) {
		}
	}

	/**
	 * Resets the progress bar
	 */
	public static void resetProgressBar() {
		try {
			SwingUtilities.invokeAndWait(() -> {
				progressBar.setMaximum(100);
				progressBar.setValue(0);
				panel.repaint();
			});
		} catch (InvocationTargetException | InterruptedException e) {
		}
	}

	/**
	 * Assigns the progress bar values
	 * 
	 * @param value Progress value
	 * @param max   Progress max
	 */
	public static void setProgress(int value, int max) {
		try {
			SwingUtilities.invokeAndWait(() -> {
				progressBar.setMaximum(max);
				progressBar.setValue(value);
				panel.repaint();
			});
		} catch (InvocationTargetException | InterruptedException e) {
		}
	}

	/**
	 * Assigns the progress bar value
	 * 
	 * @param value Progress value
	 */
	public static void setProgress(int value) {
		try {
			SwingUtilities.invokeAndWait(() -> {
				progressBar.setValue(value);
				panel.repaint();
			});
		} catch (InvocationTargetException | InterruptedException e) {
		}
	}

	/**
	 * Increases the progress bar value
	 * 
	 * @param value Progress value to increase with
	 */
	public static void increaseProgress(int value) {
		try {
			SwingUtilities.invokeAndWait(() -> {
				if (progressBar.getValue() + value > progressBar.getMaximum())
					progressBar.setValue(progressBar.getMaximum());
				else
					progressBar.setValue(progressBar.getValue() + value);
				panel.repaint();
			});
		} catch (InvocationTargetException | InterruptedException e) {
		}
	}

	/**
	 * Increases the progress bar value
	 */
	public static void increaseProgress() {
		try {
			SwingUtilities.invokeAndWait(() -> {
				if (progressBar.getValue() + 1 > progressBar.getMaximum())
					progressBar.setValue(progressBar.getMaximum());
				else
					progressBar.setValue(progressBar.getValue() + 1);
				panel.repaint();
			});
		} catch (InvocationTargetException | InterruptedException e) {
		}
	}

	/**
	 * Assigns the progress bar max value
	 * 
	 * @param max Progress max value
	 */
	public static void setProgressMax(int max) {
		try {
			SwingUtilities.invokeAndWait(() -> {
				progressBar.setMaximum(max);
				panel.repaint();
			});
		} catch (InvocationTargetException | InterruptedException e) {
		}
	}

	/**
	 * Prints a log message
	 * 
	 * @param message      Message to log
	 * @param statusUpdate True to update the label, false otherwise
	 */
	public static void log(String message, boolean statusUpdate) {
		try {
			SwingUtilities.invokeAndWait(() -> {
				if (statusUpdate)
					statusLabel.setText(" " + message);
			});
		} catch (InvocationTargetException | InterruptedException e) {
		}
		System.out.println("[LAUNCHER] [SENTINEL LAUNCHER] " + message);
	}

	/**
	 * Prints a log message
	 * 
	 * @param message Message to log
	 */
	public static void log(String message) {
		System.out.println("[LAUNCHER] [SENTINEL LAUNCHER] " + message);
	}

	/**
	 * Assigns the status message
	 * 
	 * @param message Message to use
	 */
	public static void setStatus(String message) {
		try {
			SwingUtilities.invokeAndWait(() -> {
				statusLabel.setText(" " + message);
			});
		} catch (InvocationTargetException | InterruptedException e) {
		}
	}

	/**
	 * Parses Sentinel property sets
	 * 
	 * @param props Property set to parse
	 * @return Properties map
	 */
	public static Map<String, String> parseProperties(String props) {
		HashMap<String, String> properties = new HashMap<String, String>();
		for (String line : props.replace("\r", "").split("\n")) {
			if (line.isEmpty() || line.startsWith("#") || !line.contains(": "))
				continue;
			String key = line;
			String value = "";
			if (key.contains(": ")) {
				value = key.substring(key.indexOf(": ") + 2);
				key = key.substring(0, key.indexOf(": "));
			}
			properties.put(key, value);
		}
		return properties;
	}

	/**
	 * Downloads a string
	 * 
	 * @param url URL to download from
	 * @return Downloaded string
	 * @throws IOException If downloading fails
	 */
	public static String downloadString(String url) throws IOException {
		URLConnection conn = new URL(url).openConnection();
		InputStream strm = conn.getInputStream();
		String data = new String(strm.readAllBytes(), "UTF-8");
		return data;
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

	/**
	 * Deletes a directory
	 * 
	 * @param dir Directory to download
	 */
	public static void deleteDir(File dir) {
		for (File subDir : dir.listFiles(t -> t.isDirectory())) {
			deleteDir(subDir);
		}
		for (File file : dir.listFiles(t -> !t.isDirectory())) {
			file.delete();
		}
		dir.delete();
	}

	/**
	 * Copies folders (shows progress)
	 * 
	 * @param source      Source folder
	 * @param destination Destination folder
	 * @throws IOException If copying fails
	 */
	public static void copyDirWithProgress(File source, File destination) throws IOException {
		LauncherUtils.setProgress(0, indexDir(source));
		LauncherUtils.showProgressPanel();

		destination.mkdirs();
		for (File subDir : source.listFiles(t -> t.isDirectory())) {
			copyDirWithoutProgress(subDir, new File(destination, subDir.getName()));
			LauncherUtils.increaseProgress();
		}
		for (File file : source.listFiles(t -> !t.isDirectory())) {
			Files.copy(file.toPath(), new File(destination, file.getName()).toPath(),
					StandardCopyOption.REPLACE_EXISTING);
			LauncherUtils.increaseProgress();
		}
	}

	/**
	 * Copies folders
	 * 
	 * @param source      Source folder
	 * @param destination Destination folder
	 * @throws IOException If copying fails
	 */
	public static void copyDirWithoutProgress(File source, File destination) throws IOException {
		destination.mkdirs();
		for (File subDir : source.listFiles(t -> t.isDirectory())) {
			copyDirWithoutProgress(subDir, new File(destination, subDir.getName()));
		}
		for (File file : source.listFiles(t -> !t.isDirectory())) {
			Files.copy(file.toPath(), new File(destination, file.getName()).toPath(),
					StandardCopyOption.REPLACE_EXISTING);
		}
	}

	/**
	 * Downloads a file (shows progress)
	 * 
	 * @param url  URL to the file to download
	 * @param outp Output file
	 * @throws IOException If downloading fails
	 */
	public static void downloadFile(String url, File outp) throws IOException {
		LauncherUtils.resetProgressBar();
		LauncherUtils.showProgressPanel();
		URLConnection urlConnection = new URL(url).openConnection();
		try {
			SwingUtilities.invokeAndWait(() -> {
				LauncherUtils.progressBar.setMaximum(urlConnection.getContentLength() / 1000);
				LauncherUtils.progressBar.setValue(0);
				LauncherUtils.panel.repaint();
			});
		} catch (InvocationTargetException | InterruptedException e) {
		}
		InputStream data = urlConnection.getInputStream();
		FileOutputStream out = new FileOutputStream(outp);
		while (true) {
			byte[] b = data.readNBytes(1000);
			if (b.length == 0)
				break;
			else {
				out.write(b);
				SwingUtilities.invokeLater(() -> {
					LauncherUtils.progressBar.setValue(LauncherUtils.progressBar.getValue() + 1);
					LauncherUtils.panel.repaint();
				});
			}
		}
		out.close();
		data.close();
		SwingUtilities.invokeLater(() -> {
			LauncherUtils.progressBar.setValue(LauncherUtils.progressBar.getMaximum());
			LauncherUtils.panel.repaint();
		});
	}

	/**
	 * Unzips a zip file (shows progress)
	 * 
	 * @param input  File to unzip
	 * @param output Folder to unzip into
	 * @throws IOException If unzipping fails
	 */
	public static void unZip(File input, File output) throws IOException {
		LauncherUtils.resetProgressBar();
		LauncherUtils.showProgressPanel();
		output.mkdirs();

		// count entries
		ZipFile archive = new ZipFile(input);
		int count = 0;
		Enumeration<? extends ZipEntry> en = archive.entries();
		while (en.hasMoreElements()) {
			en.nextElement();
			count++;
		}
		archive.close();

		// prepare and log
		archive = new ZipFile(input);
		en = archive.entries();
		try {
			int fcount = count;
			SwingUtilities.invokeAndWait(() -> {
				progressBar.setMaximum(fcount);
				progressBar.setValue(0);
				panel.repaint();
			});
		} catch (InvocationTargetException | InterruptedException e) {
		}

		// extract
		while (en.hasMoreElements()) {
			ZipEntry ent = en.nextElement();
			if (ent == null)
				break;

			if (ent.isDirectory()) {
				new File(output, ent.getName()).mkdirs();
			} else {
				File out = new File(output, ent.getName());
				if (out.getParentFile() != null && !out.getParentFile().exists())
					out.getParentFile().mkdirs();
				FileOutputStream os = new FileOutputStream(out);
				InputStream is = archive.getInputStream(ent);
				is.transferTo(os);
				is.close();
				os.close();
			}

			SwingUtilities.invokeLater(() -> {
				progressBar.setValue(progressBar.getValue() + 1);
				panel.repaint();
			});
		}

		// finish progress
		SwingUtilities.invokeLater(() -> {
			progressBar.setValue(progressBar.getValue() + 1);
			panel.repaint();
		});
		archive.close();
	}

	static void extractEmulationSoftware(File sourceFile, String version) throws IOException {
		// Extract
		LauncherUtils.log("Extracting software package...");
		LauncherUtils.deleteDir(new File("emulationsoftwaretmp"));
		LauncherUtils.unZip(sourceFile, new File("emulationsoftwaretmp"));

		// Update data
		LauncherUtils.log("Updating server to " + version + "...", true);
		LauncherUtils.copyDirWithProgress(new File("emulationsoftwaretmp", "baseserver"), new File("server"));
		LauncherUtils.log("Updating extra server files to " + version + "...", true);
		LauncherUtils.copyDirWithProgress(new File("emulationsoftwaretmp", "expandedserver"), new File("server"));
		LauncherUtils.log("Updating data to " + version + "...", true);
		LauncherUtils.copyDirWithProgress(new File("emulationsoftwaretmp", "rootdata"), new File("."));
		LauncherUtils.log("Updating asset modifications to " + version + "...", true);
		LauncherUtils.copyDirWithProgress(new File("emulationsoftwaretmp", "assetmodifications"),
				new File("assetmodifications"));
		for (File clientDir : new File(".").listFiles(t -> t.getName().startsWith("client-") && t.isDirectory())) {
			String clientVersion = clientDir.getName().substring("client-".length());
			LauncherUtils.log("Updating " + clientVersion + " client modifications to " + version + "...", true);
			LauncherUtils.copyDirWithProgress(new File("emulationsoftwaretmp", "clientmodifications-" + clientVersion),
					clientDir);
		}
		LauncherUtils.log("Updating default payloads to " + version + "...", true);
		LauncherUtils.copyDirWithProgress(new File("emulationsoftwaretmp", "defaultpayloads"), new File("payloads"));

		// Delete
		LauncherUtils.deleteDir(new File("emulationsoftwaretmp"));
	}

	static void extractGameDescriptor(File sourceFile, String version) throws IOException {
		// Extract
		LauncherUtils.log("Extracting game descriptor package...");
		LauncherUtils.deleteDir(new File("gamedescriptortmp"));
		LauncherUtils.unZip(sourceFile, new File("gamedescriptortmp"));

		// Update data
		LauncherUtils.log("Updating default payloads to " + version + "...", true);
		LauncherUtils.copyDirWithProgress(new File("emulationsoftwaretmp", "defaultpayloads"), new File("payloads"));
		LauncherUtils.log("Updating data to " + version + "...", true);
		LauncherUtils.copyDirWithProgress(new File("emulationsoftwaretmp", "rootdata"), new File("."));
		LauncherUtils.log("Updating asset modifications to " + version + "...", true);
		LauncherUtils.copyDirWithProgress(new File("emulationsoftwaretmp", "assetmodifications"),
				new File("assetmodifications"));

		// Delete
		LauncherUtils.deleteDir(new File("gamedescriptortmp"));
	}
}
