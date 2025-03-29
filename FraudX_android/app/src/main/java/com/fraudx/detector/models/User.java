package com.fraudx.detector.models;

public class User {
    private String firstName;
    private String lastName;
    private String email;
    private String profilePic;
    private String password;

    // Default constructor needed for Firebase
    public User() {
        // Required empty public constructor
    }

    public User(String firstName, String email, String password, String profilePic) {
        this.firstName = firstName;
        this.email = email;
        this.password = password;
        this.profilePic = profilePic;
        this.lastName = "";
    }

    public User(String firstName, String lastName, String email, String password, String profilePic) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
        this.profilePic = profilePic;
    }

    // Getters and Setters
    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getProfilePic() {
        return profilePic;
    }

    public void setProfilePic(String profilePic) {
        this.profilePic = profilePic;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
}
