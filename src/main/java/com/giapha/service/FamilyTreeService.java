package com.giapha.service;

import com.giapha.exception.*;
import com.giapha.model.Gender;
import com.giapha.model.Person;
import com.giapha.repository.PersonRepository;

import java.time.Year;
import java.util.*;

/**
 * Tầng xử lý nghiệp vụ chính (Business Logic Layer) cho hệ thống Quản lý gia phả.
 * Quản lý các quy tắc nghiệp vụ: xác thực dữ liệu, gán cha mẹ, phát hiện chu trình,
 * kiểm tra ràng buộc trước khi xóa, và duyệt cây huyết thống tổ tiên / hậu duệ.
 */
public class FamilyTreeService {

    private final PersonRepository personRepository;

    /**
     * Khởi tạo FamilyTreeService với PersonRepository.
     *
     * @param personRepository kho lưu trữ dữ liệu Person
     */
    public FamilyTreeService(PersonRepository personRepository) {
        this.personRepository = Objects.requireNonNull(personRepository, "personRepository không được để null");
    }

    /**
     * Lấy thông tin Person theo ID.
     *
     * @param id mã định danh của Person
     * @return đối tượng Person nếu tồn tại
     * @throws PersonNotFoundException nếu không tìm thấy Person với id tương ứng
     */
    public Person getPersonById(int id) {
        Person person = personRepository.findById(id);
        if (person == null) {
            throw new PersonNotFoundException("Không tìm thấy Person với id = " + id);
        }
        return person;
    }

    /**
     * Thêm mới một thành viên vào hệ thống gia phả.
     *
     * @param person thông tin Person cần thêm
     * @return đối tượng Person đã được gán id tự sinh
     * @throws IllegalArgumentException nếu dữ liệu không hợp lệ hoặc thông tin cha mẹ vi phạm
     * @throws InvalidBirthYearException nếu năm sinh không hợp lệ
     * @throws PersonNotFoundException nếu cha hoặc mẹ chỉ định không tồn tại
     */
    public Person addPerson(Person person) {
        validatePerson(person);

        if (person.getFather() != null && person.getMother() != null) {
            if (person.getFather().getId() == person.getMother().getId()) {
                throw new IllegalArgumentException("Cha và Mẹ không thể là cùng một người");
            }
        }

        if (person.getFather() != null) {
            Person father = getPersonById(person.getFather().getId());
            if (father.getGender() != Gender.MALE) {
                throw new IllegalArgumentException("Cha phải có giới tính là MALE");
            }
            person.setFather(father);
        }

        if (person.getMother() != null) {
            Person mother = getPersonById(person.getMother().getId());
            if (mother.getGender() != Gender.FEMALE) {
                throw new IllegalArgumentException("Mẹ phải có giới tính là FEMALE");
            }
            person.setMother(mother);
        }

        // BR5 is a warning only and does not block the operation.

        int generatedId = personRepository.save(person);
        person.setId(generatedId);
        return person;
    }

    /**
     * Cập nhật thông tin của một thành viên trong gia phả.
     *
     * @param person thông tin Person cập nhật kèm id
     * @throws IllegalArgumentException nếu dữ liệu không hợp lệ
     * @throws InvalidBirthYearException nếu năm sinh không hợp lệ
     * @throws PersonNotFoundException nếu Person hoặc cha/mẹ không tồn tại
     * @throws SelfParentException nếu tự gán mình làm cha hoặc mẹ
     * @throws CycleDetectedException nếu việc gán cha mẹ tạo chu trình trong cây gia phả
     */
    public void updatePerson(Person person) {
        validatePerson(person);

        if (person.getId() <= 0) {
            throw new IllegalArgumentException("Person id phải là số nguyên dương");
        }

        // Kiểm tra sự tồn tại của Person cần cập nhật
        getPersonById(person.getId());

        if (person.getFather() != null) {
            Person father = getPersonById(person.getFather().getId());
            validateParentAssignment(person, father, Gender.MALE);
            if (checkCycle(person.getId(), father.getId())) {
                throw new CycleDetectedException("Gán cha tạo chu trình vòng lặp trong cây gia phả");
            }
            person.setFather(father);
        }

        if (person.getMother() != null) {
            Person mother = getPersonById(person.getMother().getId());
            validateParentAssignment(person, mother, Gender.FEMALE);
            if (checkCycle(person.getId(), mother.getId())) {
                throw new CycleDetectedException("Gán mẹ tạo chu trình vòng lặp trong cây gia phả");
            }
            person.setMother(mother);
        }

        if (person.getFather() != null && person.getMother() != null) {
            if (person.getFather().getId() == person.getMother().getId()) {
                throw new IllegalArgumentException("Cha và Mẹ không thể là cùng một người");
            }
        }

        // BR5 is a warning only and does not block the operation.

        personRepository.update(person);
    }

    /**
     * Xóa một thành viên khỏi hệ thống gia phả.
     *
     * @param id mã định danh của Person cần xóa
     * @throws PersonNotFoundException nếu không tìm thấy Person
     * @throws PersonInUseException nếu Person đang là cha hoặc mẹ của người khác
     */
    public void deletePerson(int id) {
        getPersonById(id);

        if (checkPersonInUse(id)) {
            throw new PersonInUseException("Không thể xóa Person có id = " + id + " vì đang được dùng làm cha hoặc mẹ trong gia phả");
        }

        personRepository.delete(id);
    }

    /**
     * Gán cha cho một người con.
     *
     * @param childId  mã định danh của người con
     * @param fatherId mã định danh của người cha
     * @throws PersonNotFoundException nếu childId hoặc fatherId không tồn tại
     * @throws SelfParentException nếu childId trùng fatherId
     * @throws IllegalArgumentException nếu giới tính cha không phải MALE hoặc trùng với mẹ hiện tại
     * @throws CycleDetectedException nếu việc gán tạo chu trình trong cây gia phả
     */
    public void assignFather(int childId, int fatherId) {
        Person child = getPersonById(childId);
        Person father = getPersonById(fatherId);

        validateParentAssignment(child, father, Gender.MALE);

        if (child.getMother() != null && child.getMother().getId() == father.getId()) {
            throw new IllegalArgumentException("Cha và Mẹ không thể là cùng một người");
        }

        if (checkCycle(childId, fatherId)) {
            throw new CycleDetectedException("Gán cha tạo chu trình vòng lặp trong cây gia phả");
        }

        child.setFather(father);
        personRepository.update(child);
    }

    /**
     * Gán mẹ cho một người con.
     *
     * @param childId  mã định danh của người con
     * @param motherId mã định danh của người mẹ
     * @throws PersonNotFoundException nếu childId hoặc motherId không tồn tại
     * @throws SelfParentException nếu childId trùng motherId
     * @throws IllegalArgumentException nếu giới tính mẹ không phải FEMALE hoặc trùng với cha hiện tại
     * @throws CycleDetectedException nếu việc gán tạo chu trình trong cây gia phả
     */
    public void assignMother(int childId, int motherId) {
        Person child = getPersonById(childId);
        Person mother = getPersonById(motherId);

        validateParentAssignment(child, mother, Gender.FEMALE);

        if (child.getFather() != null && child.getFather().getId() == mother.getId()) {
            throw new IllegalArgumentException("Cha và Mẹ không thể là cùng một người");
        }

        if (checkCycle(childId, motherId)) {
            throw new CycleDetectedException("Gán mẹ tạo chu trình vòng lặp trong cây gia phả");
        }

        child.setMother(mother);
        personRepository.update(child);
    }

    /**
     * Lấy danh sách toàn bộ tổ tiên (cha, mẹ, ông, bà,...) của một người.
     *
     * @param id mã định danh của người cần tra cứu
     * @return danh sách các Person là tổ tiên (không bao gồm chính người này)
     * @throws PersonNotFoundException nếu không tìm thấy Person
     */
    public List<Person> getAncestors(int id) {
        Person focus = getPersonById(id);

        List<Person> ancestors = new ArrayList<>();
        Set<Integer> visited = new HashSet<>();
        visited.add(focus.getId());

        Queue<Integer> queue = new ArrayDeque<>();
        if (focus.getFather() != null) {
            queue.add(focus.getFather().getId());
        }
        if (focus.getMother() != null) {
            queue.add(focus.getMother().getId());
        }

        while (!queue.isEmpty()) {
            int curId = queue.poll();
            if (visited.add(curId)) {
                Person p = personRepository.findById(curId);
                if (p != null) {
                    ancestors.add(p);
                    if (p.getFather() != null) {
                        queue.add(p.getFather().getId());
                    }
                    if (p.getMother() != null) {
                        queue.add(p.getMother().getId());
                    }
                }
            }
        }

        return ancestors;
    }

    /**
     * Lấy danh sách toàn bộ hậu duệ (con, cháu, chắt,...) của một người.
     *
     * @param id mã định danh của người cần tra cứu
     * @return danh sách các Person là hậu duệ (không bao gồm chính người này)
     * @throws PersonNotFoundException nếu không tìm thấy Person
     */
    public List<Person> getDescendants(int id) {
        Person focus = getPersonById(id);

        List<Person> all = personRepository.findAll();
        Map<Integer, List<Person>> childrenMap = new HashMap<>();
        for (Person p : all) {
            if (p.getFather() != null) {
                childrenMap.computeIfAbsent(p.getFather().getId(), k -> new ArrayList<>()).add(p);
            }
            if (p.getMother() != null) {
                childrenMap.computeIfAbsent(p.getMother().getId(), k -> new ArrayList<>()).add(p);
            }
        }

        List<Person> descendants = new ArrayList<>();
        Set<Integer> visited = new HashSet<>();
        visited.add(focus.getId());

        Queue<Integer> queue = new ArrayDeque<>();
        queue.add(focus.getId());

        while (!queue.isEmpty()) {
            int curId = queue.poll();
            List<Person> children = childrenMap.getOrDefault(curId, Collections.emptyList());
            for (Person child : children) {
                if (visited.add(child.getId())) {
                    descendants.add(child);
                    queue.add(child.getId());
                }
            }
        }

        return descendants;
    }

    // ============================================================
    // PRIVATE METHODS THEO SPEC
    // ============================================================

    /**
     * Xác thực thông tin hợp lệ của một Person.
     */
    private void validatePerson(Person person) {
        if (person == null) {
            throw new IllegalArgumentException("Person không được để null");
        }

        if (person.getFullName() == null || person.getFullName().trim().isEmpty()) {
            throw new IllegalArgumentException("Họ và tên không được để trống");
        }

        String trimmedName = person.getFullName().trim();
        if (trimmedName.length() < 1 || trimmedName.length() > 100) {
            throw new IllegalArgumentException("Độ dài họ và tên phải từ 1 đến 100 ký tự");
        }

        int currentYear = Year.now().getValue();
        if (person.getBirthYear() <= 1900 || person.getBirthYear() > currentYear) {
            throw new InvalidBirthYearException("Năm sinh không hợp lệ: " + person.getBirthYear()
                    + " (phải > 1900 và <= " + currentYear + ")");
        }

        if (person.getGender() == null) {
            throw new IllegalArgumentException("Giới tính không được để null");
        }
    }

    /**
     * Xác thực tính hợp lệ khi gán quan hệ cha/mẹ.
     */
    private void validateParentAssignment(Person child, Person parent, Gender requiredGender) {
        if (child == null) {
            throw new IllegalArgumentException("Child không được để null");
        }
        if (parent == null) {
            throw new IllegalArgumentException("Parent không được để null");
        }

        if (child.getId() == parent.getId()) {
            throw new SelfParentException("Một người không thể là cha hoặc mẹ của chính mình (id = " + child.getId() + ")");
        }

        if (parent.getGender() != requiredGender) {
            throw new IllegalArgumentException("Giới tính của phụ huynh phải là " + requiredGender
                    + " nhưng thực tế là " + parent.getGender());
        }

        // BR5 is a warning only and does not block the operation.
    }

    /**
     * Kiểm tra khả năng tạo chu trình (cycle) trong cây gia phả.
     * Duyệt ngược từ candidateParent theo các nhánh cha/mẹ. Nếu gặp childId nghĩa là
     * childId là tổ tiên của candidateParent -> việc gán candidateParent làm cha/mẹ của childId
     * sẽ tạo ra chu trình khép kín.
     *
     * @param childId           id người con
     * @param candidateParentId id người dự định gán làm cha/mẹ
     * @return true nếu tạo chu trình, false nếu an toàn
     */
    private boolean checkCycle(int childId, int candidateParentId) {
        if (childId == candidateParentId) {
            return true;
        }

        Set<Integer> visited = new HashSet<>();
        Deque<Integer> stack = new ArrayDeque<>();
        stack.push(candidateParentId);
        visited.add(candidateParentId);

        while (!stack.isEmpty()) {
            int currentId = stack.pop();
            if (currentId == childId) {
                return true;
            }

            Person current = personRepository.findById(currentId);
            if (current != null) {
                if (current.getFather() != null) {
                    int fId = current.getFather().getId();
                    if (fId == childId) {
                        return true;
                    }
                    if (visited.add(fId)) {
                        stack.push(fId);
                    }
                }
                if (current.getMother() != null) {
                    int mId = current.getMother().getId();
                    if (mId == childId) {
                        return true;
                    }
                    if (visited.add(mId)) {
                        stack.push(mId);
                    }
                }
            }
        }

        return false;
    }

    /**
     * Kiểm tra xem một Person có đang được tham chiếu làm cha hoặc mẹ của người khác không.
     *
     * @param id mã định danh của Person cần kiểm tra
     * @return true nếu đang được tham chiếu, ngược lại false
     */
    private boolean checkPersonInUse(int id) {
        List<Person> all = personRepository.findAll();
        for (Person p : all) {
            if (p.getFather() != null && p.getFather().getId() == id) {
                return true;
            }
            if (p.getMother() != null && p.getMother().getId() == id) {
                return true;
            }
        }
        return false;
    }
}
