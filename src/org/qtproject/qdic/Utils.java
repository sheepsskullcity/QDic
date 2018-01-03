package org.qtproject.qdic;

import java.text.DecimalFormat;

public class Utils {
	
	public static String getStringSizeLengthFile(long size) {
		DecimalFormat df = new DecimalFormat("0.00");
		final float sizeKb = 1024.0f;
		final float sizeMb = sizeKb * sizeKb;
		final float sizeGb = sizeMb * sizeKb;
		final float sizeTerra = sizeGb * sizeKb;

		if (size < sizeKb)
			return size + " b";
		else if (size < sizeMb)
			return df.format(size / sizeKb)+ " Kb";
		else if (size < sizeGb)
			return df.format(size / sizeMb) + " Mb";
		else if (size < sizeTerra)
			return df.format(size / sizeGb) + " Gb";

		return "";
	}
	
	public static boolean cantainsIllegalChars(String str) {
		if (str.matches("^.*[*\\\\/\":?|<>].*$")) {
			return true;
		}
		return false;
	}
	
	public static String getTime(long total) {
		  long x = total / 1000;
		  if (x > 3600)
			  return " " + x / 3600 + "h. " + (x % 3600) / 60 + "m. " + (x % 3600) % 60  + "s.";
		  else if (x > 60)
			  return " " + x / 60 + "m. " + x % 60  + "s.";
		  else
			  return " " + x + "s.";
	  }
}
