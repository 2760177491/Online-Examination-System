package com.example.onlineexam.controller;

import com.example.onlineexam.dto.ApiResponse;
import com.example.onlineexam.dto.ExamStatsDto;
import com.example.onlineexam.dto.TeacherResultListItemDto;
import com.example.onlineexam.entity.ExamPaper;
import com.example.onlineexam.entity.ExamSession;
import com.example.onlineexam.entity.StudentExam;
import com.example.onlineexam.entity.User;
import com.example.onlineexam.repository.ExamPaperRepository;
import com.example.onlineexam.repository.ExamSessionRepository;
import com.example.onlineexam.repository.StudentExamRepository;
import com.example.onlineexam.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.bind.annotation.*;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 第10周：成绩查询与分析相关接口（教师端 / 学生端）
 *
 * 说明：
 * - 学生端“我的成绩”已经有 /api/results/my 和 /api/results/{studentExamId}/detail
 * - 这里补充教师端：按考试场次查看成绩列表、统计数据、导出 CSV
 */
@RestController
@RequestMapping("/api/scores")
public class ScoreController {

    private final StudentExamRepository studentExamRepository;
    private final ExamSessionRepository examSessionRepository;
    private final ExamPaperRepository examPaperRepository;
    private final UserRepository userRepository;

    public ScoreController(StudentExamRepository studentExamRepository,
                           ExamSessionRepository examSessionRepository,
                           ExamPaperRepository examPaperRepository,
                           UserRepository userRepository) {
        this.studentExamRepository = studentExamRepository;
        this.examSessionRepository = examSessionRepository;
        this.examPaperRepository = examPaperRepository;
        this.userRepository = userRepository;
    }

    private static final List<String> SUBMITTED_STATUSES = Arrays.asList("已提交", "已批改");

    /**
     * 教师端：某场考试成绩列表
     */
    @GetMapping("/teacher")
    public ApiResponse teacherList(@RequestParam Long examSessionId, HttpSession session) {
        // 简单权限保护：仅教师可访问
        String role = (String) session.getAttribute("userRole");
        if (role == null) return ApiResponse.error("用户未登录");
        if (!"TEACHER".equals(role)) return ApiResponse.error("只有教师可以查看成绩列表");

        ExamSession es = examSessionRepository.findById(examSessionId)
                .orElseThrow(() -> new RuntimeException("考试场次不存在"));
        ExamPaper paper = examPaperRepository.findById(es.getExamPaperId())
                .orElseThrow(() -> new RuntimeException("试卷不存在"));

        List<StudentExam> submitted = studentExamRepository.findByExamSessionIdAndStatusIn(examSessionId, SUBMITTED_STATUSES);
        if (submitted.isEmpty()) {
            return ApiResponse.success("查询成功", Collections.emptyList());
        }

        // 查学生信息
        Set<Long> studentIds = submitted.stream().map(StudentExam::getStudentId).collect(Collectors.toSet());
        List<User> users = userRepository.findAllById(studentIds);
        Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::getId, u -> u));

        int total = paper.getTotalScore() == null ? 0 : paper.getTotalScore();

        List<TeacherResultListItemDto> list = new ArrayList<>();
        for (StudentExam se : submitted) {
            TeacherResultListItemDto dto = new TeacherResultListItemDto();
            dto.setStudentExamId(se.getId());
            dto.setStudentId(se.getStudentId());
            dto.setScore(se.getTotalScore() == null ? 0 : se.getTotalScore());
            dto.setTotalScore(total);
            dto.setStatus(se.getStatus());
            dto.setSubmitTime(se.getSubmitTime());

            User u = userMap.get(se.getStudentId());
            if (u != null) {
                dto.setStudentUsername(u.getUsername());
                dto.setStudentRealName(u.getRealName());
            }

            if (total > 0) {
                dto.setScoreRate(dto.getScore() * 1.0 / total);
            } else {
                dto.setScoreRate(0.0);
            }
            list.add(dto);
        }

        // 默认按提交时间倒序
        list.sort((a, b) -> {
            LocalDateTime t1 = a.getSubmitTime();
            LocalDateTime t2 = b.getSubmitTime();
            if (t1 == null && t2 == null) return 0;
            if (t1 == null) return 1;
            if (t2 == null) return -1;
            return t2.compareTo(t1);
        });

        return ApiResponse.success("查询成功", list);
    }

    /**
     * 教师端：某场考试统计信息
     */
    @GetMapping("/teacher/stats")
    public ApiResponse teacherStats(@RequestParam Long examSessionId, HttpSession session) {
        String role = (String) session.getAttribute("userRole");
        if (role == null) return ApiResponse.error("用户未登录");
        if (!"TEACHER".equals(role)) return ApiResponse.error("只有教师可以查看统计");

        ExamSession es = examSessionRepository.findById(examSessionId)
                .orElseThrow(() -> new RuntimeException("考试场次不存在"));
        ExamPaper paper = examPaperRepository.findById(es.getExamPaperId())
                .orElseThrow(() -> new RuntimeException("试卷不存在"));
        int totalScore = paper.getTotalScore() == null ? 0 : paper.getTotalScore();

        List<StudentExam> submitted = studentExamRepository.findByExamSessionIdAndStatusIn(examSessionId, SUBMITTED_STATUSES);

        // 中文注释：这里直接返回 Map，避免因 DTO/Lombok 在某些环境下导致 getter/setter 不可用
        // 前端用 stats.xxx 读取不受影响（JSON key 一致即可）
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("totalParticipants", (long) submitted.size());
        dto.put("submittedCount", (long) submitted.size());

        // 中文注释：补齐“成绩分布”数据，前端柱状图需要它
        Map<String, Integer> scoreDistribution = new LinkedHashMap<>();

        if (submitted.isEmpty()) {
            dto.put("avgScore", 0.0);
            dto.put("maxScore", 0);
            dto.put("minScore", 0);
            dto.put("passRate", 0.0);
            dto.put("scoreDistribution", scoreDistribution);
            return ApiResponse.success("查询成功", dto);
        }

        List<Integer> scores = submitted.stream()
                .map(se -> se.getTotalScore() == null ? 0 : se.getTotalScore())
                .toList();

        int max = scores.stream().max(Integer::compareTo).orElse(0);
        int min = scores.stream().min(Integer::compareTo).orElse(0);
        double avg = scores.stream().mapToInt(x -> x).average().orElse(0.0);

        int passLine = (int) Math.ceil(totalScore * 0.6);
        long passCount = scores.stream().filter(s -> s >= passLine).count();
        double passRate = submitted.isEmpty() ? 0.0 : (passCount * 1.0 / submitted.size());

        dto.put("maxScore", max);
        dto.put("minScore", min);
        dto.put("avgScore", avg);
        dto.put("passRate", passRate);

        String[] buckets = new String[]{"0-60", "60-70", "70-80", "80-90", "90-100"};
        for (String b : buckets) scoreDistribution.put(b, 0);

        for (Integer s : scores) {
            double percent;
            if (totalScore <= 0) {
                percent = Math.max(0, Math.min(100, s));
            } else {
                percent = (s * 100.0 / totalScore);
            }

            String key;
            if (percent < 60) key = "0-60";
            else if (percent < 70) key = "60-70";
            else if (percent < 80) key = "70-80";
            else if (percent < 90) key = "80-90";
            else key = "90-100";

            scoreDistribution.put(key, scoreDistribution.getOrDefault(key, 0) + 1);
        }

        dto.put("scoreDistribution", scoreDistribution);
        return ApiResponse.success("查询成功", dto);
    }

    /**
     * 教师端：导出某场考试成绩（标准 Excel .xlsx）
     *
     * 中文注释：这里使用 Apache POI 真实生成 Office Open XML（.xlsx），满足“标准 Excel”要求。
     */
    @GetMapping("/teacher/export-xlsx")
    public void exportXlsx(@RequestParam Long examSessionId, HttpSession session, HttpServletResponse response) {
        String role = (String) session.getAttribute("userRole");
        if (role == null) {
            throw new RuntimeException("用户未登录");
        }
        if (!"TEACHER".equals(role)) {
            throw new RuntimeException("只有教师可以导出成绩");
        }

        ExamSession es = examSessionRepository.findById(examSessionId)
                .orElseThrow(() -> new RuntimeException("考试场次不存在"));
        ExamPaper paper = examPaperRepository.findById(es.getExamPaperId())
                .orElseThrow(() -> new RuntimeException("试卷不存在"));

        // 中文注释：文件名加入考试名称，方便老师下载后直接识别（并过滤 Windows 不允许的字符）
        String safeExamName = paper.getName() == null ? "" : paper.getName().replaceAll("[\\\\/:*?\"<>|]", "_");
        int total = paper.getTotalScore() == null ? 0 : paper.getTotalScore();

        List<StudentExam> submitted = studentExamRepository.findByExamSessionIdAndStatusIn(examSessionId, SUBMITTED_STATUSES);
        Set<Long> studentIds = submitted.stream().map(StudentExam::getStudentId).collect(Collectors.toSet());
        List<User> users = studentIds.isEmpty() ? Collections.emptyList() : userRepository.findAllById(studentIds);
        Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::getId, u -> u));

        String fileName = "scores_" + safeExamName + "_examSession_" + examSessionId + "_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".xlsx";

        try {
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

            // 中文注释：同时设置 filename 与 filename*，最大化兼容各浏览器对中文文件名的支持
            String encoded = java.net.URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"" + fileName + "\"; filename*=UTF-8''" + encoded);
            response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
            response.setHeader("Pragma", "no-cache");

            // 1) 生成工作簿
            try (Workbook wb = new XSSFWorkbook()) {
                CreationHelper helper = wb.getCreationHelper();

                // 中文注释：Excel Sheet 名称禁用部分字符且长度 <= 31
                String sheetName = paper.getName() == null || paper.getName().isBlank()
                        ? ("examSession_" + examSessionId)
                        : paper.getName();
                sheetName = sheetName.replaceAll("[\\\\/\\?\\*\\[\\]]", "_");
                if (sheetName.length() > 31) sheetName = sheetName.substring(0, 31);

                Sheet sheet = wb.createSheet(sheetName);

                // 2) 表头样式
                CellStyle headerStyle = wb.createCellStyle();
                Font headerFont = wb.createFont();
                headerFont.setBold(true);
                headerStyle.setFont(headerFont);

                // 3) 日期样式
                CellStyle dateStyle = wb.createCellStyle();
                short df = helper.createDataFormat().getFormat("yyyy-mm-dd hh:mm:ss");
                dateStyle.setDataFormat(df);

                // 4) 写表头
                String[] headers = new String[]{
                        "studentExamId", "studentId", "username", "realName",
                        "score", "totalScore", "scoreRate", "submitTime", "status"
                };
                Row headerRow = sheet.createRow(0);
                for (int i = 0; i < headers.length; i++) {
                    Cell c = headerRow.createCell(i);
                    c.setCellValue(headers[i]);
                    c.setCellStyle(headerStyle);
                }

                // 5) 写数据行
                int rowIdx = 1;
                for (StudentExam se : submitted) {
                    User u = userMap.get(se.getStudentId());
                    String username = u == null ? "" : safeString(u.getUsername());
                    String realName = u == null ? "" : safeString(u.getRealName());
                    int score = se.getTotalScore() == null ? 0 : se.getTotalScore();
                    double rate = total > 0 ? (score * 1.0 / total) : 0.0;

                    Row r = sheet.createRow(rowIdx++);
                    r.createCell(0).setCellValue(se.getId() == null ? 0 : se.getId());
                    r.createCell(1).setCellValue(se.getStudentId() == null ? 0 : se.getStudentId());
                    r.createCell(2).setCellValue(username);
                    r.createCell(3).setCellValue(realName);
                    r.createCell(4).setCellValue(score);
                    r.createCell(5).setCellValue(total);
                    r.createCell(6).setCellValue(rate);

                    Cell submitCell = r.createCell(7);
                    if (se.getSubmitTime() != null) {
                        // LocalDateTime -> Date
                        java.util.Date d = java.util.Date.from(se.getSubmitTime().atZone(java.time.ZoneId.systemDefault()).toInstant());
                        submitCell.setCellValue(d);
                        submitCell.setCellStyle(dateStyle);
                    } else {
                        submitCell.setCellValue("");
                    }

                    r.createCell(8).setCellValue(safeString(se.getStatus()));
                }

                // 6) 自适应列宽（数据量大时可能慢，但演示足够）
                for (int i = 0; i < headers.length; i++) {
                    sheet.autoSizeColumn(i);
                }

                // 7) 输出
                try (OutputStream os = response.getOutputStream()) {
                    wb.write(os);
                    os.flush();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("导出失败：" + e.getMessage(), e);
        }
    }

    private String safeString(String s) {
        return s == null ? "" : s;
    }
}
