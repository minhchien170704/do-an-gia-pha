package com.giapha.repository;

import com.giapha.model.Person;

import java.util.List;

/**
 * Interface định nghĩa các thao tác lưu trữ và truy vấn dữ liệu cho Person.
 * Tuân thủ chính xác UML Class Diagram của hệ thống Sơ đồ gia phả.
 */
public interface PersonRepository {

    /**
     * Lấy toàn bộ danh sách người trong cơ sở dữ liệu.
     *
     * @return danh sách tất cả các Person
     */
    List<Person> findAll();

    /**
     * Tìm kiếm một Person theo mã định danh (id).
     *
     * @param id mã định danh của người cần tìm
     * @return đối tượng Person nếu tìm thấy, ngược lại trả về null
     */
    Person findById(int id);

    /**
     * Tìm kiếm danh sách Person theo từ khóa họ tên.
     *
     * @param keyword từ khóa tìm kiếm
     * @return danh sách các Person có tên khớp với từ khóa
     */
    List<Person> search(String keyword);

    /**
     * Thêm mới một Person vào cơ sở dữ liệu.
     *
     * @param person đối tượng Person cần lưu
     * @return id tự động sinh (generated key) của Person vừa lưu
     */
    int save(Person person);

    /**
     * Cập nhật thông tin của một Person trong cơ sở dữ liệu.
     *
     * @param person đối tượng Person chứa thông tin cập nhật (kèm id)
     */
    void update(Person person);

    /**
     * Xóa một Person khỏi cơ sở dữ liệu theo mã định danh (id).
     *
     * @param id mã định danh của Person cần xóa
     */
    void delete(int id);
}
