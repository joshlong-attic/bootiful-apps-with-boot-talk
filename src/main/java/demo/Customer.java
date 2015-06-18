package demo;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.io.Serializable;


@Entity
public class Customer {

    @Id
    @GeneratedValue
    private Long id;

    private String surname;

    public Customer() {
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Customer{");
        sb.append("id=").append(id);
        sb.append(", surname='").append(surname).append('\'');
        sb.append('}');
        return sb.toString();
    }

    public Long getId() {
        return id;
    }

    public String getSurname() {
        return surname;
    }

    public Customer(String surname) {
        this.surname = surname;
    }
}
