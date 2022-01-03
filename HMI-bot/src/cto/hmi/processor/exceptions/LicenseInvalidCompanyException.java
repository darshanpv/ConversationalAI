package cto.hmi.processor.exceptions;

public class LicenseInvalidCompanyException extends LicenseException {

    private static final long serialVersionUID = -9069804052012922999L;

    public LicenseInvalidCompanyException() {
        super("license not issued to company");
    }

}
