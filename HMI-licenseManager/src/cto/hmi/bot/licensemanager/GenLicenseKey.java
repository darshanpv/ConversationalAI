package cto.hmi.bot.licensemanager;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Date;

public class GenLicenseKey {

	public boolean GenKey(String data) {

		try {
			KeyStore ks = KeyStore.getInstance("JKS");
			String keyStoreName = new File(".").getAbsolutePath()
					+ "/res/keys/license.jks";
			FileInputStream ksfis = new FileInputStream(keyStoreName);
			BufferedInputStream ksbufin = new BufferedInputStream(ksfis);

			String pass = "mozeondigital";
			char[] kpass = pass.toCharArray();
			String alias = "mozeon";
			// Import Private key from JKS
			ks.load(ksbufin, kpass);
			PrivateKey priv = (PrivateKey) ks.getKey(alias, kpass);
			System.out
					.println("LICENSE_MANAGER: Imported private key from KeyStore");
			java.security.cert.Certificate cert = ks.getCertificate(alias);
			byte[] encodedCert = cert.getEncoded();

			// Save the certificate in a file named "license.cer"
			String licenseCert = new File(".").getAbsolutePath()
					+ "/res/keys/license.cer";
			FileOutputStream certfos = new FileOutputStream(licenseCert);
			certfos.write(encodedCert);
			certfos.close();

			// Import Public key from JKS
			FileInputStream certfis = new FileInputStream(licenseCert);
			java.security.cert.CertificateFactory cf = java.security.cert.CertificateFactory
					.getInstance("X.509");
			cert = cf.generateCertificate(certfis);
			PublicKey pub = cert.getPublicKey();
			System.out
					.println("LICENSE_MANAGER: Imported public key from KeyStore");

			/* Create a Signature object and initialize it with the private key */
			Signature dsa = Signature.getInstance("SHA1withDSA", "SUN");
			dsa.initSign(priv);
			/* Update and sign the data */
			// String data = "connecticus15042018";
			byte[] buffer = data.getBytes();
			dsa.update(buffer, 0, data.length());
			/*
			 * Now that all the data to be signed has been read in, generate a
			 * signature for it
			 */
			byte[] realSig = dsa.sign();

			/* Save the signature in a file */
			String signature = new File(".").getAbsolutePath()
					+ "/res/keys/license";

			FileOutputStream sigfos = new FileOutputStream(signature + ".sig");
			sigfos.write(realSig);
			sigfos.close();
			System.out
					.println("LICENSE_MANAGER: Generated license signature file");
			/* store for record */
			signature = new File(".").getAbsolutePath() + "/res/data/license";
			String parts[] = data.split(",");
			sigfos = new FileOutputStream(signature + "_" + parts[1] + ".sig");
			sigfos.write(realSig);
			sigfos.close();
			System.out
					.println("LICENSE_MANAGER: Stored license signature file");
			/* Save the public key in a file */
			String pubKey = new File(".").getAbsolutePath()
					+ "/res/keys/license";
			byte[] key = pub.getEncoded();
			FileOutputStream keyfos = new FileOutputStream(pubKey + ".key");
			System.out.println("LICENSE_MANAGER: Generated license key file");
			keyfos.write(key);
			keyfos.close();
			/* store for record */
			pubKey = new File(".").getAbsolutePath() + "/res/data/license";
			keyfos = new FileOutputStream(pubKey + "_" + parts[1] + ".key");
			System.out.println("LICENSE_MANAGER: Stored license key file");
			keyfos.write(key);
			keyfos.close();
			/* Store data for record */
			String licData = new File(".").getAbsolutePath()
					+ "/res/data/license";
			String cData = "Created on: "
					+ new Date().toString() + "\n";
			FileOutputStream licDatafos = new FileOutputStream(licData + "_"
					+ parts[1] + ".dat");
			System.out.println("LICENSE_MANAGER: Stored license data");
			licDatafos.write(cData.getBytes());
			licDatafos.write((data.getBytes()));
			licDatafos.close();
			System.out
					.println("LICENSE_MANAGER: License key and signature stored in \\keys folder");
			return true;
		} catch (KeyStoreException | NoSuchAlgorithmException
				| CertificateException | IOException
				| UnrecoverableKeyException | NoSuchProviderException
				| InvalidKeyException | SignatureException e) {
			// TODO Auto-generated catch block
			System.out
					.println("LICENSE_MANAGER: SEVERE !! Failed to create the License key and signature file" + e );
			return false;
		}

	}

}
