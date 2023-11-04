package org.asf.sentinel.launcher;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;
import java.nio.ByteBuffer;
import java.security.CodeSource;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.HashMap;

public class DynamicClassLoader extends URLClassLoader {

	private HashMap<String, Class<?>> loadedClasses = new HashMap<String, Class<?>>();

	/**
	 * Create a new instance of the dynamic URL class loader
	 */
	public DynamicClassLoader() {
		super(new URL[0], null);
	}

	/**
	 * Create a new instance of the dynamic URL class loader
	 * 
	 * @param urls URLs to initialize with
	 */
	public DynamicClassLoader(URL[] urls) {
		super(urls, null);
	}

	/**
	 * Create a new instance of the dynamic URL class loader
	 * 
	 * @param name Class loader name
	 */
	public DynamicClassLoader(String name) {
		super(name, new URL[0], null);
	}

	/**
	 * Create a new instance of the dynamic URL class loader
	 * 
	 * @param urls URLs to initialize with
	 * @param name Class loader name
	 */
	public DynamicClassLoader(String name, URL[] urls) {
		super(name, urls, null);
	}

	/**
	 * Create a new instance of the dynamic URL class loader
	 * 
	 * @param parent Parent class loader
	 * @param name   Class loader name
	 */
	public DynamicClassLoader(String name, ClassLoader parent) {
		super(name, new URL[0], parent);
	}

	/**
	 * Create a new instance of the dynamic URL class loader
	 * 
	 * @param parent Parent class loader
	 */
	public DynamicClassLoader(ClassLoader parent) {
		super(new URL[0], parent);
	}

	/**
	 * Create a new instance of the dynamic URL class loader
	 * 
	 * @param parent Parent class loader
	 * @param urls   URLs to initialize with
	 */
	public DynamicClassLoader(URL[] urls, ClassLoader parent) {
		super(new URL[0], parent);
	}

	/**
	 * Create a new instance of the dynamic URL class loader
	 * 
	 * @param parent  Parent class loader
	 * @param urls    URLs to initialize with
	 * @param factory The URLStreamHandlerFactory to use
	 */
	public DynamicClassLoader(URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
		super(urls, parent, factory);
	}

	/**
	 * Create a new instance of the dynamic URL class loader
	 * 
	 * @param parent Parent class loader
	 * @param urls   URLs to initialize with
	 * @param name   Class loader name
	 */
	public DynamicClassLoader(String name, URL[] urls, ClassLoader parent) {
		super(name, urls, parent);
	}

	/**
	 * Create a new instance of the dynamic URL class loader
	 * 
	 * @param parent  Parent class loader
	 * @param urls    URLs to initialize with
	 * @param name    Class loader name
	 * @param factory The URLStreamHandlerFactory to use
	 */
	public DynamicClassLoader(String name, URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
		super(name, urls, parent, factory);
	}

	/**
	 * Add URL to the class loader
	 * 
	 * @param url URL to add
	 */
	public void addUrl(URL url) {
		super.addURL(url);
	}

	/**
	 * Add URLs to the class loader
	 * 
	 * @param urls URLs to add
	 */
	public void addUrls(URL[] urls) {
		for (URL url : urls) {
			super.addURL(url);
		}
	}

	/**
	 * Add URLs to the class loader
	 * 
	 * @param urls URLs to add
	 */
	public void addUrls(Iterable<URL> urls) {
		for (URL url : urls) {
			super.addURL(url);
		}
	}

	public Class<?> getLoadedClass(String name) {
		return loadedClasses.get(name);
	}

	@Override
	public Class<?> findClass(String name) throws ClassNotFoundException {
		for (LoadedClassProvider prov : loadedClassProviders) {
			Class<?> cls = prov.provide(name);
			if (cls != null)
				return cls;
		}
		Class<?> _class = null;
		try {
			if (loadedClasses.containsKey(name)) {
				return loadedClasses.get(name);
			} else
				_class = super.findClass(name);
		} catch (ClassNotFoundException ex) {
			ClassLoader l = getParent();
			if (l == null)
				l = Thread.currentThread().getContextClassLoader();
			if (l == this)
				l = ClassLoader.getSystemClassLoader();
			_class = Class.forName(name, true, l);
		}
		if (!loadedClasses.containsKey(name)) {
			loadedClasses.put(name, _class);
		}
		return _class;
	}

	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		return loadClass(name, true);
	}

	@Override
	public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		for (LoadedClassProvider prov : loadedClassProviders) {
			Class<?> cls = prov.provide(name);
			if (cls != null)
				return cls;
		}
		if (loadedClasses.containsKey(name))
			return loadedClasses.get(name);

		Class<?> cl = null;
		try {
			cl = doLoadClass(name, resolve);
		} catch (ClassNotFoundException | NoClassDefFoundError ex) {
			ClassLoader l = getParent();
			if (l == null)
				l = Thread.currentThread().getContextClassLoader();
			if (l == this)
				l = ClassLoader.getSystemClassLoader();
			cl = l.loadClass(name);
		}

		if (!loadedClasses.containsKey(name)) {
			loadedClasses.put(name, cl);
		}
		return cl;
	}

	Class<?> doLoadClass(String name, boolean resolve) throws ClassNotFoundException {
		String path = name.replaceAll("\\.", "/") + ".class";
		for (URL u : getURLs()) {
			try {
				if (!u.toString().endsWith(".class")) {
					if (!u.toString().endsWith("/")) {
						try {
							u = new URL("jar:" + u.toString() + "!/" + path);
						} catch (MalformedURLException e) {
						}
					} else {
						try {
							u = new URL(u + "/" + path);
						} catch (MalformedURLException e) {
						}
					}
				} else {
					if (!u.toString().endsWith("/" + path)) {
						continue;
					}
				}
				if (name.startsWith("java."))
					return ClassLoader.getSystemClassLoader().loadClass(name);

				BufferedInputStream strm = new BufferedInputStream(u.openStream());
				byte[] data = strm.readAllBytes();
				strm.close();

				Class<?> cls = defineClass(name, ByteBuffer.wrap(data), new CodeSource(u, (Certificate[]) null));
				if (resolve)
					this.resolveClass(cls);
				return cls;
			} catch (IOException ex) {
			}
		}
		throw new ClassNotFoundException("Cannot find class " + name);
	}

	private URL getResourceURL(String name) throws MalformedURLException {
		StackTraceElement[] elements = Thread.currentThread().getStackTrace();
		Class<?> caller = null;
		int index = 2;
		while (index < elements.length) {
			try {
				caller = loadClass(elements[index++].getClassName());
				while (caller.getTypeName().equals(Class.class.getTypeName())
						|| caller.getTypeName().equals(DynamicClassLoader.class.getTypeName())) {
					caller = loadClass(elements[index++].getClassName());
				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			ClassLoader cl = caller.getClassLoader();
			if (cl == this)
				cl = Thread.currentThread().getContextClassLoader();
			if (cl == this)
				cl = ClassLoader.getSystemClassLoader();

			try {
				URL cSource = caller.getProtectionDomain().getCodeSource().getLocation();
				String prefix = cSource.toString();
				if (cSource.getProtocol().equals("jar")) {
					prefix = prefix.substring(0, prefix.lastIndexOf("!"));
					prefix = prefix + "!/";
				} else if (cSource.toString().endsWith("jar")) {
					prefix = "jar:" + prefix + "!/";
				} else if (!prefix.endsWith("/"))
					prefix += "/";
				prefix += name;
				return new URL(prefix);
			} catch (NullPointerException e) {
			}
		}
		return null;
	}

	@Override
	public URL getResource(String name) {
		try {
			URL resource = getResourceURL(name);
			if (resource == null) {
				return null;
			}
			try {
				resource.openStream().close();
			} catch (IOException ex) {
				return null;
			}
			return resource;
		} catch (MalformedURLException e) {
			return null;
		}
	}

	@Override
	public InputStream getResourceAsStream(String name) {
		try {
			URL resource = getResourceURL(name);
			if (resource == null) {
				return null;
			}
			return resource.openStream();
		} catch (IOException e) {
			return null;
		}
	}

	public void addDefaultCp() {
		for (String entry : System.getProperty("java.class.path").split(File.pathSeparator)) {
			try {
				addUrl(new File(entry).toURI().toURL());
			} catch (MalformedURLException e) {
				try {
					addUrl(new URL(entry));
				} catch (MalformedURLException e2) {
				}
			}
		}
	}

	public static interface LoadedClassProvider {
		public String name();

		public Class<?> provide(String name);
	}

	public static ArrayList<LoadedClassProvider> loadedClassProviders = new ArrayList<LoadedClassProvider>();

	public static boolean knowsLoadedClassProvider(String name) {
		for (LoadedClassProvider prov : loadedClassProviders) {
			if (prov.name().equals(name))
				return true;
		}
		return false;
	}

	public static void registerLoadedClassProvider(LoadedClassProvider loadedClassProvider) {
		if (!knowsLoadedClassProvider(loadedClassProvider.name()))
			loadedClassProviders.add(loadedClassProvider);
	}

}