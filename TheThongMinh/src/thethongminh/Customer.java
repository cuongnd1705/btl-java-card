/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package thethongminh;

/**
 *
 * @author Cuong
 */
public class Customer {

    private String id;
    private String name;
    private String birthDate;
    private String address;
    private String gender;
    private Double balance;
    private Integer point;
    private String pin;
    private byte[] avatar;

    public Customer(String id, String name, String birthDate, String address, String gender, Double balance, Integer point, String pin, byte[] avatar) {
        this.id = id;
        this.name = name;
        this.birthDate = birthDate;
        this.address = address;
        this.gender = gender;
        this.balance = balance;
        this.point = point;
        this.pin = pin;
        this.avatar = avatar;
    }

    public Customer() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(String birthDate) {
        this.birthDate = birthDate;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public Double getBalance() {
        return balance;
    }

    public void setBalance(Double balance) {
        this.balance = balance;
    }

    public Integer getPoint() {
        return point;
    }

    public void setPoint(Integer point) {
        this.point = point;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public byte[] getAvatar() {
        return avatar;
    }

    public void setAvatar(byte[] avatar) {
        this.avatar = avatar;
    }
}
