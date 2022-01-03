package cto.hmi.processor.exceptions;

public class LicenseVersionExpiredException extends LicenseException {

    private static final long serialVersionUID = 8947235554238066208L;

    public LicenseVersionExpiredException() {
        super("version expired");
    }

}
