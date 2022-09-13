package domain;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class Customer implements Serializable {

    @SerializedName("first_name")
    private String firstName;

    @SerializedName("last_name")
    private String lastName;

    @SerializedName("customer_code")
    private String customerCode;

    private String id;

    @SerializedName("customer_group_id")
    private String group;

    private String email;

    public Customer() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    @Override
    public String toString() {
        return "Customer{" + "id=" + id + ", group=" + group + ", email=" + email + '}';
    }

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

    public String getCustomerCode() {
        return customerCode;
    }

    public void setCustomerCode(String customerCode) {
        this.customerCode = customerCode;
    }

    public Customer getCustomerFromAccount(Account account) {
        Customer customer = new Customer();
        customer.setCustomerCode(account.getUsername());
        customer.setFirstName(account.getFirstName());
        customer.setLastName(account.getLastName());
        customer.setEmail(account.getEmail());
        customer.setGroup("0afa8de1-147c-11e8-edec-2b197906d816");
        return customer;
    }
    
    public Customer customerWithNewGroup(String newGroup) {
        this.setGroup(newGroup);
        return this;
    }
}
