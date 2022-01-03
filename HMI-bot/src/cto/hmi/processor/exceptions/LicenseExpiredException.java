package cto.hmi.processor.exceptions;

public class LicenseExpiredException extends LicenseException {

    private static final long serialVersionUID = -9069804052012922999L;

    public LicenseExpiredException() {
        super("license expired");
    }

}
