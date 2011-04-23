package sk.fiit.rabbit.adaptiveproxy.plugins.services.logging.backends;

public class LoggingBackendFailure extends RuntimeException {

	private static final long serialVersionUID = -7420103237385517150L;

	public LoggingBackendFailure() {
		super();
	}

	public LoggingBackendFailure(String message, Throwable cause) {
		super(message, cause);
	}

	public LoggingBackendFailure(String message) {
		super(message);
	}

	public LoggingBackendFailure(Throwable cause) {
		super(cause);
	}

}
