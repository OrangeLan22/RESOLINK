package com.orangelan.resolinkserver.controller;

import com.orangelan.resolinkserver.entity.Public;
import com.orangelan.resolinkserver.repository.PublicRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public")
public class PublicController {
    
    private static final Logger logger = LoggerFactory.getLogger(PublicController.class);
    private final PublicRepository publicRepository;
    
    public PublicController(PublicRepository publicRepository) {
        this.publicRepository = publicRepository;
    }
    
    /**
     * 获取公共信息列表
     * @return 公共信息列表
     */
    @GetMapping
    public ResponseEntity<List<Public>> getPublicInfo() {
        try {
            logger.info("获取公共信息列表");
            List<Public> publicList = publicRepository.findAll();
            logger.info("成功获取公共信息列表，共 {} 条记录", publicList.size());
            return ResponseEntity.ok(publicList);
        } catch (RuntimeException e) {
            logger.error("获取公共信息失败：业务逻辑错误", e);
            return ResponseEntity.internalServerError().build();
        } catch (Exception e) {
            logger.error("获取公共信息失败：系统错误", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}