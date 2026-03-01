package sudark2.Sudark.poisonTone.api;

import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class HttpRequest {

    private final OkHttpClient client;
    private final OkHttpClient streamClient;
    private final String baseUrl;

    public HttpRequest(String baseUrl, String apiKey) {
        this.baseUrl = baseUrl;
        this.client = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request req = chain.request().newBuilder()
                            .header("Authorization", "Bearer " + apiKey)
                            .build();
                    return chain.proceed(req);
                })
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .callTimeout(60, TimeUnit.SECONDS)
                .build();
        this.streamClient = client.newBuilder()
                .readTimeout(120, TimeUnit.SECONDS)
                .callTimeout(0, TimeUnit.SECONDS)
                .build();
    }

    public String post(String endpoint, String jsonBody) throws IOException {
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(jsonBody, JSON);

        Request request = new Request.Builder()
                .url(baseUrl + endpoint)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code: " + response);
            }
            return response.body().string();
        }
    }

    public Response postStream(String endpoint, String jsonBody) throws IOException {
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(jsonBody, JSON);
        Request request = new Request.Builder()
                .url(baseUrl + endpoint)
                .post(body)
                .build();
        Response response = streamClient.newCall(request).execute();
        if (!response.isSuccessful()) {
            response.close();
            throw new IOException("Unexpected code: " + response);
        }
        return response;
    }
}
