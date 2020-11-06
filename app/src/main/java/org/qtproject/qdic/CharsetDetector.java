package org.qtproject.qdic;

import org.mozilla.universalchardet.UniversalDetector;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

class CharsetDetector {

	Charset getCharset(File f) throws IOException {
	    byte[] buf = new byte[512];
	    FileInputStream fis = new FileInputStream(f);
	    
	    UniversalDetector detector = new UniversalDetector(null);

	    int nread;
	    while ((nread = fis.read(buf)) > 0 && !detector.isDone()) {
	      detector.handleData(buf, 0, nread);
	    }

	    detector.dataEnd();

	    String encoding = detector.getDetectedCharset();

	    detector.reset();
	    fis.close();
	    if (encoding != null) {
	    	if (encoding.equalsIgnoreCase("MACCYRILLIC")) {
	    		//System.out.println("Detected encoding = Cp1251");
	    		return Charset.forName("Cp1251");
	    	}
	    	//System.out.println("Detected encoding = " + encoding);
			return Charset.forName(encoding);
		} else {
			System.out.println("No encoding detected.");
			return StandardCharsets.UTF_8;
		}
	}

}