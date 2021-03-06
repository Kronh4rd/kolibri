package dev.leonlatsch.kolibri.ui.profile;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.theartofdev.edmodo.cropper.CropImage;

import java.util.regex.Pattern;

import dev.leonlatsch.kolibri.R;
import dev.leonlatsch.kolibri.constants.Regex;
import dev.leonlatsch.kolibri.constants.Responses;
import dev.leonlatsch.kolibri.constants.Values;
import dev.leonlatsch.kolibri.database.DatabaseMapper;
import dev.leonlatsch.kolibri.database.EntityChangedListener;
import dev.leonlatsch.kolibri.database.interfaces.UserInterface;
import dev.leonlatsch.kolibri.database.model.User;
import dev.leonlatsch.kolibri.ui.MainActivity;
import dev.leonlatsch.kolibri.ui.ProfilePicActivity;
import dev.leonlatsch.kolibri.rest.dto.Container;
import dev.leonlatsch.kolibri.rest.dto.UserDTO;
import dev.leonlatsch.kolibri.rest.service.AuthService;
import dev.leonlatsch.kolibri.rest.service.RestServiceFactory;
import dev.leonlatsch.kolibri.rest.service.UserService;
import dev.leonlatsch.kolibri.security.Hash;
import dev.leonlatsch.kolibri.util.AndroidUtils;
import dev.leonlatsch.kolibri.util.ImageUtil;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Fragment for displaying and changing the logged in user
 *
 * @author Leon Latsch
 * @since 1.0.0
 */
public class ProfileFragment extends Fragment implements EntityChangedListener<User> {

    private boolean isReloadMode = false;
    private boolean profilePicChanged = false;
    private boolean passwordChanged = false;
    private String passwordCache;

    private DatabaseMapper databaseMapper = DatabaseMapper.getInstance();

    private UserInterface userInterface;
    private UserService userService;
    private AuthService authService;

    private MainActivity parent;
    private ImageView profilePicImageView;
    private EditText usernameEditText;
    private EditText emailEditText;
    private TextView status_message;

    private boolean emailValid;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        parent = (MainActivity) getActivity();
        profilePicImageView = view.findViewById(R.id.profile_profile_pic_card).findViewById(R.id.profile_profile_pic);
        usernameEditText = view.findViewById(R.id.profile_username_editText);
        emailEditText = view.findViewById(R.id.profile_email_editText);
        EditText passwordEditText = view.findViewById(R.id.profile_password_editText);
        FloatingActionButton changeProfilePicFab = view.findViewById(R.id.profile_profile_pic_change);
        Button saveBtn = view.findViewById(R.id.profile_saveBtn);
        TextView deleteAccount = view.findViewById(R.id.profile_deleteBtn);
        status_message = view.findViewById(R.id.profile_status_message);

        changeProfilePicFab.setOnClickListener(v -> changeProfilePic());

        saveBtn.setOnClickListener(v -> saveBtn());

        deleteAccount.setOnClickListener(v -> deleteAccount());

        passwordEditText.setOnClickListener(v -> changePassword());

        profilePicImageView.setOnClickListener(v -> showProfilePic());

        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                dataChanged();
            }
        };

        emailEditText.addTextChangedListener(textWatcher);


        userInterface = UserInterface.getInstance();
        userInterface.addEntityChangedListener(this);

        userService = RestServiceFactory.getUserService();
        authService = RestServiceFactory.getAuthService();

        mapUserToView(userInterface.getUser());
        displayStatusMessage(Values.EMPTY);

        return view;
    }

    private void validate() {
        validateEmail();
    }

    /**
     * Validate the email address
     */
    private void validateEmail() {
        final String email = emailEditText.getText().toString();
        if (email.isEmpty() || !Pattern.matches(Regex.EMAIL, email)) {
            showStatusIcon(emailEditText, R.drawable.icons8_cancel_48);
            emailValid = false;
            isLoading(false);
            return;
        }

        Call<Container<String>> usernameCall = userService.checkEmail(userInterface.getAccessToken(), email);
        usernameCall.enqueue(new Callback<Container<String>>() {
            @Override
            public void onResponse(Call<Container<String>> call, Response<Container<String>> response) {
                if (response.isSuccessful()) {
                    if (Responses.MSG_FREE.equals(response.body().getMessage())
                            || Responses.MSG_TAKEN_BY_YOU.equals(response.body().getMessage())) {
                        emailValid = true;
                        showStatusIcon(emailEditText, 0);
                        save();
                    } else {
                        showStatusIcon(emailEditText, R.drawable.icons8_cancel_48);
                        isLoading(false);
                    }
                }
            }

            @Override
            public void onFailure(Call<Container<String>> call, Throwable t) {
                parent.showDialog(getString(R.string.error), getString(R.string.error));
                isLoading(false);
            }
        });
    }

    private void dataChanged() {
        if (!isReloadMode) {
            displayStatusMessage(getString(R.string.unsaved_data));
        }
    }

    /**
     * Called when the user clicks the profile pic.
     */
    private void showProfilePic() {
        Intent intent = new Intent(parent.getApplicationContext(), ProfilePicActivity.class);
        intent.putExtra(Values.INTENT_KEY_PROFILE_PIC_UID, userInterface.getUser().getUid());
        intent.putExtra(Values.INTENT_KEY_PROFILE_PIC_USERNAME, userInterface.getUser().getUsername());
        startActivity(intent);
    }

    private void displayStatusMessage(String message) {
        status_message.setText(message);
    }

    /**
     * Called when the user clicks the password EditText.
     * Show a standalone dialog that handles the password changing.
     */
    private void changePassword() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.AlertDialogCustom);
        builder.setTitle(getString(R.string.password));

        final View view = getLayoutInflater().inflate(R.layout.dialog_password, null);
        builder.setView(view);

        builder.setPositiveButton(getString(R.string.ok), (dialog, which) -> {
            // Just initialize this button
        });
        builder.setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.cancel());

        final AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            final EditText oldPasswordEditText = view.findViewById(R.id.password_old_password_EditText);
            final EditText newPasswordEditText = view.findViewById(R.id.password_new_password_EditText);
            final EditText confirmPasswordEditText = view.findViewById(R.id.password_confirm_password_EditText);

            UserDTO userDTO = new UserDTO();
            userDTO.setEmail(userInterface.getUser().getEmail());
            userDTO.setPassword(Hash.createHexHash(oldPasswordEditText.getText().toString()));
            Call<Container<String>> call = authService.login(userDTO);
            call.enqueue(new Callback<Container<String>>() {
                @Override
                public void onResponse(Call<Container<String>> call, Response<Container<String>> response) {
                    if (response.isSuccessful()) {
                        if (Responses.MSG_AUTHORIZED.equals(response.body().getMessage())) {
                            showStatusIcon(oldPasswordEditText, R.drawable.icons8_checked_48);
                            String password = newPasswordEditText.getText().toString();
                            String passwordConfirm = confirmPasswordEditText.getText().toString();

                            if (!password.isEmpty() && Pattern.matches(Regex.PASSWORD, password)) {
                                showStatusIcon(newPasswordEditText, R.drawable.icons8_checked_48);
                                if (password.equals(passwordConfirm)) {
                                    showStatusIcon(confirmPasswordEditText, R.drawable.icons8_checked_48);
                                    passwordCache = password;
                                    saveBtn();
                                    passwordChanged = true;
                                    dialog.dismiss();
                                } else {
                                    showStatusIcon(confirmPasswordEditText, R.drawable.icons8_cancel_48);
                                }
                            } else {
                                showStatusIcon(newPasswordEditText, R.drawable.icons8_cancel_48);
                            }
                        } else {
                            showStatusIcon(oldPasswordEditText, R.drawable.icons8_cancel_48);
                        }
                    } else {
                        showStatusIcon(oldPasswordEditText, R.drawable.icons8_cancel_48);
                    }
                }

                @Override
                public void onFailure(Call<Container<String>> call, Throwable t) {
                    parent.showDialog(getString(R.string.error), getString(R.string.error_no_internet));
                }
            });
        });
    }

    /**
     * Called when the user clicks the delete account button.
     * Deletes the logged in user in the backend and logs out.
     */
    private void deleteAccount() {
        DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    isLoading(true);
                    Call<Container<String>> call = userService.delete(userInterface.getAccessToken());
                    call.enqueue(new Callback<Container<String>>() {
                        @Override
                        public void onResponse(Call<Container<String>> call, Response<Container<String>> response) {
                            if (response.isSuccessful()) {
                                if (Responses.MSG_OK.equals(response.body().getMessage())) {
                                    parent.logout(false);
                                } else {
                                    parent.showDialog(getString(R.string.error), getString(R.string.error_common));
                                }
                            }
                            isLoading(false);
                        }

                        @Override
                        public void onFailure(Call<Container<String>> call, Throwable t) {
                            parent.showDialog(getString(R.string.error), getString(R.string.error_no_internet));
                            isLoading(false);
                        }
                    });
                    break;
                case DialogInterface.BUTTON_NEGATIVE:
                    dialog.dismiss();
                    break;
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(parent, R.style.AlertDialogCustom);
        builder.setMessage(getString(R.string.are_you_sure_delete)).setPositiveButton(getString(R.string.yes), dialogClickListener)
                .setNegativeButton(getString(R.string.no), dialogClickListener).show();
    }

    /**
     * Called when the save button is pressed.
     */
    private void saveBtn() {
        isLoading(true);
        validate();
    }

    /**
     * Validate all changed data, save it in the backend and notify other components.
     */
    private void save() {
        final User user = mapViewToUser();
        UserDTO dto = databaseMapper.toDto(user);

        if (profilePicChanged) {
            dto.setProfilePic(extractBase64());
        }

        // delete local password
        user.setPassword(null);
        passwordCache = null;

        Call<Container<String>> call = userService.update(userInterface.getAccessToken(), dto);
        call.enqueue(new Callback<Container<String>>() {
            @Override
            public void onResponse(Call<Container<String>> call, Response<Container<String>> response) {
                System.out.println(response.code());
                if (response.isSuccessful()) {
                    if (Responses.MSG_OK.equals(response.body().getMessage())) {
                        saveNewUserAndAccessToken(response.body().getContent());
                        displayToast(R.string.account_saved);
                        displayStatusMessage(Values.EMPTY);
                    }
                } else {
                    parent.showDialog(getString(R.string.error), getString(R.string.error_common));
                    dataChanged();
                }
                isLoading(false);
            }

            @Override
            public void onFailure(Call<Container<String>> call, Throwable t) {
                parent.showDialog(getString(R.string.error), getString(R.string.error_no_internet));
                dataChanged();
                isLoading(false);
            }
        });
    }

    /**
     * Save the new user and the new access token.
     *
     * @param newAccessToken
     */
    private void saveNewUserAndAccessToken(final String newAccessToken) {
        Call<Container<UserDTO>> call;
        String accessToken;
        if (newAccessToken == null) {
            call = userService.get(userInterface.getAccessToken());
            accessToken = userInterface.getAccessToken();
        } else {
            accessToken = newAccessToken;
            call = userService.get(newAccessToken);
        }

        call.enqueue(new Callback<Container<UserDTO>>() {
            @Override
            public void onResponse(Call<Container<UserDTO>> call, Response<Container<UserDTO>> response) {
                if (response.isSuccessful()) {
                    userInterface.save(response.body().getContent(), accessToken);
                }
            }

            @Override
            public void onFailure(Call<Container<UserDTO>> call, Throwable t) {
                parent.showDialog(getString(R.string.error), getString(R.string.error_no_internet));
            }
        });
    }

    private void displayToast(int text) {
        Toast.makeText(parent, text, Toast.LENGTH_LONG).show();
    }

    private String extractBase64() {
        BitmapDrawable bitmapDrawable = (BitmapDrawable) profilePicImageView.getDrawable();
        return ImageUtil.createBase64(bitmapDrawable.getBitmap());
    }

    private void changeProfilePic() {
        AndroidUtils.createImageCropper(getString(R.string.apply)).start(getContext(), this);
    }

    /**
     * Display a {@link User} at the fragments view.
     *
     * @param user
     */
    private void mapUserToView(User user) {
        isReloadMode = true;
        if (user.getProfilePicTn() != null) {
            profilePicImageView.setImageBitmap(ImageUtil.createBitmap(user.getProfilePicTn()));
        }
        usernameEditText.setText(user.getUsername());
        emailEditText.setText(user.getEmail());
        isReloadMode = false;
    }

    /**
     * Extract the views values and create a {@link User} from it.
     *
     * @return
     */
    private User mapViewToUser() {
        User savedUser = userInterface.getUser();
        savedUser.setEmail(emailEditText.getText().toString());
        if (passwordChanged) {
            savedUser.setPassword(Hash.createHexHash(passwordCache));
        } else {
            savedUser.setPassword(null);
        }

        return savedUser;
    }

    /**
     * Called when the image cropper returns with a url.
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (result != null) {
                Uri resultUri = result.getUri();
                profilePicImageView.setImageBitmap(BitmapFactory.decodeFile(resultUri.getPath()));
                profilePicChanged = true;
                saveBtn();
            }
        }
    }

    @Override
    public void entityChanged(User newEntity) {
        if (newEntity != null) {
            mapUserToView(newEntity);
        }
    }

    private void isLoading(boolean loading) {
        if (loading) {
            AndroidUtils.animateView(parent.getProgressOverlay(), View.VISIBLE, 0.4f);
        } else {
            AndroidUtils.animateView(parent.getProgressOverlay(), View.GONE, 0.4f);
        }
    }

    private void showStatusIcon(EditText editText, int drawable) {
        editText.setCompoundDrawablesWithIntrinsicBounds(0, 0, drawable, 0);
    }
}
