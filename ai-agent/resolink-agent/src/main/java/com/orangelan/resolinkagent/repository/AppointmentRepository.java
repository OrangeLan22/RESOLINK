package com.orangelan.resolinkagent.repository;

import com.orangelan.resolinkagent.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    
    /**
     * 根据资源ID和类型查询预约记录
     * @param resId 资源ID
     * @param type 资源类型
     * @return 预约记录列表
     */
    List<Appointment> findByResIdAndType(Long resId, String type);
    
    /**
     * 根据资源ID和类型查询预约次数
     * @param resId 资源ID
     * @param type 资源类型
     * @return 预约次数
     */
    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.resId = :resId AND a.type = :type")
    Long countByResIdAndType(@Param("resId") Long resId, @Param("type") String type);
    
    /**
     * 根据资源ID和类型查询有效预约次数（状态为正常的）
     * @param resId 资源ID
     * @param type 资源类型
     * @return 有效预约次数
     */
    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.resId = :resId AND a.type = :type AND a.status = '0'")
    Long countValidByResIdAndType(@Param("resId") Long resId, @Param("type") String type);
    
    /**
     * 根据资源ID和类型查询已取消预约次数
     * @param resId 资源ID
     * @param type 资源类型
     * @return 已取消预约次数
     */
    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.resId = :resId AND a.type = :type AND a.status = '-1'")
    Long countCancelledByResIdAndType(@Param("resId") Long resId, @Param("type") String type);
    
    /**
     * 检查资源在指定时间段内是否有预约冲突
     * @param resId 资源ID
     * @param type 资源类型
     * @param startTime 开始时间戳
     * @param endTime 结束时间戳
     * @return 有冲突的预约记录数
     */
    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.resId = :resId AND a.type = :type AND a.status = '0' AND " +
           "((UNIX_TIMESTAMP(CONCAT(a.appointmentDate, ' ', a.startTime)) <= :startTime AND UNIX_TIMESTAMP(CONCAT(a.appointmentDate, ' ', a.endTime)) > :startTime) OR " +
           "(UNIX_TIMESTAMP(CONCAT(a.appointmentDate, ' ', a.startTime)) < :endTime AND UNIX_TIMESTAMP(CONCAT(a.appointmentDate, ' ', a.endTime)) >= :endTime) OR " +
           "(UNIX_TIMESTAMP(CONCAT(a.appointmentDate, ' ', a.startTime)) >= :startTime AND UNIX_TIMESTAMP(CONCAT(a.appointmentDate, ' ', a.endTime)) <= :endTime))")
    Long countConflictingAppointments(@Param("resId") Long resId, @Param("type") String type, 
                                  @Param("startTime") Long startTime, @Param("endTime") Long endTime);
    
    /**
     * 检查需要审核的资源在指定时间段内是否有预约冲突（包括预留时间）
     * @param resId 资源ID
     * @param type 资源类型
     * @param startTime 开始时间戳（已减去预留时间）
     * @param endTime 结束时间戳（已加上预留时间）
     * @return 有冲突的预约记录数
     */
    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.resId = :resId AND a.type = :type AND a.status = '0' AND " +
           "((UNIX_TIMESTAMP(CONCAT(a.appointmentDate, ' ', a.startTime)) <= :startTime AND UNIX_TIMESTAMP(CONCAT(a.appointmentDate, ' ', a.endTime)) > :startTime) OR " +
           "(UNIX_TIMESTAMP(CONCAT(a.appointmentDate, ' ', a.startTime)) < :endTime AND UNIX_TIMESTAMP(CONCAT(a.appointmentDate, ' ', a.endTime)) >= :endTime) OR " +
           "(UNIX_TIMESTAMP(CONCAT(a.appointmentDate, ' ', a.startTime)) >= :startTime AND UNIX_TIMESTAMP(CONCAT(a.appointmentDate, ' ', a.endTime)) <= :endTime))")
    Long countConflictingAppointmentsWithBuffer(@Param("resId") Long resId, @Param("type") String type, 
                                           @Param("startTime") Long startTime, @Param("endTime") Long endTime);
}