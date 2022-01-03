package cto.hmi.processor.licensemanager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

public class VerLicenseKey {
	
	public Boolean Verify(String data) {
		boolean verifies = false;
		try {
			String licenseSig = new File(".").getAbsolutePath()
					+ "/res/keys/license.sig";
			String licenseKey = new File(".").getAbsolutePath()
					+ "/res/keys/license.key";
			/* import encoded public key */

			FileInputStream keyfis;

			keyfis = new FileInputStream(licenseKey);
			byte[] encKey = new byte[keyfis.available()];
			keyfis.read(encKey);
			keyfis.close();

			X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(encKey);

			KeyFactory keyFactory = KeyFactory.getInstance("DSA", "SUN");
			PublicKey pubKey = keyFactory.generatePublic(pubKeySpec);

			/* input the signature bytes */
			FileInputStream sigfis = new FileInputStream(licenseSig);
			byte[] sigToVerify = new byte[sigfis.available()];
			sigfis.read(sigToVerify);
			sigfis.close();

			/* create a Signature object and initialize it with the public key */
			Signature sig = Signature.getInstance("SHA1withDSA", "SUN");
			sig.initVerify(pubKey);

			/* Update and verify the data */
			// String data = "connecticus15042018";
			byte[] buffer = data.getBytes();

			sig.update(buffer, 0, data.length());
			verifies = sig.verify(sigToVerify);

		} catch (IOException | NoSuchAlgorithmException
				| NoSuchProviderException | InvalidKeySpecException
				| InvalidKeyException | SignatureException e) {
			// TODO Auto-generated catch block
			System.err.println("Caught exception " + e.toString());
		}
		return verifies;

	}
}
