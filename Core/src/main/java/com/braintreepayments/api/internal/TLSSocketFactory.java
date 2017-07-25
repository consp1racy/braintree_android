package com.braintreepayments.api.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.TlsVersion;
import okhttp3.internal.platform.Platform;

public class TLSSocketFactory extends SSLSocketFactory {

    private final SSLSocketFactory mInternalSSLSocketFactory;
    private final X509TrustManager mTrustManager;

    public TLSSocketFactory() throws SSLException {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, null, null); // use system security providers
            mInternalSSLSocketFactory = sslContext.getSocketFactory();
            mTrustManager = Platform.get().trustManager(mInternalSSLSocketFactory);
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new SSLException(e.getMessage());
        }
    }

    /**
     * @see <a href="http://developer.android.com/training/articles/security-ssl.html#UnknownCa">Android
     * Documentation</a>
     */
    public TLSSocketFactory(InputStream certificateStream) throws SSLException {
        try {
            final KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);

            final CertificateFactory cf = CertificateFactory.getInstance("X.509");
            final Collection<? extends Certificate> certificates = cf.generateCertificates(certificateStream);
            for (Certificate cert : certificates) {
                if (cert instanceof X509Certificate) {
                    String subject = ((X509Certificate) cert).getSubjectDN().getName();
                    keyStore.setCertificateEntry(subject, cert);
                }
            }

            final TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(keyStore);
            final TrustManager[] trustManagers = tmf.getTrustManagers();
            if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
                throw new IllegalStateException("Unexpected default trust managers:" + Arrays.toString(trustManagers));
            }

            final SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagers, null);

            mInternalSSLSocketFactory = sslContext.getSocketFactory();
            mTrustManager = (X509TrustManager) trustManagers[0];
        } catch (Exception e) {
            throw new SSLException(e.getMessage());
        } finally {
            try {
                certificateStream.close();
            } catch (IOException | NullPointerException ignored) {
                /* No-op. */
            }
        }
    }

    public X509TrustManager getTrustManager() {
        return mTrustManager;
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return mInternalSSLSocketFactory.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return mInternalSSLSocketFactory.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket(Socket s, String host, int port, boolean autoClose)
            throws IOException {
        return enableTLSOnSocket(mInternalSSLSocketFactory.createSocket(s, host, port, autoClose));
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        return enableTLSOnSocket(mInternalSSLSocketFactory.createSocket(host, port));
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort)
            throws IOException {
        return enableTLSOnSocket(
                mInternalSSLSocketFactory.createSocket(host, port, localHost, localPort));
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        return enableTLSOnSocket(mInternalSSLSocketFactory.createSocket(host, port));
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress,
            int localPort) throws IOException {
        return enableTLSOnSocket(
                mInternalSSLSocketFactory.createSocket(address, port, localAddress, localPort));
    }

    private Socket enableTLSOnSocket(Socket socket) {
        if (socket instanceof SSLSocket) {
            ArrayList<String> supportedProtocols =
                    new ArrayList<>(Arrays.asList(((SSLSocket) socket).getSupportedProtocols()));
            supportedProtocols.retainAll(Collections.singletonList(TlsVersion.TLS_1_2.javaName()));

            ((SSLSocket) socket).setEnabledProtocols(supportedProtocols.toArray(new String[supportedProtocols.size()]));
        }

        return socket;
    }
}
