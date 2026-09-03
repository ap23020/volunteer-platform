package gr.hua.dit.ap.vmp.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "organization_users")
public class OrganizationUser extends User {

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @ManyToOne
    @JoinColumn(name = "organization_id", referencedColumnName = "id")
    private Organization organization = new Organization();

    // Constructors
    public OrganizationUser() {
        super();
    }

    public OrganizationUser(String email, String password, String phone,
                            String firstName, String lastName, Organization organization) {
        super(email, password, phone, Role.ORGANIZATION);
        this.firstName = firstName;
        this.lastName = lastName;
        this.organization = organization;
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

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }
}

