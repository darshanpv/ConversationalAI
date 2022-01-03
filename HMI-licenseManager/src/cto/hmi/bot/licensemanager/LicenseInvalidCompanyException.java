package cto.hmi.bot.licensemanager;

public class LicenseInvalidCompanyException extends LicenseException {

    private static final long serialVersionUID = -9069804052012922999L;

    /**
     * Constructs a new exception with null as its detail message. The cause is
     * not initialized, and may subsequently be initialized by a call to
     * Throwable.initCause(java.lang.Throwable).
     */
    public LicenseInvalidCompanyException() {
        super("license not issued to company");
    }

}
