package dev.leonlatsch.kolibri.boot;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.regex.Pattern;

import dev.leonlatsch.kolibri.R;
import dev.leonlatsch.kolibri.constants.Regex;
import dev.leonlatsch.kolibri.constants.Responses;
import dev.leonlatsch.kolibri.constants.Values;
import dev.leonlatsch.kolibri.rest.dto.Container;
import dev.leonlatsch.kolibri.rest.service.CommonService;
import dev.leonlatsch.kolibri.rest.service.ConfigService;
import dev.leonlatsch.kolibri.rest.service.RestServiceFactory;
import dev.leonlatsch.kolibri.settings.Config;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * A {@link AlertDialog} to configure the backend
 *
 * @author Leon Latsch
 * @since 1.0.0
 */
public class BackendDialog extends AlertDialog {

    private static final String SLASH = "/";
    private static final String HTTPS = "https://";
    private static final String HTTP = "http://";

    private TextView connectButton;
    private EditText hostnameEditText;
    private ProgressBar progressBar;
    private ImageView successImageView;

    private CommonService commonService;
    private ConfigService configService;

    public BackendDialog(Context context) {
        super(context);
        View view = getLayoutInflater().inflate(R.layout.dialog_backend, null);
        connectButton = view.findViewById(R.id.dialog_backend_button);
        hostnameEditText = view.findViewById(R.id.dialog_backend_edit_text);
        progressBar = view.findViewById(R.id.dialog_backend_progressbar);
        successImageView = view.findViewById(R.id.dialog_backend_success_indicator);

        connectButton.setOnClickListener(view1 -> connect());
        hostnameEditText.setOnEditorActionListener((textView, i, keyEvent) -> {
            connect();
            return true;
        });

        setCancelable(false);
        setView(view);
    }

    private void connect() {
        isLoading(true);
        String url = buildUrl(hostnameEditText.getText().toString());
        if (url == null) {
            isLoading(false);
            success(false);
            return;
        }

        tryHealthcheck(url);
    }

    private void tryHealthcheck(final String url) {
        RestServiceFactory.initialize(url);
        commonService = RestServiceFactory.getCommonService();
        configService = RestServiceFactory.getConfigService();
        commonService.healthcheck().enqueue(new Callback<Container<Void>>() {
            @Override
            public void onResponse(Call<Container<Void>> call, Response<Container<Void>> response) {
                if (response.isSuccessful() && Responses.MSG_OK.equals(response.body().getMessage())) {
                    saveConfig(url);
                } else {
                    isLoading(false);
                    success(false);
                }
            }

            @Override
            public void onFailure(Call<Container<Void>> call, Throwable t) {
                isLoading(false);
                success(false);
            }
        });
    }

    private boolean checkVersion(String backendVersion) {
        try {
            PackageInfo packageInfo = getContext().getPackageManager().getPackageInfo(getContext().getPackageName(), 0);
            String[] arr = packageInfo.versionName.split("\\.");
            String majorVersion = packageInfo.versionName.split("\\.")[0]; // Get the major version eg. 1 from 1.5.3
            return backendVersion.startsWith(majorVersion);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /**
     * Build a url with the default protocol
     *
     * @param input
     * @return
     */
    private String buildUrl(String input) {
        if (!Pattern.matches(Regex.URL, input)) {
            input = HTTPS + input;
            if (!Pattern.matches(Regex.URL, input)) {
                return null;
            }
        }

        if (!input.endsWith(SLASH)) {
            input += SLASH;
        }

        return input;
    }

    private void success(boolean success) {
        successImageView.setVisibility(View.VISIBLE);
        if (success) {
            successImageView.setImageDrawable(getContext().getResources().getDrawable(R.drawable.icons8_checked_48, getContext().getTheme()));
            connectButton.setText(R.string.CONNECTED);
            connectButton.setOnClickListener(null);
        } else {
            successImageView.setImageDrawable(getContext().getResources().getDrawable(R.drawable.icons8_cancel_48, getContext().getTheme()));
            connectButton.setText(R.string.CONNECT);
            connectButton.setOnClickListener(view -> connect());
        }
    }

    private void isLoading(boolean isLoading) {
        if (isLoading) {
            hostnameEditText.setEnabled(false);
            connectButton.setClickable(false);
            progressBar.setVisibility(View.VISIBLE);
            successImageView.setVisibility(View.GONE);
        } else {
            hostnameEditText.setEnabled(true);
            connectButton.setClickable(true);
            progressBar.setVisibility(View.GONE);
        }
    }

    /**
     * Saves the config after a healtheck succeeded
     *
     * @param url
     */
    private void saveConfig(String url) {
        SharedPreferences preferences = Config.getSharedPreferences(getContext());
        SharedPreferences.Editor editor = preferences.edit();

        editor.putString(Config.KEY_BACKEND_HTTP_BASEURL, url);
        editor.putString(Config.KEY_BACKEND_BROKER_HOST, extractHostname(url));

        configService.getBrokerPort().enqueue(new Callback<Integer>() { // Get the port for the broker
            @Override
            public void onResponse(Call<Integer> call, Response<Integer> response) {
                if (response.isSuccessful()) {
                    int port = response.body();
                    editor.putInt(Config.KEY_BACKEND_BROKER_PORT, port);
                    commonService.getVersion().enqueue(new Callback<String>() {
                        @Override
                        public void onResponse(Call<String> call, Response<String> response) {
                            if (response.isSuccessful() && checkVersion(response.body())) {
                                editor.apply();
                                isLoading(false);
                                success(true);
                                new Handler().postDelayed(() -> dismiss(), 1000); // Wait 1 sec before dismissing
                            } else {
                                isLoading(false);
                                success(false);
                            }
                        }

                        @Override
                        public void onFailure(Call<String> call, Throwable t) {
                            isLoading(false);
                            success(false);
                        }
                    });
                }
            }

            @Override
            public void onFailure(Call<Integer> call, Throwable t) {
                isLoading(false);
                success(false); // Very unlikely
            }
        });
    }

    /**
     * Extract the hostname for the broker
     *
     * @param url
     * @return
     */
    private String extractHostname(String url) {
        if (Pattern.matches(Regex.URL, url)) {
            String brokerHost = null;

            if (url.startsWith(HTTPS)) { // Remove http protocol
                brokerHost = url.replace(HTTPS, Values.EMPTY);
            } else if (url.startsWith(HTTP)) {
                brokerHost = url.replace(HTTP, Values.EMPTY);
            }

            if (brokerHost.endsWith(SLASH)) { // Remove trailing slashes
                brokerHost = brokerHost.replace(SLASH, Values.EMPTY);
            }

            return brokerHost;
        } else {
            return null;
        }
    }
}
