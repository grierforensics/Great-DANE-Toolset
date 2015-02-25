package com.grierforensics.danesmimeatoolset;

import com.sun.mail.pop3.POP3Store;
import org.bouncycastle.asn1.nist.NISTObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v1CertificateBuilder;
import org.bouncycastle.cert.dane.DANECertificateFetcher;
import org.bouncycastle.cert.dane.DANEException;
import org.bouncycastle.cert.dane.fetcher.JndiDANEFetcherFactory;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509v1CertificateBuilder;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.jcajce.*;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.mail.smime.SMIMESigned;
import org.bouncycastle.mail.smime.SMIMEToolkit;
import org.bouncycastle.openssl.jcajce.JcaPKIXIdentityBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DigestCalculator;
import org.bouncycastle.operator.DigestCalculatorProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.pkix.jcajce.JcaPKIXIdentity;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.*;
import java.util.*;

/**
 * This is sample workflow code that doesn't currently work when run, because the sample email addresses don't have DNS setup (i think)
 * This was copied from Jonathan Grier and refactored to it's current state.
 */
public class DaneSmimeaTool {

    public static void main(String[] args)
            throws Exception {
        Security.addProvider(new BouncyCastleProvider());

        String fromFullName = "Sample Person";
        String fromEmailAddress = "person@sample.org";
        String toEmailAddress = "user@example.com";

        sendEncryptedEmail(fromFullName, fromEmailAddress, toEmailAddress);
        readMailServer(fromFullName, fromEmailAddress, toEmailAddress);
    }

    private static void readMailServer(String fromFullName, String fromEmailAddress, String toEmailAddress) throws Exception {
        DigestCalculatorProvider digestCalculatorProvider = new JcaDigestCalculatorProviderBuilder().setProvider("BC").build();
        SMIMEToolkit toolkit = new SMIMEToolkit(digestCalculatorProvider);
        X509Certificate toUserCert = getCertForEmailAddress(digestCalculatorProvider, toEmailAddress);
        JcaPKIXIdentity myIdentity = loadIdentity(fromFullName, fromEmailAddress);

        readMailServer(new MessageHandler(toolkit, myIdentity, toUserCert));
    }

    private static void readMailServer(MessageHandler messageHandler) throws Exception {
        Properties properties = new Properties();
        properties.put("mail.pop3.host", "mail.sample.org");
        Session emailSession = Session.getDefaultInstance(properties);
        POP3Store emailStore = (POP3Store) emailSession.getStore("pop3");
        String emailUserName = "person";
        String emailPassword = "personPassword";
        emailStore.connect(emailUserName, emailPassword);

        // create the folder object and open it
        Folder emailFolder = emailStore.getFolder("INBOX");
        emailFolder.open(Folder.READ_ONLY);

        // search for the message we want in the inbox.
        Message[] messages = emailFolder.getMessages();
        for (int i = 0; i < messages.length; i++) {
            Message message = messages[i];
            messageHandler.handleMessage(i, message);
        }

        // close the store and folder objects
        emailFolder.close(false);
        emailStore.close();
    }


    private static class MessageHandler {

        private final SMIMEToolkit toolkit;
        private final JcaPKIXIdentity myIdentity;
        private final X509Certificate toUserCert;

        public MessageHandler(SMIMEToolkit toolkit, JcaPKIXIdentity myIdentity, X509Certificate toUserCert) {
            this.toolkit = toolkit;
            this.myIdentity = myIdentity;
            this.toUserCert = toUserCert;
        }

        public void handleMessage(int i, Message message) throws Exception {
            InternetAddress fromAddress = (InternetAddress) message.getFrom()[0];

            if (fromAddress.getAddress().equalsIgnoreCase("user@example.com")) {
                // If it is encrypted, decrypt it using our cert and key.
                // If it is signed, then:
                // a. Extract the signer's cert
                // b. Set a boolean if the signature is valid (that is, the signature was signed by the included cert and matches the message), regardless of if the cert is trusted
                // c. Set a boolean if the signer's cert is trusted via DANE; that is, it is one of the certs retrieved via DANE above
                // d. Set a boolean if the signer's cert is trusted via a CA; that is, signed by a (trusted) CA chain
                MessageDetails details = getMessageDetails(toolkit, message, myIdentity, toUserCert);

                System.out.println("---------------------------------");
                System.out.println("Email Number " + (i + 1));
                System.out.println("Valid Signature  : " + details.isSignatureValid);
                System.out.println("Is DANE certified: " + details.isDANESigned);
                System.out.println("Is CA certified  : " + details.isCASigned);
            }
        }
    }

    private static void sendEncryptedEmail(String fromFullName, String fromEmailAddress, String toEmailAddress) throws Exception {
        DigestCalculatorProvider digestCalculatorProvider = new JcaDigestCalculatorProviderBuilder().setProvider("BC").build();
        SMIMEToolkit toolkit = new SMIMEToolkit(digestCalculatorProvider);
        JcaPKIXIdentity myIdentity = loadIdentity(fromFullName, fromEmailAddress);
        X509Certificate toUserCert = getCertForEmailAddress(digestCalculatorProvider, toEmailAddress);


        Properties props = System.getProperties();
        Session session = Session.getDefaultInstance(props, null);

        Address fromUser = new InternetAddress("\"" + fromFullName + "\"<" + fromEmailAddress + ">");
        Address toUser = new InternetAddress(toEmailAddress);

        MimeBodyPart msg = new MimeBodyPart();
        msg.setText("Hello world!");

        MimeMultipart signedMultipart = toolkit.sign(msg, new JcaSimpleSignerInfoGeneratorBuilder().setProvider("BC").build("SHA1withRSA", myIdentity.getPrivateKey(), myIdentity.getX509Certificate()));
        MimeBodyPart encrypted = toolkit.encrypt(signedMultipart, new JceCMSContentEncryptorBuilder(NISTObjectIdentifiers.id_aes128_CBC).setProvider("BC").build(), new JceKeyTransRecipientInfoGenerator(toUserCert).setProvider("BC"));

        MimeMessage body = new MimeMessage(session);
        body.setFrom(fromUser);
        body.setRecipient(Message.RecipientType.TO, toUser);
        body.setSubject("example message");
        body.setContent(encrypted, encrypted.getContentType());
        body.saveChanges();

        Transport.send(body);      // note: an Authenticator may need to added to our session object to identify us to the mail server.

        //notes:
        // Using our cert and private key, the above cert(s), and JavaMail, send a signed, encrypted email to user@example.com
        // (user@example.com is using standard S/MIME from Outlook or Thunderbird)
       /* Similar to:
        $ openssl smime -sign -in message -out signed-message -signer /path/to/your/certificate.pem -inkey  /path/to/your/secret-key.pem -text
		$ openssl smime -encrypt -out encrypted-signed-message -in signed-message /path/to/intended-operators/certificate.pem
	    */
    }

    private static JcaPKIXIdentity loadIdentity(String fullName, String emailAddress) throws Exception {
        // Load our self-signed cert and private key that were previously created via openssl
        JcaPKIXIdentity myIdentity = loadOpenSSLIdentity("privKey.pem", "cert.pem");
        // Alternatively, generate a self-signed cert and private key equivalent to the above, save them to a file, and load them from that file
        if (myIdentity == null) {
            myIdentity = generateIdentity(fullName, emailAddress);
        }
        // Retrieve via DANE the cert(s) listed for user@example.com
        return myIdentity;
    }

    private static X509Certificate getCertForEmailAddress(DigestCalculatorProvider digestCalculatorProvider, String toEmailAddress) throws OperatorCreationException, DANEException, CertificateException {
        DigestCalculator sha224Calculator = digestCalculatorProvider.get(new AlgorithmIdentifier(NISTObjectIdentifiers.id_sha224));

        JndiDANEFetcherFactory fetcher = new JndiDANEFetcherFactory().usingDNSServer("8.8.8.8");
        DANECertificateFetcher certFetcher = new DANECertificateFetcher(fetcher, sha224Calculator);

        X509CertificateHolder userCertHolder = (X509CertificateHolder) certFetcher.fetch(toEmailAddress).get(0);

        JcaX509CertificateConverter certConverter = new JcaX509CertificateConverter().setProvider("BC");

        return certConverter.getCertificate(userCertHolder);
    }


    // If it is encrypted, decrypt it using our cert and key.
    // If it is signed, then:
    // a. Extract the signer's cert
    // b. Set a boolean if the signature is valid (that is, the signature was signed by the included cert and matches the message), regardless of if the cert is trusted
    // c. Set a boolean if the signer's cert is trusted via DANE; that is, it is one of the certs retrieved via DANE above
    // d. Set a boolean if the signer's cert is trusted via a CA; that is, signed by a (trusted) CA chain
    private static MessageDetails getMessageDetails(SMIMEToolkit toolkit, Message message, JcaPKIXIdentity myIdentity, X509Certificate theirCertificate)
            throws Exception {
        Object content;

        if (toolkit.isEncrypted(message)) {
            /* Similar to:
             $ openssl smime -decrypt -in encrypted-signed-message -out received-msg -recip /path/to/operators/certificate.pem -inkey /path/to/operators/private-key.pem
	         */
            content = toolkit.decrypt((MimeBodyPart) message.getContent(), myIdentity.getRecipientId(), new JceKeyTransEnvelopedRecipient(myIdentity.getPrivateKey()).setProvider("BC"));

        } else {
            content = message.getContent();
        }

        if (content instanceof MimeMultipart) {
            MimeMultipart mm = (MimeMultipart) content;
            if (toolkit.isSigned(mm)) {
                SMIMESigned smimeSigned = new SMIMESigned(mm);

                SignerInformation signerInformation = smimeSigned.getSignerInfos().getSigners().iterator().next();

                 /* Similar to:
                  $ openssl smime -pk7out -in message-signed.txt | openssl pkcs7 -text -noout -print_certs
			      and then separating the cert from the CA chain which may be included
		         */
                X509CertificateHolder certHolder = toolkit.extractCertificate(mm, signerInformation);

                 /* Similar to:
			        $ openssl cms -verify -in smime.msg -certfile cert.pem -no_signer_cert_verify
		         */
                boolean isSignatureValid = toolkit.isValidSignature(mm, new JcaSimpleSignerInfoVerifierBuilder().setProvider("BC").build(certHolder));

                if (certHolder.equals(new JcaX509CertificateHolder(theirCertificate))) {
                    // DANE verified
                    return new MessageDetails(isSignatureValid, true, false);
                } else {
                    // We'd need to load the CA chain here and verify the CertPath. If it checks out we can set true
                    boolean certPathValid = validateCertPath(certHolder);

                    return new MessageDetails(isSignatureValid, false, certPathValid);
                }
            }
        } else {
            Part bodyPart = (Part) content;
            if (toolkit.isSigned(bodyPart)) {
                SMIMESigned smimeSigned = new SMIMESigned(bodyPart);

                SignerInformation signerInformation = smimeSigned.getSignerInfos().getSigners().iterator().next();

                 /* Similar to:
                  $ openssl smime -pk7out -in message-signed.txt | openssl pkcs7 -text -noout -print_certs
			      and then separating the cert from the CA chain which may be included
		         */
                X509CertificateHolder certHolder = toolkit.extractCertificate(bodyPart, signerInformation);

                 /* Similar to:
			        $ openssl cms -verify -in smime.msg -certfile cert.pem -no_signer_cert_verify
		         */
                boolean isSignatureValid = toolkit.isValidSignature(bodyPart, new JcaSimpleSignerInfoVerifierBuilder().setProvider("BC").build(certHolder));

                if (certHolder.equals(new JcaX509CertificateHolder(theirCertificate))) {
                    // DANE verified
                    return new MessageDetails(isSignatureValid, true, false);
                } else {
                    // We'd need to load the CA chain here and verify the CertPath. If it checks out we can set true
                    boolean certPathValid = validateCertPath(certHolder);

                    return new MessageDetails(isSignatureValid, false, certPathValid);
                }
            }
        }

        return new MessageDetails(false, false, false);
    }

    // Example of CertificatePath validation around a CA's certificate chain.
    private static boolean validateCertPath(X509CertificateHolder certHolder)
            throws GeneralSecurityException {
        JcaX509CertificateConverter converter = new JcaX509CertificateConverter().setProvider("BC");

        X509Certificate rootCert = null; // TODO: set the trust anchor for the chain.

        List list = new ArrayList();

        list.add(converter.getCertificate(certHolder));

        // TODO: add other certificates and possible CRLs to the CertStore.

        CollectionCertStoreParameters ccsp = new CollectionCertStoreParameters(list);
        CertStore store = CertStore.getInstance("Collection", ccsp, "BC");

        //Searching for rootCert by subjectDN without CRL
        Set trust = new HashSet();
        trust.add(new TrustAnchor(rootCert, null));

        try {
            CertPathBuilder cpb = CertPathBuilder.getInstance("PKIX", "BC");
            X509CertSelector targetConstraints = new X509CertSelector();
            targetConstraints.setSubject(certHolder.getSubject().getEncoded());
            PKIXBuilderParameters params = new PKIXBuilderParameters(trust, targetConstraints);

            params.addCertStore(store);

            PKIXCertPathBuilderResult result = (PKIXCertPathBuilderResult) cpb.build(params);

            return true;
        } catch (CertPathBuilderException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    private static JcaPKIXIdentity loadOpenSSLIdentity(String keyFileName, String certFileName)
            throws IOException, CertificateException {
        File keyFile = new File(keyFileName);

        if (!keyFile.canRead()) {
            return null;
        }

        return new JcaPKIXIdentityBuilder().setProvider("BC").build(keyFile, new File(certFileName));
    }

    private static JcaPKIXIdentity generateIdentity(String fullName, String emailAddress)
            throws Exception {
       /*
		The openssl commands used are:
		# 1.	Create a new RSA private key of 2048 bits:
		$ openssl genrsa -out certkey.pem 2048
		# 2.	Create the private key and corresponding public key in DER (binary) form:
		# $ openssl rsa -in certkey.pem -outform der -out certkey.der
		# $ openssl rsa -in certkey.pem -pubout -outform der -out certpub.der
		# 3.	Create a self-signed certificate using that key. The user name here is "Sample Person" and her e-mail address is "sample@example.org":
		$ openssl req -subj "/CN=Sample Person/emailAddress=sample@example.org" -new -key certkey.pem -x509 -days 3660 -outform der -out cert.der
		*/
        KeyPairGenerator kpGen = KeyPairGenerator.getInstance("RSA", "BC");

        kpGen.initialize(2048, new SecureRandom());

        // after this the keys are created. getEncoded() on the rsaPair.getPrivate() and rsaPair.getPublic() will
        // return the DER encoding of the key.
        KeyPair rsaPair = kpGen.generateKeyPair();

        // specify the name to go in the subject/issuer for the certificate
        X500NameBuilder x500Bldr = new X500NameBuilder();

        x500Bldr.addRDN(BCStyle.CN, fullName);
        x500Bldr.addRDN(BCStyle.EmailAddress, emailAddress);

        X500Name id = x500Bldr.build();

        // set the start and expiry dates for the certificate
        Date notBefore = new Date(System.currentTimeMillis() - (5 * 60 * 1000));
        Date notAfter = new Date(notBefore.getTime() + 3660L * 24 * 60 * 60 * 1000);

        // build the certificate
        X509v1CertificateBuilder certBldr = new JcaX509v1CertificateBuilder(
                id,
                BigInteger.valueOf(System.currentTimeMillis()),
                notBefore,
                notAfter,
                id,
                rsaPair.getPublic());

        ContentSigner signer = new JcaContentSignerBuilder("SHA1withRSA").setProvider("BC").build(rsaPair.getPrivate());
        JcaX509CertificateConverter certConverter = new JcaX509CertificateConverter().setProvider("BC");

        // create self-signed certificate
        X509Certificate rsaCert = certConverter.getCertificate(certBldr.build(signer));

        return new JcaPKIXIdentity(rsaPair.getPrivate(), new X509Certificate[]{rsaCert});
    }

    private static class MessageDetails {

        private final boolean isSignatureValid;
        private final boolean isDANESigned;
        private final boolean isCASigned;

        public MessageDetails(boolean isSignatureValid, boolean isDANESigned, boolean isCASigned) {
            this.isSignatureValid = isSignatureValid;
            this.isDANESigned = isDANESigned;
            this.isCASigned = isCASigned;
        }
    }
}
	



