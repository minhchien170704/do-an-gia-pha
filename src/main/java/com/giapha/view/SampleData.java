package com.giapha.view;

import com.giapha.model.Gender;
import com.giapha.model.Person;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Dữ liệu mẫu in-memory để preview giao diện.
 * Sẽ được thay thế bằng Repository/Service ở cụm sau.
 */
public class SampleData {

    private static SampleData instance;
    private final ObservableList<Person> people = FXCollections.observableArrayList();

    private SampleData() {
        initSampleData();
    }

    public static SampleData getInstance() {
        if (instance == null) {
            instance = new SampleData();
        }
        return instance;
    }

    private void initSampleData() {
        Person ongNoi = new Person("Nguyễn Văn An", 1945, Gender.MALE, null);
        ongNoi.setId(1);

        Person baNoi = new Person("Trần Thị Bình", 1948, Gender.FEMALE, null);
        baNoi.setId(2);

        Person ongNgoai = new Person("Lê Văn Cường", 1946, Gender.MALE, null);
        ongNgoai.setId(3);

        Person baNgoai = new Person("Phạm Thị Dung", 1950, Gender.FEMALE, null);
        baNgoai.setId(4);

        Person cha = new Person("Nguyễn Văn Đức", 1970, Gender.MALE, null);
        cha.setId(5);
        cha.setFather(ongNoi);
        cha.setMother(baNoi);

        Person me = new Person("Lê Thị Hoa", 1972, Gender.FEMALE, null);
        me.setId(6);
        me.setFather(ongNgoai);
        me.setMother(baNgoai);

        Person con1 = new Person("Nguyễn Văn Phúc", 1995, Gender.MALE, null);
        con1.setId(7);
        con1.setFather(cha);
        con1.setMother(me);

        Person con2 = new Person("Nguyễn Thị Giang", 1998, Gender.FEMALE, null);
        con2.setId(8);
        con2.setFather(cha);
        con2.setMother(me);

        Person con3 = new Person("Nguyễn Văn Hùng", 2001, Gender.MALE, null);
        con3.setId(9);
        con3.setFather(cha);
        con3.setMother(me);

        people.addAll(ongNoi, baNoi, ongNgoai, baNgoai, cha, me, con1, con2, con3);
    }

    public ObservableList<Person> getPeople() {
        return people;
    }

    public Person findById(int id) {
        return people.stream()
                .filter(p -> p.getId() == id)
                .findFirst()
                .orElse(null);
    }

    public List<Person> getChildren(Person parent) {
        return people.stream()
                .filter(p -> p.getFather() == parent || p.getMother() == parent)
                .collect(Collectors.toList());
    }

    public List<Person> search(String keyword) {
        String lower = keyword.toLowerCase();
        return people.stream()
                .filter(p -> p.getFullName().toLowerCase().contains(lower))
                .collect(Collectors.toList());
    }

    public int nextId() {
        return people.stream().mapToInt(Person::getId).max().orElse(0) + 1;
    }

    public void addPerson(Person person) {
        person.setId(nextId());
        people.add(person);
    }

    public void removePerson(Person person) {
        people.remove(person);
    }

    public List<Person> getMales() {
        return people.stream()
                .filter(p -> p.getGender() == Gender.MALE)
                .collect(Collectors.toList());
    }

    public List<Person> getFemales() {
        return people.stream()
                .filter(p -> p.getGender() == Gender.FEMALE)
                .collect(Collectors.toList());
    }
}
