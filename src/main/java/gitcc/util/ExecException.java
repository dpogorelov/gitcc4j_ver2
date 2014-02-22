package gitcc.util;

public class ExecException extends RuntimeException {

	public ExecException(String message) {
		super("ExecException: " + message);
	}

}
