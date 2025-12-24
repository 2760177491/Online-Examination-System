package com.example.onlineexam.service.impl;

import com.example.onlineexam.dto.MonitorLogDto;
import com.example.onlineexam.entity.ExamMonitorLog;
import com.example.onlineexam.entity.User;
import com.example.onlineexam.repository.ExamMonitorLogRepository;
import com.example.onlineexam.repository.UserRepository;
import com.example.onlineexam.service.ExamMonitorService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ExamMonitorServiceImpl implements ExamMonitorService {

    private final ExamMonitorLogRepository monitorLogRepository;
    private final UserRepository userRepository;

    public ExamMonitorServiceImpl(ExamMonitorLogRepository monitorLogRepository,
                                 UserRepository userRepository) {
        this.monitorLogRepository = monitorLogRepository;
        this.userRepository = userRepository;
    }

    @Override
    public void heartbeat(Long examSessionId, Long studentId) {
        ExamMonitorLog log = monitorLogRepository
                .findByExamSessionIdAndStudentId(examSessionId, studentId)
                .orElseGet(() -> {
                    ExamMonitorLog n = new ExamMonitorLog();
                    n.setExamSessionId(examSessionId);
                    n.setStudentId(studentId);
                    n.setSwitchCount(0);
                    return n;
                });

        log.setLastHeartbeatTime(LocalDateTime.now());
        log.setLastEvent("heartbeat");
        monitorLogRepository.save(log);
    }

    @Override
    public void screenSwitch(Long examSessionId, Long studentId) {
        ExamMonitorLog log = monitorLogRepository
                .findByExamSessionIdAndStudentId(examSessionId, studentId)
                .orElseGet(() -> {
                    ExamMonitorLog n = new ExamMonitorLog();
                    n.setExamSessionId(examSessionId);
                    n.setStudentId(studentId);
                    n.setSwitchCount(0);
                    return n;
                });

        log.setLastHeartbeatTime(LocalDateTime.now());
        log.setLastEvent("switch");
        log.setSwitchCount((log.getSwitchCount() == null ? 0 : log.getSwitchCount()) + 1);
        monitorLogRepository.save(log);
    }

    @Override
    public List<MonitorLogDto> listByExamSessionId(Long examSessionId) {
        List<ExamMonitorLog> logs = monitorLogRepository.findByExamSessionId(examSessionId);
        List<MonitorLogDto> result = new ArrayList<>();

        for (ExamMonitorLog l : logs) {
            MonitorLogDto dto = new MonitorLogDto();
            dto.setStudentId(l.getStudentId());
            dto.setLastHeartbeatTime(l.getLastHeartbeatTime());
            dto.setSwitchCount(l.getSwitchCount() == null ? 0 : l.getSwitchCount());

            User u = userRepository.findById(l.getStudentId()).orElse(null);
            if (u != null) {
                dto.setStudentUsername(u.getUsername());
                dto.setStudentRealName(u.getRealName());
            }
            result.add(dto);
        }

        return result;
    }
}

