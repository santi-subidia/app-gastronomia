package com.example.app_movil_gastronomia.di;

import com.example.app_movil_gastronomia.BuildConfig;
import com.example.app_movil_gastronomia.core.AuthInterceptor;
import com.example.app_movil_gastronomia.core.SessionManager;
import com.example.app_movil_gastronomia.core.TokenManager;
import com.example.app_movil_gastronomia.data.api.AuthApi;
import com.example.app_movil_gastronomia.data.api.CajaApi;
import com.example.app_movil_gastronomia.data.api.ConfiguracionApi;
import com.example.app_movil_gastronomia.data.api.DemoraApi;
import com.example.app_movil_gastronomia.data.api.EstadosPedidoApi;
import com.example.app_movil_gastronomia.data.api.MetodoPagoApi;
import com.example.app_movil_gastronomia.data.api.MetodoVentaApi;
import com.example.app_movil_gastronomia.data.api.PedidoApi;
import com.example.app_movil_gastronomia.data.api.ProductoApi;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

@Module
@InstallIn(SingletonComponent.class)
public class NetworkModule {

    @Provides
    @Singleton
    public Gson provideGson() {
        return new GsonBuilder().create();
    }

    @Provides
    @Singleton
    public HttpLoggingInterceptor provideLoggingInterceptor() {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        if (BuildConfig.DEBUG) {
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        } else {
            interceptor.setLevel(HttpLoggingInterceptor.Level.NONE);
        }
        return interceptor;
    }

    @Provides
    @Singleton
    public SessionManager provideSessionManager() {
        return new SessionManager();
    }

    @Provides
    @Singleton
    public OkHttpClient provideOkHttpClient(
            TokenManager tokenManager,
            SessionManager sessionManager,
            HttpLoggingInterceptor loggingInterceptor
    ) {
        return new OkHttpClient.Builder()
                .addInterceptor(new AuthInterceptor(tokenManager, sessionManager))
                .addInterceptor(loggingInterceptor)
                .build();
    }

    @Provides
    @Singleton
    public Retrofit provideRetrofit(OkHttpClient client, Gson gson) {
        return new Retrofit.Builder()
                .baseUrl(BuildConfig.API_BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
    }

    @Provides
    @Singleton
    public AuthApi provideAuthApi(Retrofit retrofit) {
        return retrofit.create(AuthApi.class);
    }

    @Provides
    @Singleton
    public ProductoApi provideProductoApi(Retrofit retrofit) {
        return retrofit.create(ProductoApi.class);
    }

    @Provides
    @Singleton
    public ConfiguracionApi provideConfiguracionApi(Retrofit retrofit) {
        return retrofit.create(ConfiguracionApi.class);
    }

    @Provides
    @Singleton
    public DemoraApi provideDemoraApi(Retrofit retrofit) {
        return retrofit.create(DemoraApi.class);
    }

    @Provides
    @Singleton
    public PedidoApi providePedidoApi(Retrofit retrofit) {
        return retrofit.create(PedidoApi.class);
    }

    @Provides
    @Singleton
    public CajaApi provideCajaApi(Retrofit retrofit) {
        return retrofit.create(CajaApi.class);
    }

    @Provides
    @Singleton
    public EstadosPedidoApi provideEstadosPedidoApi(Retrofit retrofit) {
        return retrofit.create(EstadosPedidoApi.class);
    }

    @Provides
    @Singleton
    public MetodoPagoApi provideMetodoPagoApi(Retrofit retrofit) {
        return retrofit.create(MetodoPagoApi.class);
    }

    @Provides
    @Singleton
    public MetodoVentaApi provideMetodoVentaApi(Retrofit retrofit) {
        return retrofit.create(MetodoVentaApi.class);
    }
}
