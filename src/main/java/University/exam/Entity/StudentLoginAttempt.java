package University.exam.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "student_login_attempts")
public class StudentLoginAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private String studentId;

    @Column(name = "attempt_time", nullable = false)
    private LocalDateTime attemptTime;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "browser_info")
    private String browserInfo;

    @Column(name = "device_info")
    private String deviceInfo;

    @Column(name = "result", nullable = false)
    private String result;

    public StudentLoginAttempt() {}

    public StudentLoginAttempt(String studentId, String ipAddress, String browserInfo, String deviceInfo, String result) {
        this.studentId = studentId;
        this.attemptTime = LocalDateTime.now();
        this.ipAddress = ipAddress;
        this.browserInfo = browserInfo;
        this.deviceInfo = deviceInfo;
        this.result = result;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public LocalDateTime getAttemptTime() { return attemptTime; }
    public void setAttemptTime(LocalDateTime attemptTime) { this.attemptTime = attemptTime; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getBrowserInfo() { return browserInfo; }
    public void setBrowserInfo(String browserInfo) { this.browserInfo = browserInfo; }

    public String getDeviceInfo() { return deviceInfo; }
    public void setDeviceInfo(String deviceInfo) { this.deviceInfo = deviceInfo; }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
}
