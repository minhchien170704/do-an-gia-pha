package com.giapha.model;

/**
 * Model đại diện cho một cá nhân trong cây gia phả.
 */
public class Person {
    private int id;
    private String fullName;
    private int birthYear;
    private Gender gender;
    private String photoPath;
    private Person father;
    private Person mother;

    /**
     * Constructor mặc định không tham số.
     */
    public Person() {
    }

    /**
     * Constructor tạo Person mới khi chưa có ID và chưa gán cha/mẹ.
     *
     * @param fullName  họ và tên
     * @param birthYear năm sinh
     * @param gender    giới tính
     * @param photoPath đường dẫn ảnh đại diện
     */
    public Person(String fullName, int birthYear, Gender gender, String photoPath) {
        this.fullName = fullName;
        this.birthYear = birthYear;
        this.gender = gender;
        this.photoPath = photoPath;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public int getBirthYear() {
        return birthYear;
    }

    public void setBirthYear(int birthYear) {
        this.birthYear = birthYear;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public String getPhotoPath() {
        return photoPath;
    }

    public void setPhotoPath(String photoPath) {
        this.photoPath = photoPath;
    }

    public Person getFather() {
        return father;
    }

    public void setFather(Person father) {
        this.father = father;
    }

    public Person getMother() {
        return mother;
    }

    public void setMother(Person mother) {
        this.mother = mother;
    }

    @Override
    public String toString() {
        return "Person{" +
                "id=" + id +
                ", fullName='" + fullName + '\'' +
                '}';
    }
}
