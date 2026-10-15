package com.smartsenergy.microgrid.api;

import com.smartsenergy.microgrid.utils.SessionManager;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class ApiClient {
    private static ApiService service;
    private static String currentBaseUrl;

    private ApiClient() {}

    public static synchronized void reset() {
        service = null;
        currentBaseUrl = null;
    }

    public static synchronized ApiService get() {
        String baseUrl = SessionManager.getInstance().getBaseUrl();
        if (service == null || !baseUrl.equals(currentBaseUrl)) {
            currentBaseUrl = baseUrl;

            HttpLoggingInterceptor log = new HttpLoggingInterceptor();
            log.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(20, TimeUnit.SECONDS)
                    .writeTimeout(20, TimeUnit.SECONDS)
                    .addInterceptor(chain -> {
                        Request original = chain.request();
                        Request.Builder b = original.newBuilder();
                        String token = SessionManager.getInstance().token();
                        if (token != null && !token.isEmpty()) {
                            b.header("Authorization", "Bearer " + token);
                        }
                        return chain.proceed(b.build());
                    })
                    .addInterceptor(log);

            // Configure SSL trust manager to handle both HTTP and HTTPS (development self-signed certs)
            try {
                TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                        @Override
                        public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                        @Override
                        public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[]{}; }
                    }
                };

                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, trustAllCerts, new SecureRandom());
                SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

                clientBuilder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
                clientBuilder.hostnameVerifier((hostname, session) -> true);
            } catch (Exception ignored) {}

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(currentBaseUrl)
                    .client(clientBuilder.build())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            service = retrofit.create(ApiService.class);
        }
        return service;
    }
}
