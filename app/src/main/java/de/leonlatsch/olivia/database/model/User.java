package de.leonlatsch.olivia.database.model;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;

@Table(name = "user")
public class User extends Model {

    @Column(name = "uid", index = true)
    private String uid;

    @Column(name = "username")
    private String username;

    @Column(name = "email")
    private String email;

    @Column(name = "password")
    private String password;

    @Column(name = "profile_pic_tn")
    private String profilePicTn;

    @Column(name = "token")
    private String accessToken;

    @Column(name = "private_key")
    private String privateKey;

    public User() {}

    public User(String uid, String username, String email, String password, String profilePicTn, String accessToken, String privateKey) {
        this.uid = uid;
        this.username = username;
        this.email = email;
        this.password = password;
        this.profilePicTn = profilePicTn;
        this.accessToken = accessToken;
        this.privateKey = privateKey;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getProfilePicTn() {
        return profilePicTn;
    }

    public void setProfilePicTn(String profilePicTn) {
        this.profilePicTn = profilePicTn;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }
}
