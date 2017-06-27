package com.ilusons.harmony.ref.m3u;

/**
 * This class provides some useful static methods.
 * 
 * @author Ke
 */
public class M3UToolSet {
	/**
	 * Load a m3u file. The file size is larger, the time spends longer.
	 * 
	 * @param filename
	 *            the m3u filename.
	 * @return the instance of M3UFile.
	 */
	public static M3UFile load(String filename) {
		final M3UFile file = new M3UFile();
		final M3UHandler handler = new M3UHandler() {

			@Override
			public void onReadEXTM3U(M3UHead header) {
				file.setHeader(header);
			}

			@Override
			public void onReadEXTINF(M3UItem item) {
				file.addItem(item);
			}
		};
		M3UParser.getInstance().parse(filename, handler);
		return file;
	}

	public static M3UFile create() {
		final M3UFile file = new M3UFile();
		return file;
	}
}
