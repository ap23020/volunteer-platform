package gr.hua.dit.ap.vmp.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "volunteers")
public class Volunteer extends User {

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column
    private String skills;

    @Column
    private String interests;

    // Constructors
    public Volunteer() {
        super();
    }

    public Volunteer(String email, String password, String phone,
                     String firstName, String lastName, String skills, String interests) {
        super(email, password, phone, Role.VOLUNTEER);
        this.firstName = firstName;
        this.lastName = lastName;
        this.skills = skills;
        this.interests = interests;
    }

    // Getters & Setters
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

    public String getSkills() {
        return skills;
    }

    public void setSkills(String skills) {
        this.skills = skills;
    }

    public String getInterests() {
        return interests;
    }

    public void setInterests(String interests) {
        this.interests = interests;
    }
}
