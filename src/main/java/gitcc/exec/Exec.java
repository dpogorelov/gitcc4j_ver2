package gitcc.exec;

import gitcc.util.ExecException;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class Exec {
	private static Logger log = Logger.getLogger(Exec.class);
	
	private final String[] cmd;
	private File root;

	public Exec(String... cmd) {
		this.cmd = cmd;
	}

	public void setRoot(File root) {
		this.root = root;
	}

	public byte[] _exec(boolean isControlErrors, String... args) {
		return _exec(isControlErrors, null, args);
	}

	public byte[] _exec(String... args) {
		return _exec(true, null, args);
	}

	private byte[] _exec(boolean isControlErrors, String[] env, String... args) {
		String[] _args = new String[args.length + cmd.length];
		System.arraycopy(args, 0, _args, cmd.length, args.length);
		for (int i = 0; i < cmd.length; i++) {
			_args[i] = cmd[i];
		}
		debug(_args);
		try {
			Process process = Runtime.getRuntime().exec(_args, env, root);
            try {
            	StreamGobbler errorGobbler = new StreamGobbler(process.getErrorStream());
                StreamGobbler outputGobbler = new StreamGobbler(process.getInputStream());
                
                errorGobbler.start();
                outputGobbler.start();
                
            	int exitVal = process.waitFor();
                
                byte[] stdout = outputGobbler.getResult();
                if( isControlErrors ) {
                	String error = bytesToString(errorGobbler.getResult());
                    if (exitVal > 0 && error.length() > 0) {
                        throw new ExecException(error + bytesToString(stdout));
                    }
                }
                return stdout;
            } finally {
                process.destroy();
            }
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void debug(String[] _args) {
		StringBuilder b = new StringBuilder();
		for (String arg : _args) {
			b.append(arg).append(" ");
		}
		log.debug(b.toString());
	}

	public String exec(boolean isControlErrors, String[] env, String... args) {
		return bytesToString(_exec(isControlErrors, env, args));
	}
	
	public String exec(String[] env, String... args) {
		return bytesToString(_exec(true, env, args));
	}
	
	public String exec(String... args) {
		return exec(null, args);
	}
	
	public String exec(boolean isControlErrors, String... args) {
		return exec(isControlErrors, null, args);
	}
	
	public String exec(boolean isControlErrors, List<String> args) {
    	String[] _args = args.toArray(new String[args.size()]);
    	return exec(isControlErrors, _args);
    }
	
	public String exec(List<String> args) {
    	return exec(true, args);
    }

	public File getRoot() {
		return root;
	}

	private String bytesToString(byte[] stdout) {
		if( stdout != null ) {
			return new String(stdout).trim();
		}
		return "";
	}

}
