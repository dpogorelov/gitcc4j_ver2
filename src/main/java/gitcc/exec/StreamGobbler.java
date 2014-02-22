package gitcc.exec;

import org.apache.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by dpogorel on 21.02.14.
 */
public class StreamGobbler extends Thread {
    private static Logger log = Logger.getLogger(StreamGobbler.class);
	private InputStream is;
	private byte[] result;
	
    StreamGobbler(InputStream is) {
        this.is = is;
    }

    public void run() {
    	ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
            byte[] b = new byte[1024];
            for (int i; (i = is.read(b)) != -1;) {
                out.write(b, 0, i);
            }
            result = out.toByteArray();
            out.close();
        } catch(IOException e) {
        	log.error(e);
		}
    }

	public byte[] getResult() {
		return result;
	}

	
}
