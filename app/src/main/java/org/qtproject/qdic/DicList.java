package org.qtproject.qdic;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

public class DicList {

	public static String dicListError = "dicListError";
	public static String dicError = "dicError";
	
	private final HashMap<String, Boolean> map = new HashMap<>();
	private final ArrayList<String> list = new ArrayList<>();
	private int currentItem = 0;
	private final static String dicRE;

	static {
		dicRE = "^.+\\.[Dd][Ii][Cc]$";
	}

	private final static String rexRE;

	static {
		rexRE = "^.+\\.[Rr][Ee][Xx]$";
	}

	private final static String iniRE;

	static {
		iniRE = "^.+\\.[Ii][Nn][Ii]$";
	}

	public static boolean isDic(File f) {
		String name = f.getName();
		if (name.matches(dicRE)) {
			return true;
		}
		if (name.matches(rexRE)) {
			return true;
		}
		return name.matches(iniRE);
	}	
	
	public DicList(String path) throws IllegalArgumentException {
		File f = new File(path);
		if (f.exists() && f.isFile()) {
	        CharsetDetector cd = new CharsetDetector();
	        Charset	charset;
			String name = f.getName();
			if (name.matches(dicRE)) {
				list.add(path);
				map.put(path, false);
			}
			if (name.matches(rexRE)) {
				list.add(path);
				map.put(path, true);
			}
			if (name.matches(iniRE)) {
				String temp;
				try {
					charset = cd.getCharset(f);
				BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path), charset));
				while((temp = reader.readLine()) != null){
					String newDicPath = f.getParent() + File.separator + temp;
					if (!new File(newDicPath).exists()) {
						reader.close();
						throw new IOException();
					}
					list.add(newDicPath);
					map.put(newDicPath, temp.matches(rexRE));
				}
				reader.close();
				} catch (IOException e) {
					e.printStackTrace();
					throw new IllegalArgumentException(dicListError);
				}
			}
		} else {
			throw new IllegalArgumentException(dicError);
		}
	}
	
	public String getNextDic() {
		if (currentItem < list.size()) {
			String s = list.get(currentItem);
			currentItem++;
			return s;
		} else {
			return null;
		}
	}
	
	public boolean isRexType(String path) {
		final AtomicReference<Boolean> aBoolean = new AtomicReference<>(map.get(path));
		return aBoolean.get();
	}

	
	public int getSize() {
		return list.size();
	}
}
