package com.orangelan.resolinkserver.repository;

import com.orangelan.resolinkserver.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    
    /**
     * 根据员工工号查询预约记录
     * @param empId 员工工号
     * @return 预约记录列表
     */
    List<Appointment> findByEmpId(String empId);
    
    /**
     * 根据资源ID查询预约记录
     * @param resId 资源ID
     * @return 预约记录列表
     */
    List<Appointment> findByResId(Long resId);
    
    /**
     * 根据资源类型查询预约记录
     * @param type 资源类型
     * @return 预约记录列表
     */
    List<Appointment> findByType(String type);
    
    /**
     * 根据状态查询预约记录
     * @param status 状态
     * @return 预约记录列表
     */
    List<Appointment> findByStatus(String status);
    
    /**
     * 根据预约日期范围查询预约记录
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 预约记录列表
     */
    @Query("SELECT a FROM Appointment a WHERE a.appointmentDate BETWEEN :startDate AND :endDate ORDER BY a.appointmentDate DESC")
    List<Appointment> findByAppointmentDateBetween(@Param("startDate") Date startDate, @Param("endDate") Date endDate);
    
    /**
     * 根据员工工号和预约日期范围查询预约记录
     * @param empId 员工工号
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 预约记录列表
     */
    @Query("SELECT a FROM Appointment a WHERE a.empId = :empId AND a.appointmentDate BETWEEN :startDate AND :endDate ORDER BY a.appointmentDate DESC")
    List<Appointment> findByEmpIdAndAppointmentDateBetween(@Param("empId") String empId, 
                                                          @Param("startDate") Date startDate, 
                                                          @Param("endDate") Date endDate);
    
    /**
     * 根据资源ID和预约日期范围查询预约记录
     * @param resId 资源ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 预约记录列表
     */
    @Query("SELECT a FROM Appointment a WHERE a.resId = :resId AND a.appointmentDate BETWEEN :startDate AND :endDate ORDER BY a.appointmentDate DESC")
    List<Appointment> findByResIdAndAppointmentDateBetween(@Param("resId") Long resId, 
                                                          @Param("startDate") Date startDate, 
                                                          @Param("endDate") Date endDate);
    
    /**
     * 根据审批状态查询预约记录
     * @param approval 审批状态
     * @return 预约记录列表
     */
    List<Appointment> findByApproval(Integer approval);
}