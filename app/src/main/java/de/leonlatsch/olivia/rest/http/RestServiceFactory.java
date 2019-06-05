package de.leonlatsch.olivia.rest.http;

import java.io.IOException;

import de.leonlatsch.olivia.rest.repository.UserRestRepository;
import de.leonlatsch.olivia.rest.service.UserRestService;
import okhttp3.Credentials;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

public class RestServiceFactory {

    private static final String BASE_URL = "https://olivia.leonlatsch.de:7443/";

    // TODO: create a server side stored keypair and REMOVE this afterwords
    private static final String API_TOKEN = "bfdc99b120cd49e0e1a18dc8267afa3e";
    private static final String API_KEY = "6eb77586c6fbfd1280412db3bf0e103f";

	//TODO: create a service with a repository that cas just be used without creating a thread o.ä.
    public static UserRestService createUserService() {
        OkHttpClient client = OliviaHttpClient.getOliviaHttpClient();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(JacksonConverterFactory.create())
                .build();

        UserRestRepository repository =  retrofit.create(UserRestRepository.class);

        return new UserRestService(repository);
    }


}
