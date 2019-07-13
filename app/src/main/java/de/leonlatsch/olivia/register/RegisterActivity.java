package de.leonlatsch.olivia.register;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.util.regex.Pattern;

import de.leonlatsch.olivia.R;
import de.leonlatsch.olivia.main.MainActivity;
import de.leonlatsch.olivia.constants.JsonRespose;
import de.leonlatsch.olivia.constants.Regex;
import de.leonlatsch.olivia.constants.Values;
import de.leonlatsch.olivia.dto.StringDTO;
import de.leonlatsch.olivia.dto.UserDTO;
import de.leonlatsch.olivia.database.interfaces.UserInterface;
import de.leonlatsch.olivia.rest.service.RestServiceFactory;
import de.leonlatsch.olivia.rest.service.UserService;
import de.leonlatsch.olivia.security.Hash;
import de.leonlatsch.olivia.util.AndroidUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private View progressOverlay;
    private EditText usernameEditText;
    private EditText emailEditText;
    private EditText passwordEditText;
    private EditText passwordConfirmEditText;
    private Button registerBtn;

    private UserService userService;
    private UserInterface userInterface;

    private boolean usernameValid;
    private boolean emailValid;
    private boolean passwordValid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        progressOverlay = findViewById(R.id.progressOverlay);
        usernameEditText = findViewById(R.id.registerUsernameEditText);
        emailEditText = findViewById(R.id.registerEmailEditText);
        passwordEditText = findViewById(R.id.registerPasswordEditText);
        passwordConfirmEditText = findViewById(R.id.registerPasswordConfirmEditText);
        registerBtn = findViewById(R.id.registerNowBtn);

        userService = RestServiceFactory.getUserService();
        userInterface = UserInterface.getInstance();

        registerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                register();
            }
        });

        usernameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                validateUsername();
            }
        });

        emailEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                validateEmail();
            }
        });

        TextWatcher passwordTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                validatePassword();
            }
        };

        passwordEditText.addTextChangedListener(passwordTextWatcher);
        passwordConfirmEditText.addTextChangedListener(passwordTextWatcher);

        loadCachedData();
        usernameEditText.requestFocus();
    }

    private void validatePassword() {
        final String password = passwordEditText.getText().toString();
        final String passwordConfirm = passwordConfirmEditText.getText().toString();

        if (password.isEmpty() || !Pattern.matches(Regex.PASSWORD, password)) {
            showStatusIcon(passwordEditText, R.drawable.icons8_cancel_48);
            passwordValid = false;
            return;
        } else {
            showStatusIcon(passwordEditText, R.drawable.icons8_checked_48);
        }

        if (password.equals(passwordConfirm)) {
            showStatusIcon(passwordConfirmEditText, R.drawable.icons8_checked_48);
            passwordValid = true;
        } else {
            showStatusIcon(passwordConfirmEditText, R.drawable.icons8_cancel_48);
            passwordValid = false;
        }
    }

    private void validateEmail() {
        final String email = emailEditText.getText().toString();
        if (email.isEmpty() || !Pattern.matches(Regex.EMAIL, email)) {
            showStatusIcon(emailEditText, R.drawable.icons8_cancel_48);
            emailValid = false;
            return;
        }

        Call<StringDTO> call = userService.checkEmail(email);
        call.enqueue(new Callback<StringDTO>() {
            @Override
            public void onResponse(Call<StringDTO> call, Response<StringDTO> response) {
                if (response.isSuccessful()) {
                    String message = response.body().getMessage();
                    if (JsonRespose.FREE.equals(message)) {
                        showStatusIcon(emailEditText, R.drawable.icons8_checked_48);
                        emailValid = true;
                    } else {
                        showStatusIcon(emailEditText, R.drawable.icons8_cancel_48);
                        emailValid = false;
                    }
                }
            }

            @Override
            public void onFailure(Call<StringDTO> call, Throwable t) {
                showDialog(getString(R.string.error), getString(R.string.error));
            }
        });
    }

    private void validateUsername() {
        final String username = usernameEditText.getText().toString();
        if (username.isEmpty() || username.length() < 3) {
            showStatusIcon(usernameEditText, R.drawable.icons8_cancel_48);
            usernameValid = false;
            return;
        }

        Call<StringDTO> call = userService.checkUsername(username);
        call.enqueue(new Callback<StringDTO>() {
            @Override
            public void onResponse(Call<StringDTO> call, Response<StringDTO> response) {
                if (response.isSuccessful()) {
                    String message = response.body().getMessage();
                    if (JsonRespose.FREE.equals(message)) {
                        showStatusIcon(usernameEditText, R.drawable.icons8_checked_48);
                        usernameValid = true;
                    } else {
                        usernameValid = false;
                        showStatusIcon(usernameEditText, R.drawable.icons8_cancel_48);
                    }
                }
            }

            @Override
            public void onFailure(Call<StringDTO> call, Throwable t) {
                showDialog(getString(R.string.error), getString(R.string.error));
            }
        });
    }

    private void register() {
        isLoading(true);

        validateUsername();
        validateEmail();
        validatePassword();

        if (!usernameValid || !emailValid || !passwordValid) {
            isLoading(false);
            return;
        }
        final UserDTO userDTO = new UserDTO();
        userDTO.setEmail(emailEditText.getText().toString());
        userDTO.setUsername(usernameEditText.getText().toString());
        userDTO.setPassword(Hash.createHexHash(passwordEditText.getText().toString()));

        Call<StringDTO> call = userService.create(userDTO);
        call.enqueue(new Callback<StringDTO>() {
            @Override
            public void onResponse(Call<StringDTO> call, Response<StringDTO> response) {
                isLoading(false);
                if (response.isSuccessful() && JsonRespose.OK.equals(response.body().getMessage())) {
                    saveUserAndStartMain(userDTO.getEmail());
                } else {
                    showDialog(getString(R.string.error), getString(R.string.error));
                }
            }

            @Override
            public void onFailure(Call<StringDTO> call, Throwable t) {
                isLoading(false);
                showDialog("Error", getString(R.string.error_no_internet));
            }
        });
    }

    private void saveUserAndStartMain(final String email) {
        Call<UserDTO> call = userService.getByEmail(email);
        call.enqueue(new Callback<UserDTO>() {
            @Override
            public void onResponse(Call<UserDTO> call, Response<UserDTO> response) {
                if (response.isSuccessful()) {
                    userInterface.saveUser(response.body());
                    isLoading(false);
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    startActivity(intent);
                    finish();
                }
            }

            @Override
            public void onFailure(Call<UserDTO> call, Throwable t) {
                showDialog(getString(R.string.error), getString(R.string.error_no_internet));
            }
        });
    }

    private void showStatusIcon(EditText editText, int drawable) {
        editText.setCompoundDrawablesWithIntrinsicBounds(0, 0, drawable,0 );
    }

    private void loadCachedData() {
        if (getIntent().getExtras() != null) {
            String cachedEmail = (String) getIntent().getExtras().get(Values.INTENT_EMAIL);

            if (cachedEmail != null) {
                emailEditText.setText(cachedEmail);
            }
        }
    }

    private void isLoading(boolean loading) {
        if (loading) {
            AndroidUtils.animateView(progressOverlay, View.VISIBLE, 0.4f);
        } else {
            AndroidUtils.animateView(progressOverlay, View.GONE, 0.4f);
        }
    }

    private void showDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }
}
